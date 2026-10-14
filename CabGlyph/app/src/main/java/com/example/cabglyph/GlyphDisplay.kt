package com.example.cabglyph

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject

/** Shows the cab info on the Glyph Matrix from inside our app (not as a Glyph Toy). */
class GlyphDisplay(private val context: Context) {
    private var gm: GlyphMatrixManager? = null
    private var connected = false
    private var pending: CabInfo? = null
    private val handler = Handler(Looper.getMainLooper())
    private val clearTask = Runnable { clear() }

    /** How long to keep the code on the matrix after the notification arrives. */
    var displayMillis: Long = 45 * 60 * 1000L

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

    fun show(info: CabInfo) {
        pending = info
        val m = gm
        if (m == null || !connected) { start(); return }
        try {
            val obj = GlyphMatrixObject.Builder()
                .setImageSource(MatrixRenderer.render(info.otp, info.cabLast4))
                .setPosition(0, 0)
                .setScale(100)
                .setBrightness(255)
                .build()
            val frame = GlyphMatrixFrame.Builder().addTop(obj).build(context)
            m.setAppMatrixFrame(frame)
            handler.removeCallbacks(clearTask)
            handler.postDelayed(clearTask, displayMillis)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to draw on Glyph Matrix", e)
        }
    }

    fun clear() {
        pending = null
        handler.removeCallbacks(clearTask)
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
