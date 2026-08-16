package com.facialai.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class VibrationHelper(context: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun vibrateForRisk(riskLevel: String) {
        when (riskLevel.uppercase()) {
            "HIGH" -> vibratePattern(longArrayOf(0, 500, 200, 500, 200, 500))
            "MEDIUM" -> vibratePattern(longArrayOf(0, 300, 200, 300))
            "LOW" -> vibratePattern(longArrayOf(0, 200))
            else -> {}
        }
    }
    
    fun vibrateSuccess() {
        vibratePattern(longArrayOf(0, 100, 100, 100))
    }

    private fun vibratePattern(pattern: LongArray) {
        if (!vibrator.hasVibrator()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
