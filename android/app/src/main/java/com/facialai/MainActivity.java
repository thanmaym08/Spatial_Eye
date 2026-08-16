package com.facialai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * MainActivity serves as the merged primary dashboard connecting
 * the Java Accessibility Module (Voice Guidance, Voice Commands, Vibration Feedback)
 * with the Spatial Memory Camera features (Save Place, Check Changes, My Places).
 */
public class MainActivity extends AppCompatActivity implements AccessibilityManager.UiUpdateListener {

    private static final int PERMISSION_REQUEST_CODE = 201;

    private AccessibilityManager accessibilityManager;
    private TextView tvStatus;
    private TextView tvDetectionInfo;
    private Button btnSavePlace;
    private Button btnCheckChanges;
    private Button btnMyPlaces;
    private Button btnVoice;
    private Button btnVibrationToggle;
    private Button btnEmergency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI Elements
        tvStatus = findViewById(R.id.tv_status);
        tvDetectionInfo = findViewById(R.id.tv_detection_info);
        btnSavePlace = findViewById(R.id.btn_save_place);
        btnCheckChanges = findViewById(R.id.btn_check_changes);
        btnMyPlaces = findViewById(R.id.btn_my_places);
        btnVoice = findViewById(R.id.btn_voice);
        btnVibrationToggle = findViewById(R.id.btn_vibration_toggle);
        btnEmergency = findViewById(R.id.btn_emergency);

        // Initialize Accessibility Controller
        accessibilityManager = new AccessibilityManager(this);
        accessibilityManager.setUiUpdateListener(this);
        accessibilityManager.startDetection();

        // Check Permissions
        checkPermissions();

        // Spatial Memory Action Listeners
        btnSavePlace.setOnClickListener(v -> {
            accessibilityManager.getVoiceManager().speak("Opening camera to save new place.");
            startActivity(new Intent(MainActivity.this, SavePlaceActivity.class));
        });

        btnCheckChanges.setOnClickListener(v -> {
            accessibilityManager.getVoiceManager().speak("Opening camera to check for environmental changes.");
            startActivity(new Intent(MainActivity.this, CheckChangesActivity.class));
        });

        btnMyPlaces.setOnClickListener(v -> {
            accessibilityManager.getVoiceManager().speak("Opening your saved places library.");
            startActivity(new Intent(MainActivity.this, MyPlacesActivity.class));
        });

        // Accessibility Action Listeners
        btnVoice.setOnClickListener(v -> accessibilityManager.getVoiceManager().startListening());
        btnVibrationToggle.setOnClickListener(v -> accessibilityManager.toggleVibration());
        btnEmergency.setOnClickListener(v -> accessibilityManager.triggerEmergencyMode());

        // Simulate initial AI object detection
        simulateSampleAiDetection();
    }

    /**
     * Checks and requests runtime permissions for Camera, Audio Recording, and Vibration.
     */
    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.VIBRATE
        };

        boolean missing = false;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                missing = true;
                break;
            }
        }

        if (missing) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            boolean audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
            if (!cameraGranted || !audioGranted) {
                Toast.makeText(this, "Camera and Audio permissions are required for full features.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Callback for system status updates.
     */
    @Override
    public void onStatusUpdated(String statusText) {
        runOnUiThread(() -> {
            tvStatus.setText(statusText);
            if (statusText.contains("EMERGENCY")) {
                tvStatus.setTextColor(Color.RED);
            } else if (statusText.contains("Active")) {
                tvStatus.setTextColor(Color.GREEN);
            } else {
                tvStatus.setTextColor(Color.YELLOW);
            }
        });
    }

    /**
     * Callback when an AI detection is processed. Updates UI text and accessibility announcement.
     */
    @Override
    public void onDetectionProcessed(String formattedMessage, boolean isDanger) {
        runOnUiThread(() -> {
            tvDetectionInfo.setText(formattedMessage);
            tvDetectionInfo.setContentDescription(formattedMessage);
            if (isDanger) {
                tvDetectionInfo.setTextColor(Color.RED);
            } else {
                tvDetectionInfo.setTextColor(Color.WHITE);
            }
        });
    }

    /**
     * Callback when vibration toggle state changes.
     */
    @Override
    public void onVibrationStateChanged(boolean isEnabled) {
        runOnUiThread(() -> {
            btnVibrationToggle.setText(isEnabled ? "VIBRATION: ON" : "VIBRATION: OFF");
            btnVibrationToggle.setContentDescription(isEnabled ? "Vibration is enabled. Tap to disable." : "Vibration is disabled. Tap to enable.");
        });
    }

    /**
     * Simulates receiving data payload from the AI detection model.
     */
    private void simulateSampleAiDetection() {
        tvDetectionInfo.postDelayed(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("object", "chair");
                json.put("distance", "2");
                json.put("position", "left");
                json.put("danger", false);

                if (accessibilityManager != null) {
                    accessibilityManager.processAiDetection(json);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, 1500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (accessibilityManager != null) {
            accessibilityManager.shutdown();
        }
    }
}
