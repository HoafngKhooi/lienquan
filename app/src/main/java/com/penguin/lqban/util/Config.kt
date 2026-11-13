package com.penguin.lqban.util

object Config {
    const val SCAN_INTERVAL_MS = 1500L
    const val TEMPLATE_THRESHOLD = 0.82

    data class Roi(val x: Float, val y: Float, val w: Float, val h: Float)

    val BLUE_BANS = Roi(0.02f, 0.05f, 0.28f, 0.22f)
    val RED_BANS  = Roi(0.70f, 0.05f, 0.28f, 0.22f)
    val ALLY_PICKS = Roi(0.02f, 0.30f, 0.20f, 0.50f)

    val OVERLAY = Roi(0.15f, 0.72f, 0.70f, 0.12f)
}
