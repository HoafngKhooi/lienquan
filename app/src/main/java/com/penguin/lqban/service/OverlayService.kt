package com.penguin.lqban.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class OverlayService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("OverlayService", "Phase 3 chưa implement — stub đang chạy")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i("OverlayService", "stopped")
        super.onDestroy()
    }
}
