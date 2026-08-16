package com.facialai

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.facialai.databinding.ActivityAlertBinding
import com.facialai.utils.SpeechHelper
import com.facialai.utils.VibrationHelper

class AlertActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlertBinding
    private lateinit var speechHelper: SpeechHelper
    private lateinit var vibrationHelper: VibrationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        speechHelper = SpeechHelper(this)
        vibrationHelper = VibrationHelper(this)

        val changesDetected = intent.getBooleanExtra("CHANGES_DETECTED", false)
        val ttsMessage = intent.getStringExtra("TTS_MESSAGE") ?: ""
        val alertMessages = intent.getStringArrayListExtra("ALERT_MESSAGES") ?: emptyList<String>()

        setupUI(changesDetected, ttsMessage, alertMessages)

        // Give TTS time to initialize, delay speaking slightly
        binding.root.postDelayed({
            speechHelper.speak(ttsMessage)
            vibrationHelper.vibrateForRisk(if (changesDetected) "HIGH" else "NONE")
        }, 500)

        binding.btnSpeakAgain.setOnClickListener {
            speechHelper.speak(ttsMessage)
        }
    }

    private fun setupUI(changesDetected: Boolean, ttsMessage: String, alertMessages: List<String>) {
        if (!changesDetected) {
            binding.tvHeader.text = "NO IMPORTANT CHANGES"
            val successColor = ContextCompat.getColor(this, R.color.color_success)
            binding.tvHeader.setTextColor(successColor)
            binding.tvRiskLevel.text = "SAFE"
            binding.tvRiskLevel.setTextColor(successColor)
            binding.tvMessage.text = ttsMessage
            binding.tvChangesList.text = ""
        } else {
            binding.tvHeader.text = "CHANGE DETECTED"
            val riskColor = ContextCompat.getColor(this, R.color.color_high_risk)
            binding.tvHeader.setTextColor(riskColor)
            binding.tvRiskLevel.text = "RISK LEVEL: HIGH"
            binding.tvRiskLevel.setTextColor(riskColor)
            
            binding.tvMessage.text = ttsMessage
            
            val sb = StringBuilder()
            for (change in alertMessages) {
                sb.append("- ").append(change).append("\n")
            }
            binding.tvChangesList.text = sb.toString()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechHelper.shutdown()
    }
}
