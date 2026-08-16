package com.facialai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

/**
 * VoiceManager handles Text-to-Speech (TTS) guidance output
 * and SpeechRecognizer input for voice commands.
 */
public class VoiceManager implements TextToSpeech.OnInitListener {
    private static final String TAG = "VoiceManager";

    public interface VoiceCommandListener {
        void onStartCommand();
        void onStopCommand();
        void onRepeatCommand();
        void onEmergencyCommand();
        void onError(String errorMessage);
    }

    private final Context context;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private VoiceCommandListener commandListener;
    private boolean isTtsInitialized = false;
    private String lastSpokenMessage = "";

    public VoiceManager(Context context) {
        this.context = context;
        this.tts = new TextToSpeech(context, this);
        initSpeechRecognizer();
    }

    public void setVoiceCommandListener(VoiceCommandListener listener) {
        this.commandListener = listener;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language US is not supported for TextToSpeech.");
            } else {
                isTtsInitialized = true;
                Log.d(TAG, "TextToSpeech initialized successfully.");
            }
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with status: " + status);
        }
    }

    /**
     * Speaks the specified text message using Android TextToSpeech.
     */
    public void speak(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        lastSpokenMessage = text;
        if (isTtsInitialized && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UtteranceId_" + System.currentTimeMillis());
            Log.d(TAG, "TTS Announcement: " + text);
        } else {
            Log.w(TAG, "TTS not ready yet. Queued message: " + text);
        }
    }

    /**
     * Repeats the last spoken message.
     */
    public void repeatLastMessage() {
        if (!lastSpokenMessage.isEmpty()) {
            speak(lastSpokenMessage);
        } else {
            speak("No previous message to repeat.");
        }
    }

    /**
     * Initializes the SpeechRecognizer for voice command listening.
     */
    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "Listening for voice command...");
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {}

                @Override
                public void onError(int error) {
                    Log.e(TAG, "Speech recognition error code: " + error);
                    if (commandListener != null) {
                        commandListener.onError("Speech recognition error: " + error);
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        processRecognizedCommand(matches.get(0).toLowerCase(Locale.US));
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            Log.w(TAG, "Speech recognition is not available on this device.");
        }
    }

    /**
     * Starts listening for voice commands via SpeechRecognizer.
     */
    public void startListening() {
        if (speechRecognizer != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            speechRecognizer.startListening(intent);
            speak("Listening for voice command.");
        } else {
            if (commandListener != null) {
                commandListener.onError("Speech recognizer unavailable.");
            }
        }
    }

    /**
     * Stops listening for voice commands.
     */
    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    /**
     * Processes recognized spoken commands.
     */
    private void processRecognizedCommand(String command) {
        Log.d(TAG, "Recognized Command: " + command);
        if (commandListener == null) return;

        if (command.contains("start")) {
            commandListener.onStartCommand();
        } else if (command.contains("stop")) {
            commandListener.onStopCommand();
        } else if (command.contains("repeat")) {
            commandListener.onRepeatCommand();
        } else if (command.contains("emergency")) {
            commandListener.onEmergencyCommand();
        } else {
            speak("Unrecognized command: " + command);
        }
    }

    /**
     * Shuts down TTS and SpeechRecognizer resources safely.
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
}
