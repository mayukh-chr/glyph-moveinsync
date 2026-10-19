package com.example.cabglyph

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generative lock-screen art for a track. Deterministic per song (seeded by title + artist),
 * coloured from the album art:
 *  - dark gradient background
 *  - flowing translucent ribbons in the lower two thirds (top stays calm for the clock)
 *  - a 25x25 dot-matrix halftone of the cover, echoing the Glyph Matrix
 */
object WallpaperArt {

    fun generate(np: NowPlaying, width: Int, height: Int): Bitmap {
        val rnd = Random((np.title + "\u0000" + np.artist).hashCode())
        val art = np.art?.copy(Bitmap.Config.ARGB_8888, false)
        val palette = art?.let(::extractPalette)?.takeIf { it.isNotEmpty() } ?: fallbackPalette(rnd)

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val w = width.toFloat(); val h = height.toFloat()

        // 1. Background
        val top = shade(palette[0], 0.16f)
        val bottom = shade(palette.getOrElse(1) { palette[0] }, 0.30f)
        canvas.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP)
        })

        // 2. Ribbons
        val ribbon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        repeat(7 + rnd.nextInt(4)) { i ->
            val baseY = h * (0.38f + rnd.nextFloat() * 0.55f)
            val amp = h * (0.02f + rnd.nextFloat() * 0.07f)
            val freq = 0.6f + rnd.nextFloat() * 1.8f
            val phase = rnd.nextFloat() * 2f * PI.toFloat()
            val tilt = (rnd.nextFloat() - 0.5f) * h * 0.18f
            val path = Path()
            var x = -w * 0.1f
            path.moveTo(x, y(x, w, baseY, amp, freq, phase, tilt))
            while (x <= w * 1.1f) {
                x += w / 60f
                path.lineTo(x, y(x, w, baseY, amp, freq, phase, tilt))
            }
            ribbon.strokeWidth = w * (0.015f + rnd.nextFloat() * 0.12f)
            ribbon.color = brighten(palette[i % palette.size])
            ribbon.alpha = 40 + rnd.nextInt(80)
            canvas.drawPath(path, ribbon)
        }

        // 3. Dot-matrix halftone of the cover
        if (art != null) {
            val grid = 25
            val small = Bitmap.createScaledBitmap(art, grid, grid, true)
            val radius = w * 0.36f
            val cx = w / 2f; val cy = h * 0.64f
            val cell = radius * 2f / grid
            val dot = Paint(Paint.ANTI_ALIAS_FLAG)
            for (gy in 0 until grid) for (gx in 0 until grid) {
                val dx = gx - (grid - 1) / 2f; val dy = gy - (grid - 1) / 2f
                if (dx * dx + dy * dy > (grid / 2f) * (grid / 2f)) continue
                val p = small.getPixel(gx, gy)
                val l = (0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)) / 255f
                dot.color = Color.WHITE
                dot.alpha = (60 + l * 195).toInt()
                canvas.drawCircle(cx + dx * cell, cy + dy * cell, cell * 0.12f + cell * 0.36f * l, dot)
            }
        }

        // 4. Vignette
        canvas.drawRect(0f, 0f, w, h, Paint().apply {
            shader = RadialGradient(w / 2f, h * 0.55f, h * 0.75f,
                intArrayOf(Color.TRANSPARENT, Color.argb(170, 0, 0, 0)),
                floatArrayOf(0.55f, 1f), Shader.TileMode.CLAMP)
        })
        return bmp
    }

    private fun y(x: Float, w: Float, baseY: Float, amp: Float, freq: Float, phase: Float, tilt: Float): Float {
        val t = x / w
        return baseY + tilt * (t - 0.5f) +
            amp * sin(t * freq * 2f * PI.toFloat() + phase) +
            amp * 0.35f * sin(t * freq * 5.3f * PI.toFloat() + phase * 1.7f)
    }

    /** Up to 3 dominant, reasonably distinct colours (hue buckets weighted by saturation). */
    private fun extractPalette(art: Bitmap): List<Int> {
        val small = Bitmap.createScaledBitmap(art, 32, 32, true)
        val buckets = Array(12) { FloatArray(4) } // weight, r, g, b
        val hsv = FloatArray(3)
        for (y in 0 until 32) for (x in 0 until 32) {
            val p = small.getPixel(x, y)
            Color.colorToHSV(p, hsv)
            if (hsv[2] < 0.12f) continue
            val wgt = hsv[1] * hsv[2] + 0.04f
            val b = buckets[(hsv[0] / 30f).toInt().coerceIn(0, 11)]
            b[0] += wgt; b[1] += Color.red(p) * wgt; b[2] += Color.green(p) * wgt; b[3] += Color.blue(p) * wgt
        }
        return buckets.filter { it[0] > 0f }
            .sortedByDescending { it[0] }
            .take(3)
            .map { Color.rgb((it[1] / it[0]).toInt(), (it[2] / it[0]).toInt(), (it[3] / it[0]).toInt()) }
    }

    private fun fallbackPalette(rnd: Random): List<Int> {
        val hue = rnd.nextFloat() * 360f
        return listOf(0f, 40f, 200f).map { Color.HSVToColor(floatArrayOf((hue + it) % 360f, 0.6f, 0.8f)) }
    }

    private fun shade(c: Int, value: Float): Int {
        val hsv = FloatArray(3); Color.colorToHSV(c, hsv)
        hsv[2] = value
        return Color.HSVToColor(hsv)
    }

    private fun brighten(c: Int): Int {
        val hsv = FloatArray(3); Color.colorToHSV(c, hsv)
        hsv[2] = hsv[2].coerceAtLeast(0.7f)
        return Color.HSVToColor(hsv)
    }
}
