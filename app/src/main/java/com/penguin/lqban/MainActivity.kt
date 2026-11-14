package com.penguin.lqban

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.penguin.lqban.service.CaptureService
import com.penguin.lqban.vision.VisionTest

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val requestCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            tvStatus.text = "Đã cấp quyền, sẵn sàng chạy ngầm"
            val intent = Intent(this, CaptureService::class.java).apply {
                action = "START_CAPTURE"
                putExtra("resultCode", result.resultCode)
                putExtra("data", data)
            }
            ContextCompat.startForegroundService(this, intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvStatus = findViewById(R.id.tvStatus)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        findViewById<Button>(R.id.btnGrantOverlay).setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }

        findViewById<Button>(R.id.btnGrantCapture).setOnClickListener {
            requestCapture.launch(mediaProjectionManager.createScreenCaptureIntent())
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            val intent = Intent(this, CaptureService::class.java).apply { action = "START_RECORDING" }
            ContextCompat.startForegroundService(this, intent)
            Toast.makeText(this, "Đang chạy ngầm...", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            val intent = Intent(this, CaptureService::class.java).apply { action = "STOP_RECORDING" }
            startService(intent)
            Toast.makeText(this, "Đã dừng", Toast.LENGTH_SHORT).show()
        }

        // Nút Test Giả Lập
        findViewById<Button>(R.id.btnTestMock).setOnClickListener {
            tvStatus.text = "Đang chạy Test Giả Lập..."
            val resultText = VisionTest.runMockTest(this)
            tvStatus.text = resultText
            Toast.makeText(this, "Test xong! Xem kết quả trên màn hình.", Toast.LENGTH_LONG).show()
        }
    }
}
