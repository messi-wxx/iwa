package com.cq.iwa.scan

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cq.iwa.R
import com.cq.iwa.core.ui.ext.applyStatusBar
import com.cq.iwa.databinding.ActivityQrScanBinding
import com.journeyapps.barcodescanner.CaptureManager

class QrScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScanBinding
    private lateinit var capture: CaptureManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyScanStatusBar()
        capture = CaptureManager(this, binding.barcodeView)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.decode()
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun applyScanStatusBar() {
        applyStatusBar(
            color = ContextCompat.getColor(this, R.color.main_background),
            lightBackground = true,
        )
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
        applyScanStatusBar()
    }

    override fun onPause() {
        capture.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        capture.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return binding.barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }
}
