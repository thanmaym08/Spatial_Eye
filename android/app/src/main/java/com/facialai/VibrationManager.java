package com.facialai;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

/**
 * VibrationManager handles tactile feedback and haptic vibration patterns
 * for object detection, danger alerts, environment changes, and emergency mode.
 */
public class VibrationManager {
    private static final String TAG = "VibrationManager";

    private final Vibrator vibrator;
    private boolean isVibrationEnabled = true;

    public VibrationManager(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            this.vibrator = (vibratorManager != null) ? vibratorManager.getDefaultVibrator() : null;
        } else {
            this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    public boolean isVibrationEnabled() {
        return isVibrationEnabled;
    }

    public void setVibrationEnabled(boolean enabled) {
        this.isVibrationEnabled = enabled;
        if (!enabled) {
            cancelVibration();
        }
    }

    public boolean toggleVibration() {
        setVibrationEnabled(!isVibrationEnabled);
        return isVibrationEnabled;
    }

    /**
     * Pattern 1: Short vibration for normal object detection.
     */
    public void vibrateNormalObject() {
        if (!isVibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(100);
        }
    }

    /**
     * Pattern 2: Long vibration for dangerous obstacles.
     */
    public void vibrateDangerousObstacle() {
        if (!isVibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(800);
        }
    }

    /**
     * Pattern 3: Double vibration for environment change.
     */
    public void vibrateEnvironmentChange() {
        if (!isVibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = {0, 150, 100, 150};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    /**
     * Pattern 4: Repeated vibration for approaching person.
     */
    public void vibratePersonApproaching() {
        if (!isVibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = {0, 200, 100, 200, 100, 200};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    /**
     * Pattern 5: Continuous vibration pulse for emergency mode.
     */
    public void vibrateEmergencyMode() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = {0, 400, 100, 400, 100, 400, 100};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    /**
     * Cancels any active vibration.
     */
    public void cancelVibration() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
}
