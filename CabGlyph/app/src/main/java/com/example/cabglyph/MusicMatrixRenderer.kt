package com.example.cabglyph

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Now-playing frame for the 25x25 Glyph Matrix:
 *  - album art as a contrast-stretched greyscale disc in the middle
 *  - a ring around the edge that fills clockwise with track progress
 */
object MusicMatrixRenderer {
    private const val SIZE = 25
    private const val C = 12f // centre pixel
    private const val ART_RADIUS = 8.6f
    private const val RING_INNER = 10.4f
    private const val RING_OUTER = 11.6f

    // Cache the downscaled art so progress ticks don't rescale every time.
    private var cachedSource: Bitmap? = null
    private var cachedLuma: IntArray? = null

    // 5x7 music note used when the player gives no artwork.
    private val NOTE = intArrayOf(0b00111, 0b00101, 0b00100, 0b00100, 0b11100, 0b11100, 0b11000)

    fun render(np: NowPlaying): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.BLACK)

        val luma = np.art?.let { lumaFor(it) }
        if (luma != null) {
            for (y in 0 until SIZE) for (x in 0 until SIZE) {
                if (dist(x, y) <= ART_RADIUS) {
                    val v = luma[y * SIZE + x]
                    bmp.setPixel(x, y, Color.rgb(v, v, v))
                }
            }
        } else {
            for (row in NOTE.indices) for (col in 0 until 5) {
                if (NOTE[row] shr (4 - col) and 1 == 1) bmp.setPixel(10 + col, 9 + row, Color.WHITE)
            }
        }

        val progress = np.progress()
        val dim = Color.rgb(45, 45, 45)
        for (y in 0 until SIZE) for (x in 0 until SIZE) {
            val d = dist(x, y)
            if (d < RING_INNER || d > RING_OUTER) continue
            // angle from 12 o'clock, clockwise, 0..1
            var a = (atan2((x - C).toDouble(), (C - y).toDouble()) / (2 * PI)).toFloat()
            if (a < 0) a += 1f
            val lit = progress >= 0f && a <= progress
            bmp.setPixel(x, y, if (lit) Color.WHITE else dim)
        }
        return bmp
    }

    private fun dist(x: Int, y: Int): Float {
        val dx = x - C; val dy = y - C
        return sqrt(dx * dx + dy * dy)
    }

    private fun lumaFor(art: Bitmap): IntArray {
        if (art === cachedSource) cachedLuma?.let { return it }
        val small = Bitmap.createScaledBitmap(art.copy(Bitmap.Config.ARGB_8888, false), SIZE, SIZE, true)
        val raw = IntArray(SIZE * SIZE) { i ->
            val p = small.getPixel(i % SIZE, i / SIZE)
            (0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p)).toInt()
        }
        // Auto-contrast so dark covers still read on the LEDs.
        val lo = raw.min(); val hi = raw.max()
        val span = (hi - lo).coerceAtLeast(1)
        val out = IntArray(raw.size) { i -> 20 + (raw[i] - lo) * 235 / span }
        cachedSource = art
        cachedLuma = out
        return out
    }
}
