package com.example.cabglyph

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Draws two rows of 4 digits onto a 25x25 bitmap using a hand-made 3x5 pixel font,
 * which stays crisp on the Phone (3) circular matrix.
 *
 *   Top row    = OTP
 *   Bottom row = last 4 digits of the cab number
 */
object MatrixRenderer {
    private const val SIZE = 25

    // 3x5 digit font, each row is 3 bits (MSB = left pixel)
    private val FONT = mapOf(
        '0' to intArrayOf(0b111, 0b101, 0b101, 0b101, 0b111),
        '1' to intArrayOf(0b010, 0b110, 0b010, 0b010, 0b111),
        '2' to intArrayOf(0b111, 0b001, 0b111, 0b100, 0b111),
        '3' to intArrayOf(0b111, 0b001, 0b111, 0b001, 0b111),
        '4' to intArrayOf(0b101, 0b101, 0b111, 0b001, 0b001),
        '5' to intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111),
        '6' to intArrayOf(0b111, 0b100, 0b111, 0b101, 0b111),
        '7' to intArrayOf(0b111, 0b001, 0b010, 0b010, 0b010),
        '8' to intArrayOf(0b111, 0b101, 0b111, 0b101, 0b111),
        '9' to intArrayOf(0b111, 0b101, 0b111, 0b001, 0b111),
    )

    fun render(otp: String, cab: String): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.BLACK)
        // 4 digits * 3px + 3 gaps * 1px = 15px wide -> x starts at 5 to centre
        drawRow(bmp, otp, x0 = 5, y0 = 5, color = Color.WHITE)
        // thin divider
        for (x in 8..16) bmp.setPixel(x, 12, Color.rgb(70, 70, 70))
        drawRow(bmp, cab, x0 = 5, y0 = 15, color = Color.rgb(170, 170, 170))
        return bmp
    }

    private fun drawRow(bmp: Bitmap, digits: String, x0: Int, y0: Int, color: Int) {
        digits.take(4).forEachIndexed { i, ch ->
            val glyph = FONT[ch] ?: return@forEachIndexed
            val gx = x0 + i * 4
            for (row in 0 until 5) for (col in 0 until 3) {
                if (glyph[row] shr (2 - col) and 1 == 1) bmp.setPixel(gx + col, y0 + row, color)
            }
        }
    }
}
