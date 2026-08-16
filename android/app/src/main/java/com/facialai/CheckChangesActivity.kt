package com.facialai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.facialai.api.RetrofitClient
import com.facialai.databinding.ActivityCheckChangesBinding
import com.facialai.models.Place
import com.facialai.models.CheckChangesResponse
import com.facialai.utils.CameraHelper
import com.facialai.utils.SpeechHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class CheckChangesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCheckChangesBinding
    private lateinit var cameraHelper: CameraHelper
    private lateinit var speechHelper: SpeechHelper
    private var placesList: List<Place> = emptyList()

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraHelper.startCamera(binding.viewFinder)
        } else {
            val msg = "Camera permission is required to check changes"
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            speechHelper.speak(msg)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckChangesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        speechHelper = SpeechHelper(this)
        cameraHelper = CameraHelper(this, this)
        checkAndStartCamera()


        loadPlaces()

        binding.btnCheck.setOnClickListener {
            val selectedPosition = binding.spinnerPlaces.selectedItemPosition
            if (selectedPosition < 0 || selectedPosition >= placesList.size) {
                speechHelper.speak("Please select a place first.")
                return@setOnClickListener
            }

            val selectedPlace = placesList[selectedPosition]
            
            speechHelper.speak("Checking for changes, please hold the camera steady.")
            binding.progressBar.visibility = View.VISIBLE
            binding.btnCheck.isEnabled = false

            cameraHelper.captureImage { imageBytes ->
                if (imageBytes != null) {
                    checkChangesOnServer(selectedPlace.id, imageBytes)
                } else {
                    handleError("Failed to capture image")
                }
            }
        }
    }

    private fun loadPlaces() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getPlaces()
                }
                if (response.isSuccessful) {
                    placesList = response.body() ?: emptyList()
                    val adapter = ArrayAdapter(
                        this@CheckChangesActivity,
                        android.R.layout.simple_spinner_item,
                        placesList.map { it.placeName }
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerPlaces.adapter = adapter
                } else {
                    Toast.makeText(this@CheckChangesActivity, "Failed to load places", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CheckChangesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun checkChangesOnServer(placeId: String, imageBytes: ByteArray) {
        lifecycleScope.launch {
            try {
                val placeIdBody = placeId.toRequestBody("text/plain".toMediaTypeOrNull())
                val imageBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("file", "check.jpg", imageBody)

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.checkChanges(placeIdBody, imagePart)
                }

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!.changeResult
                    val intent = Intent(this@CheckChangesActivity, AlertActivity::class.java).apply {
                        putExtra("CHANGES_DETECTED", result.hasImportantChanges)
                        putExtra("TTS_MESSAGE", result.ttsMessage)
                        putStringArrayListExtra("ALERT_MESSAGES", ArrayList(result.alertMessages))
                    }
                    startActivity(intent)
                    finish()
                } else {
                    handleError("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                handleError("Network error: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnCheck.isEnabled = true
            }
        }
    }

    private fun handleError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnCheck.isEnabled = true
        speechHelper.speak("Error checking changes")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun checkAndStartCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraHelper.startCamera(binding.viewFinder)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechHelper.shutdown()
    }
}

