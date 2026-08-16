package com.facialai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.facialai.databinding.ActivityMainBinding
import com.facialai.utils.SpeechHelper

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var speechHelper: SpeechHelper

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        speechHelper = SpeechHelper(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnSavePlace.setOnClickListener {
            speechHelper.speak(getString(R.string.speak_save_place))
            startActivity(Intent(this, SavePlaceActivity::class.java))
        }

        binding.btnCheckChanges.setOnClickListener {
            speechHelper.speak(getString(R.string.speak_check_changes))
            startActivity(Intent(this, CheckChangesActivity::class.java))
        }

        binding.btnMyPlaces.setOnClickListener {
            speechHelper.speak(getString(R.string.speak_my_places))
            startActivity(Intent(this, MyPlacesActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechHelper.isInitialized) {
            speechHelper.shutdown()
        }
    }
}
