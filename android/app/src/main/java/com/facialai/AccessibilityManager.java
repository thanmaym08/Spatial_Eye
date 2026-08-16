package com.facialai;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

/**
 * AccessibilityManager serves as the central bridge receiving AI model detection output,
 * parsing fields, formulating natural voice announcements, triggering vibration patterns,
 * and managing emergency mode.
 */
public class AccessibilityManager {
    private static final String TAG = "AccessibilityManager";

    public interface UiUpdateListener {
        void onStatusUpdated(String statusText);
        void onDetectionProcessed(String formattedMessage, boolean isDanger);
        void onVibrationStateChanged(boolean isEnabled);
    }

    private final VoiceManager voiceManager;
    private final VibrationManager vibrationManager;
    private UiUpdateListener uiListener;
    private boolean isDetectionActive = false;

    public AccessibilityManager(Context context) {
        this.voiceManager = new VoiceManager(context);
        this.vibrationManager = new VibrationManager(context);

        this.voiceManager.setVoiceCommandListener(new VoiceManager.VoiceCommandListener() {
            @Override
            public void onStartCommand() {
                startDetection();
            }

            @Override
            public void onStopCommand() {
                stopDetection();
            }

            @Override
            public void onRepeatCommand() {
                repeatInstructions();
            }

            @Override
            public void onEmergencyCommand() {
                triggerEmergencyMode();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Voice Command Error: " + errorMessage);
            }
        });
    }

    public void setUiUpdateListener(UiUpdateListener listener) {
        this.uiListener = listener;
    }

    public VoiceManager getVoiceManager() {
        return voiceManager;
    }

    public VibrationManager getVibrationManager() {
        return vibrationManager;
    }

    public boolean isDetectionActive() {
        return isDetectionActive;
    }

    /**
     * Starts the AI detection service.
     */
    public void startDetection() {
        isDetectionActive = true;
        String msg = "Detection started. Monitoring environment.";
        voiceManager.speak(msg);
        vibrationManager.vibrateNormalObject();
        if (uiListener != null) {
            uiListener.onStatusUpdated("Status: Active");
        }
    }

    /**
     * Stops the AI detection service.
     */
    public void stopDetection() {
        isDetectionActive = false;
        vibrationManager.cancelVibration();
        String msg = "Detection stopped.";
        voiceManager.speak(msg);
        if (uiListener != null) {
            uiListener.onStatusUpdated("Status: Stopped");
        }
    }

    /**
     * Repeats the last voice instruction.
     */
    public void repeatInstructions() {
        voiceManager.repeatLastMessage();
    }

    /**
     * Toggles vibration feedback state.
     */
    public void toggleVibration() {
        boolean newState = vibrationManager.toggleVibration();
        String msg = newState ? "Vibration feedback enabled." : "Vibration feedback disabled.";
        voiceManager.speak(msg);
        if (uiListener != null) {
            uiListener.onVibrationStateChanged(newState);
        }
    }

    /**
     * Processes JSON payload received from the AI model.
     * Example input:
     * {
     *     "object": "chair",
     *     "distance": "2",
     *     "position": "left",
     *     "danger": false
     * }
     */
    public void processAiDetection(JSONObject jsonObject) {
        if (jsonObject == null) return;
        try {
            String objectName = jsonObject.optString("object", "obstacle");
            String distanceStr = jsonObject.optString("distance", "");
            String position = jsonObject.optString("position", "ahead");
            boolean danger = jsonObject.optBoolean("danger", false);
            boolean isChange = jsonObject.optBoolean("is_change", false);

            double distanceMeters = 0;
            try {
                if (!distanceStr.isEmpty()) {
                    distanceMeters = Double.parseDouble(distanceStr);
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Could not parse distance string: " + distanceStr);
            }

            processAiDetection(objectName, distanceMeters, position, danger, isChange);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing AI detection JSON", e);
        }
    }

    /**
     * Core processing logic for structured detection data.
     */
    public void processAiDetection(String objectName, double distanceMeters, String position, boolean danger, boolean isChange) {
        if (!isDetectionActive && !danger) {
            Log.d(TAG, "Detection is paused, ignoring non-danger event.");
            return;
        }

        String voiceMsg = buildVoiceMessage(objectName, distanceMeters, position, danger, isChange);
        voiceManager.speak(voiceMsg);

        // Determine vibration feedback pattern
        if (danger) {
            vibrationManager.vibrateDangerousObstacle();
        } else if (isChange) {
            vibrationManager.vibrateEnvironmentChange();
        } else if (objectName.equalsIgnoreCase("person")) {
            vibrationManager.vibratePersonApproaching();
        } else {
            vibrationManager.vibrateNormalObject();
        }

        if (uiListener != null) {
            uiListener.onDetectionProcessed(voiceMsg, danger);
        }
    }

    /**
     * Formulates natural language announcements for Text-to-Speech.
     */
    private String buildVoiceMessage(String objectName, double distanceMeters, String position, boolean danger, boolean isChange) {
        if (danger) {
            return "Danger! " + capitalize(objectName) + " detected " + formatDistance(distanceMeters) + " " + position + ". Caution required!";
        }
        if (isChange) {
            return "New obstacle ahead: " + capitalize(objectName) + " on your " + position + ".";
        }
        return capitalize(objectName) + " detected " + formatDistance(distanceMeters) + " " + formatPosition(position) + ".";
    }

    private String formatDistance(double meters) {
        if (meters <= 0) return "";
        if (meters == (int) meters) {
            return (int) meters + " meters";
        }
        return String.format("%.1f meters", meters);
    }

    private String formatPosition(String pos) {
        if (pos == null || pos.isEmpty()) return "ahead";
        if (pos.equalsIgnoreCase("left")) return "on your left";
        if (pos.equalsIgnoreCase("right")) return "on your right";
        if (pos.equalsIgnoreCase("center") || pos.equalsIgnoreCase("ahead")) return "ahead";
        return pos;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "Object";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Emergency Mode Trigger.
     */
    public void triggerEmergencyMode() {
        vibrationManager.vibrateEmergencyMode();
        String emergencyMsg = "Emergency mode activated! High risk danger nearby. Stop moving and seek assistance.";
        voiceManager.speak(emergencyMsg);
        if (uiListener != null) {
            uiListener.onStatusUpdated("STATUS: EMERGENCY ALERT!");
            uiListener.onDetectionProcessed(emergencyMsg, true);
        }
    }

    /**
     * Releases resources on activity destruction.
     */
    public void shutdown() {
        vibrationManager.cancelVibration();
        voiceManager.shutdown();
    }
}
