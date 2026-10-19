package com.example.cabglyph

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import java.util.concurrent.Executors

/**
 * Sets a generated lock-screen wallpaper for the current song.
 *
 * Debounced: a track must keep playing for [settleMillis] before we touch the wallpaper,
 * so skipping through a playlist doesn't re-theme Nothing OS (clock / accent colours
 * follow the wallpaper) on every skip.
 */
class LockWallpaperUpdater(context: Context) {
    private val ctx = context.applicationContext
    private val wm = WallpaperManager.getInstance(ctx)
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var pending: Runnable? = null
    private var pendingKey: String? = null
    @Volatile private var appliedKey: String? = null

    var settleMillis: Long = 5_000L

    fun onMusic(np: NowPlaying?) {
        if (np == null || !np.isPlaying) { cancelPending(); return }
        val key = keyOf(np)
        if (key == appliedKey || key == pendingKey) return
        cancelPending()
        pendingKey = key
        val r = Runnable { pending = null; pendingKey = null; apply(np) }
        pending = r
        handler.postDelayed(r, settleMillis)
    }

    /** Apply right away (used by the "apply now" button). */
    fun applyNow(np: NowPlaying) { cancelPending(); apply(np) }

    fun stop() { cancelPending() }

    private fun apply(np: NowPlaying) {
        val key = keyOf(np)
        worker.execute {
            try {
                val size = screenSize()
                val bmp = WallpaperArt.generate(np, size.x, size.y)
                wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
                bmp.recycle()
                appliedKey = key
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set lock wallpaper", e)
            }
        }
    }

    private fun cancelPending() {
        pending?.let { handler.removeCallbacks(it) }
        pending = null
        pendingKey = null
    }

    // Re-apply once if the artwork shows up after the first wallpaper was made without it.
    private fun keyOf(np: NowPlaying) = np.trackKey + if (np.art != null) "|art" else ""

    @Suppress("DEPRECATION")
    private fun screenSize(): Point {
        val display = ctx.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        val p = Point()
        display.getRealSize(p)
        // Portrait orientation regardless of current rotation.
        return if (p.x > p.y) Point(p.y, p.x) else p
    }

    companion object { private const val TAG = "CabGlyph.Wallpaper" }
}
