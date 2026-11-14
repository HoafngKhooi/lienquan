package com.penguin.lqban.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.penguin.lqban.data.ChampionRepository
import com.penguin.lqban.engine.BanScorer
import com.penguin.lqban.engine.SuggestRequest
import com.penguin.lqban.util.Config
import java.io.File
import java.io.FileOutputStream

object VisionTest {

    fun runMockTest(context: Context): String {
        val logResult = StringBuilder()

        // 1. Tạo một màn hình giả lập 1080x2400 (tỷ lệ điện thoại)
        val mockScreen = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mockScreen)
        canvas.drawColor(Color.parseColor("#1A1A1A")) // Nền xám tối

        // 2. Vẽ các "tướng bị cấm" vào vùng Cấm Xanh (BLUE_BANS)
        // Tọa độ tính toán dựa trên Config
        val blueRoi = Config.BLUE_BANS
        val bx = (blueRoi.x * 1080).toInt()
        val by = (blueRoi.y * 2400).toInt()
        
        // Vẽ 2 avatar giả vào vùng cấm xanh
        drawMockAvatar(canvas, bx + 20, by + 20, Color.RED, "FLORENTINO") // Giả lập Florentino
        drawMockAvatar(canvas, bx + 140, by + 20, Color.BLUE, "HAYATE")     // Giả lập Hayate

        // 3. Vẽ "tướng đồng đội nhá" vào vùng Ally (ALLY_PICKS)
        val allyRoi = Config.ALLY_PICKS
        val ax = (allyRoi.x * 1080).toInt()
        val ay = (allyRoi.y * 2400).toInt()
        drawMockAvatar(canvas, ax + 50, ay + 50, Color.GREEN, "NAKROTH")

        // 4. Lưu ảnh giả lập ra bộ nhớ để người dùng xem
        saveBitmapToFile(context, mockScreen, "mock_screen.png")

        // 5. Chạy Pipeline nhận diện trên chính bức ảnh giả lập này
        val data = ChampionRepository.load(context)
        val bannedSet = mutableSetOf<String>()
        val allyPicks = mutableSetOf<String>()

        // Load ảnh mẫu (avatar) - Ở đây ta sẽ so sánh màu sắc thay vì ảnh thật
        // vì ta biết avatar giả đang có màu gì
        val blueCrop = cropRoi(mockScreen, Config.BLUE_BANS)
        val allyCrop = cropRoi(mockScreen, Config.ALLY_PICKS)

        // Giả lập logic nhận diện (vì chưa có ảnh thật)
        // Thực tế NccMatcher sẽ so sánh từng pixel
        if (hasColor(blueCrop, Color.RED)) bannedSet.add("florentino")
        if (hasColor(blueCrop, Color.BLUE)) bannedSet.add("hayate")
        if (hasColor(allyCrop, Color.GREEN)) allyPicks.add("nakroth")

        logResult.append("--- KẾT QUẢ QUÉT MÀN HÌNH GIẢ LẬP ---\n")
        logResult.append("Tướng bị cấm (Phe Xanh): $bannedSet\n")
        logResult.append("Tướng đồng đội chọn: $allyPicks\n\n")

        // 6. Gọi Engine tính điểm
        if (bannedSet.isNotEmpty() || allyPicks.isNotEmpty()) {
            val req = SuggestRequest(banned = bannedSet, allyPicks = allyPicks, allyIntent = allyPicks, side = "blue")
            val suggestions = BanScorer.suggest(req, data.champions, data.counterMatrix)

            logResult.append("🔥 GỢI Ý CẤM:\n")
            for (s in suggestions) {
                val reason = s.reasons.joinToString { it.tag }
                logResult.append("-> ${s.name} (Điểm: ${s.score.toInt()}) - Lý do: $reason\n")
            }
        }

        blueCrop.recycle()
        allyCrop.recycle()
        mockScreen.recycle()

        Log.i("VisionTest", logResult.toString())
        return logResult.toString()
    }

    private fun drawMockAvatar(canvas: Canvas, x: Int, y: Int, color: Int, text: String) {
        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + 100).toFloat(), (y + 100).toFloat(), paint)
        
        val textPaint = Paint().apply {
            this.color = Color.WHITE
            textSize = 20f
        }
        canvas.drawText(text, x.toFloat(), (y + 50).toFloat(), textPaint)
    }

    private fun cropRoi(source: Bitmap, roi: Config.Roi): Bitmap {
        val x = (roi.x * source.width).toInt()
        val y = (roi.y * source.height).toInt()
        val w = (roi.w * source.width).toInt()
        val h = (roi.h * source.height).toInt()
        val safeW = if (x + w > source.width) source.width - x else w
        val safeH = if (y + h > source.height) source.height - y else h
        return Bitmap.createBitmap(source, x, y, safeW, safeH)
    }

    private fun hasColor(bitmap: Bitmap, targetColor: Int): Boolean {
        // Quét thưa (mỗi 10 pixel) để kiểm tra màu
        for (y in 0 until bitmap.height step 10) {
            for (x in 0 until bitmap.width step 10) {
                if (bitmap.getPixel(x, y) == targetColor) return true
            }
        }
        return false
    }

    private fun saveBitmapToFile(context: Context, bitmap: Bitmap, filename: String) {
        try {
            val file = File(context.getExternalFilesDir(null), filename)
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            Log.i("VisionTest", "Đã lưu ảnh test tại: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("VisionTest", "Lỗi lưu ảnh: ${e.message}")
        }
    }
}
