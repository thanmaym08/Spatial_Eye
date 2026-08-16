package com.facialai.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class SpeechHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("SpeechHelper", "Language not supported")
            } else {
                isReady = true
                tts.setSpeechRate(1.05f)
            }
        } else {
            Log.e("SpeechHelper", "Initialization failed")
        }
    }

    fun speak(text: String) {
        if (isReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun speakQueued(text: String) {
        if (isReady) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    fun stop() {
        if (isReady) {
            tts.stop()
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
