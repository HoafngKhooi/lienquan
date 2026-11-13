package com.penguin.lqban

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvStatus = findViewById(R.id.tvStatus)

        findViewById<Button>(R.id.btnGrantOverlay).setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        findViewById<Button>(R.id.btnGrantCapture).setOnClickListener {
            Toast.makeText(this, "Phase 2 sẽ implement", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            Toast.makeText(this, "Phase 2 sẽ implement", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            Toast.makeText(this, "Phase 2 sẽ implement", Toast.LENGTH_SHORT).show()
        }
    }
}
