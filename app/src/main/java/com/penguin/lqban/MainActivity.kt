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

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var mediaProjectionManager: MediaProjectionManager

    // Nhận kết quả từ hộp thoại xin quyền chụp màn hình
    private val requestCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            tvStatus.text = "Đã cấp quyền chụp"
            Toast.makeText(this, "Sẵn sàng bắt đầu!", Toast.LENGTH_SHORT).show()
            
            // Lưu tạm data vào biến toàn cục hoặc truyền thẳng qua Service (ở đây truyền thẳng)
            val intent = Intent(this, com.penguin.lqban.service.CaptureService::class.java).apply {
                action = "START_CAPTURE"
                putExtra("resultCode", result.resultCode)
                putExtra("data", data)
            }
            ContextCompat.startForegroundService(this, intent)
        } else {
            Toast.makeText(this, "Từ chối cấp quyền!", Toast.LENGTH_SHORT).show()
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
            val intent = mediaProjectionManager.createScreenCaptureIntent()
            requestCapture.launch(intent)
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            // Nếu đã cấp quyền, khởi động lại service
            val intent = Intent(this, com.penguin.lqban.service.CaptureService::class.java).apply {
                action = "START_RECORDING"
            }
            ContextCompat.startForegroundService(this, intent)
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            val intent = Intent(this, com.penguin.lqban.service.CaptureService::class.java).apply {
                action = "STOP_RECORDING"
            }
            startService(intent)
        }
    }
}
