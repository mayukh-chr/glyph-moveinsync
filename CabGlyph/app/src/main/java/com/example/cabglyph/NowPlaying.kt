package com.example.cabglyph

import android.graphics.Bitmap
import android.os.SystemClock

/** Snapshot of whatever media session is currently playing (or was last playing). */
data class NowPlaying(
    val packageName: String,
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val isPlaying: Boolean,
    val durationMs: Long,
    private val positionMs: Long,
    private val positionUpdatedAt: Long, // SystemClock.elapsedRealtime() base
    private val speed: Float,
) {
    /** Stable id for "this song", used to debounce wallpaper changes. */
    val trackKey: String get() = "$packageName|$title|$artist"

    fun currentPositionMs(now: Long = SystemClock.elapsedRealtime()): Long {
        if (!isPlaying || positionUpdatedAt <= 0) return positionMs
        val pos = positionMs + ((now - positionUpdatedAt) * speed).toLong()
        return if (durationMs > 0) pos.coerceIn(0, durationMs) else pos.coerceAtLeast(0)
    }

    /** 0..1, or -1 when the player doesn't report a duration. */
    fun progress(): Float =
        if (durationMs <= 0) -1f else (currentPositionMs().toFloat() / durationMs).coerceIn(0f, 1f)
}
