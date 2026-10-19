package com.example.cabglyph

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Decides what the Glyph Matrix shows. Priority:
 *   1. Cab OTP + cab number (for [cabDisplayMillis] after the message arrives)
 *   2. Now playing — album art + progress ring (if enabled and music is playing)
 *   3. Nothing (matrix released)
 */
class MatrixController(context: Context, private val prefs: Prefs) {
    private val display = GlyphDisplay(context)
    private val handler = Handler(Looper.getMainLooper())

    private var cab: CabInfo? = null
    private var music: NowPlaying? = null

    /** How long to keep the cab code on the matrix after the notification arrives. */
    var cabDisplayMillis: Long = 45 * 60 * 1000L
    /** How often the progress ring is redrawn while music is showing. */
    var musicTickMillis: Long = 3_000L

    private val cabExpiry = Runnable { cab = null; refresh() }
    private val musicTick = Runnable { refresh() }

    fun start() = display.start()

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        display.stop()
    }

    fun showCab(info: CabInfo) {
        cab = info
        handler.removeCallbacks(cabExpiry)
        handler.postDelayed(cabExpiry, cabDisplayMillis)
        refresh()
    }

    /** "Clear matrix" button: drops the cab code (music, if any, takes over again). */
    fun clearCab() {
        cab = null
        handler.removeCallbacks(cabExpiry)
        refresh()
    }

    fun onMusic(np: NowPlaying?) {
        music = np
        refresh()
    }

    fun refresh() {
        handler.removeCallbacks(musicTick)
        val c = cab
        val m = music
        when {
            c != null -> {
                display.show(MatrixRenderer.render(c.otp, c.cabLast4))
                showing = true
            }
            prefs.musicGlyph && m != null && m.isPlaying -> {
                display.show(MusicMatrixRenderer.render(m))
                showing = true
                handler.postDelayed(musicTick, musicTickMillis)
            }
            showing -> {
                display.clear()
                showing = false
            }
        }
    }

    private var showing = false
}
