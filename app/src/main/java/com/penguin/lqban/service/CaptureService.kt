package com.penguin.lqban.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.penguin.lqban.MainActivity
import com.penguin.lqban.R
import com.penguin.lqban.data.ChampionRepository
import com.penguin.lqban.engine.BanScorer
import com.penguin.lqban.engine.SuggestRequest
import com.penguin.lqban.util.Config
import com.penguin.lqban.vision.NccMatcher
import kotlinx.coroutines.*
import java.nio.ByteBuffer

class CaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var isScanning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_CAPTURE" -> {
                startForegroundService()
                val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
                val dataIntent = intent.getParcelableExtra<Intent>("data")
                if (dataIntent != null) {
                    initMediaProjection(resultCode, dataIntent)
                }
            }
            "START_RECORDING" -> {
                if (mediaProjection != null) {
                    startScanning()
                } else {
                    Log.e("CaptureService", "Chưa cấp quyền MediaProjection")
                }
            }
            "STOP_RECORDING" -> {
                stopScanning()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "LQ_BAN_CHANNEL")
            .setContentTitle("LQ Ban Assistant đang chạy")
            .setContentText("Đang phân tích màn hình game...")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .build()
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, notification)
        }
    }

    private fun initMediaProjection(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        // Lấy thông số màn hình thực tế
        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi

        Log.i("CaptureService", "Màn hình: ${screenWidth}x${screenHeight} @ ${screenDensity}dpi")
        startScanning()
    }

    private fun startScanning() {
        if (isScanning) return
        isScanning = true

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "LQBanCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            if (!isScanning) return@setOnImageAvailableListener
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val planes = image.planes
                val buffer: ByteBuffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * screenWidth

                // Tạo bitmap từ buffer
                val bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride,
                    screenHeight, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                
                // Crop bỏ padding
                val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
                bitmap.recycle()

                // Xử lý ảnh trên luồng riêng
                processFrame(croppedBitmap)

            } catch (e: Exception) {
                Log.e("CaptureService", "Lỗi đọc frame: ${e.message}")
            } finally {
                image.close()
            }
        }, null)

        // Vòng lặp quét định kỳ
        serviceScope.launch {
            while (isScanning) {
                delay(Config.SCAN_INTERVAL_MS)
                // Thực tế việc quét được trigger bởi setOnImageAvailableListener
                // delay ở đây chỉ để giới hạn tần suất nếu cần
            }
        }
        Log.i("CaptureService", "Bắt đầu quét màn hình...")
    }

    private fun processFrame(frame: Bitmap) {
        // Load data tĩnh
        val data = ChampionRepository.load(this)
        
        // Cắt 3 vùng ROI (giả định màn hình đang ở chế độ dọc)
        val blueRoi = cropRoi(frame, Config.BLUE_BANS)
        val redRoi = cropRoi(frame, Config.RED_BANS)
        val allyRoi = cropRoi(frame, Config.ALLY_PICKS)

        val bannedSet = mutableSetOf<String>()
        val allyPicks = mutableSetOf<String>()

        // Giả lập việc load avatar (từ assets/avatars/)
        // Lấy danh sách tướng và kiểm tra
        for (champ in data.champions) {
            val avatarBitmap = loadAvatar(champ.id) ?: continue
            
            if (NccMatcher.match(blueRoi, avatarBitmap, Config.TEMPLATE_THRESHOLD) ||
                NccMatcher.match(redRoi, avatarBitmap, Config.TEMPLATE_THRESHOLD)) {
                bannedSet.add(champ.id)
            }
            
            if (NccMatcher.match(allyRoi, avatarBitmap, Config.TEMPLATE_THRESHOLD)) {
                allyPicks.add(champ.id)
            }
        }

        if (bannedSet.isNotEmpty() || allyPicks.isNotEmpty()) {
            val req = SuggestRequest(banned = bannedSet, allyPicks = allyPicks, allyIntent = allyPicks, side = "blue")
            val suggestions = BanScorer.suggest(req, data.champions, data.counterMatrix)
            
            Log.d("VISION_RESULT", "Đã cấm: $bannedSet | Đồng đội: $allyPicks")
            for (s in suggestions) {
                Log.i("VISION_RESULT", "-> Gợi ý: ${s.name} (${s.score.toInt()}) - ${s.reasons.joinToString { it.tag }}")
            }
        }

        blueRoi.recycle()
        redRoi.recycle()
        allyRoi.recycle()
        frame.recycle()
    }

    private fun cropRoi(source: Bitmap, roi: Config.Roi): Bitmap {
        val x = (roi.x * source.width).toInt()
        val y = (roi.y * source.height).toInt()
        val w = (roi.w * source.width).toInt()
        val h = (roi.h * source.height).toInt()
        
        // Đảm bảo không vượt quá biên
        val safeW = if (x + w > source.width) source.width - x else w
        val safeH = if (y + h > source.height) source.height - y else h
        
        return Bitmap.createBitmap(source, x, y, safeW, safeH)
    }

    private fun loadAvatar(id: String): Bitmap? {
        return try {
            val stream = assets.open("avatars/$id.png")
            android.graphics.BitmapFactory.decodeStream(stream)
        } catch (e: Exception) {
            // Chưa có ảnh thật, bỏ qua
            null
        }
    }

    private fun stopScanning() {
        isScanning = false
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("LQ_BAN_CHANNEL", "LQ Ban Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopScanning()
        serviceScope.cancel()
        super.onDestroy()
    }
}
