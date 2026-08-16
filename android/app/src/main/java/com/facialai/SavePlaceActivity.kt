package com.facialai

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.facialai.api.RetrofitClient
import com.facialai.databinding.ActivitySavePlaceBinding
import com.facialai.utils.CameraHelper
import com.facialai.utils.SpeechHelper
import com.facialai.utils.VibrationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class SavePlaceActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySavePlaceBinding
    private lateinit var cameraHelper: CameraHelper
    private lateinit var speechHelper: SpeechHelper
    private lateinit var vibrationHelper: VibrationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavePlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        speechHelper = SpeechHelper(this)
        vibrationHelper = VibrationHelper(this)
        cameraHelper = CameraHelper(this, this)
        cameraHelper.startCamera(binding.viewFinder)

        binding.btnSave.setOnClickListener {
            val placeName = binding.etPlaceName.text.toString()
            if (placeName.isEmpty()) {
                val msg = "Please enter a place name"
                speechHelper.speak(msg)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            speechHelper.speak("Saving memory, please hold the camera steady.")
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSave.isEnabled = false

            cameraHelper.captureImage { imageBytes ->
                if (imageBytes != null) {
                    savePlaceToServer(placeName, imageBytes)
                } else {
                    handleError("Failed to capture image")
                }
            }
        }
    }

    private fun savePlaceToServer(placeName: String, imageBytes: ByteArray) {
        lifecycleScope.launch {
            try {
                val userIdBody = "default".toRequestBody("text/plain".toMediaTypeOrNull())
                val placeNameBody = placeName.toRequestBody("text/plain".toMediaTypeOrNull())
                
                val imageBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("file", "capture.jpg", imageBody)

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.savePlace(userIdBody, placeNameBody, imagePart)
                }

                if (response.isSuccessful) {
                    vibrationHelper.vibrateSuccess()
                    val msg = "Place saved successfully: $placeName"
                    speechHelper.speak(msg)
                    Toast.makeText(this@SavePlaceActivity, msg, Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    handleError("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                handleError("Network error: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
        }
    }

    private fun handleError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnSave.isEnabled = true
        speechHelper.speak("Error saving place")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechHelper.shutdown()
    }
}
