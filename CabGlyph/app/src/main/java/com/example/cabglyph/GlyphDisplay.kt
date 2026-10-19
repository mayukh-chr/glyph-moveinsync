package com.example.cabglyph

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject

/**
 * Thin wrapper over the Glyph Matrix SDK: draws a 25x25 frame from inside our app
 * (not as a Glyph Toy). What to draw is decided by [MatrixController].
 */
class GlyphDisplay(private val context: Context) {
    private var gm: GlyphMatrixManager? = null
    private var connected = false
    private var pending: Bitmap? = null

    fun start() {
        if (gm != null) return
        gm = GlyphMatrixManager.getInstance(context.applicationContext).also { m ->
            m.init(object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(name: ComponentName) {
                    m.register(Glyph.DEVICE_23112) // Phone (3)
                    connected = true
                    pending?.let { show(it) }
                }
                override fun onServiceDisconnected(name: ComponentName) {
                    connected = false
                }
            })
        }
    }

    fun show(frameBitmap: Bitmap) {
        pending = frameBitmap
        val m = gm
        if (m == null || !connected) { start(); return }
        try {
            val obj = GlyphMatrixObject.Builder()
                .setImageSource(frameBitmap)
                .setPosition(0, 0)
                .setScale(100)
                .setBrightness(255)
                .build()
            val frame = GlyphMatrixFrame.Builder().addTop(obj).build(context)
            m.setAppMatrixFrame(frame)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to draw on Glyph Matrix", e)
        }
    }

    fun clear() {
        pending = null
        try { gm?.closeAppMatrix() } catch (e: Exception) { Log.e(TAG, "close failed", e) }
    }

    fun stop() {
        clear()
        try { gm?.unInit() } catch (_: Exception) {}
        gm = null
        connected = false
    }

    companion object { private const val TAG = "CabGlyph" }
}
