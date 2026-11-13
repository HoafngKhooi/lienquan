package com.penguin.lqban.vision

import android.graphics.Bitmap

object NccMatcher {
    /**
     * So khớp Template (avatar) với một vùng của Source (ROI) bằng thuật toán NCC.
     * Trả về điểm số cao nhất (từ 0.0 đến 1.0).
     */
    fun match(source: Bitmap, template: Bitmap, threshold: Double = 0.82): Boolean {
        val sw = source.width
        val sh = source.height
        val tw = template.width
        val th = template.height

        if (tw > sw || th > sh) return false

        // Chuyển sang mảng pixel để tính toán nhanh hơn
        val sPixels = IntArray(sw * sh)
        source.getPixels(sPixels, 0, sw, 0, 0, sw, sh)

        val tPixels = IntArray(tw * th)
        template.getPixels(tPixels, 0, tw, 0, 0, tw, th)

        // Tính mean (giá trị trung bình) của template (dùng kênh màu Green để tối ưu tốc độ)
        var tMean = 0.0
        for (i in tPixels.indices) {
            tMean += (tPixels[i] shr 8) and 0xFF // Lấy kênh Green
        }
        tMean /= (tw * th)

        val maxX = sw - tw
        val maxY = sh - th

        // Bước nhảy (stride) để tăng tốc độ, có thể set = 2 hoặc 4 nếu ảnh lớn
        val step = 2 

        var maxScore = 0.0

        for (y in 0..maxY step step) {
            for (x in 0..maxX step step) {
                // Tính NCC cho vùng hiện tại
                var sMean = 0.0
                for (ty in 0 until th step step) {
                    for (tx in 0 until tw step step) {
                        val sPixel = sPixels[(y + ty) * sw + (x + tx)]
                        sMean += (sPixel shr 8) and 0xFF
                    }
                }
                sMean /= ((tw / step) * (th / step)).toDouble()

                var num = 0.0
                var denS = 0.0
                var denT = 0.0

                for (ty in 0 until th step step) {
                    for (tx in 0 until tw step step) {
                        val sVal = ((sPixels[(y + ty) * sw + (x + tx)] shr 8) and 0xFF).toDouble()
                        val tVal = ((tPixels[ty * tw + tx] shr 8) and 0xFF).toDouble()

                        val diffS = sVal - sMean
                        val diffT = tVal - tMean

                        num += diffS * diffT
                        denS += diffS * diffS
                        denT += diffT * diffT
                    }
                }

                val score = if (denS == 0.0 || denT == 0.0) 0.0 else num / Math.sqrt(denS * denT)
                if (score > maxScore) {
                    maxScore = score
                }

                if (maxScore >= threshold) return true // Early exit nếu đã vượt ngưỡng
            }
        }
        return false
    }
}
