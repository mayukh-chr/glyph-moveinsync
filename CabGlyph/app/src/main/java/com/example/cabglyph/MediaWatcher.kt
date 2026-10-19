package com.example.cabglyph

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Follows active media sessions (Spotify, YT Music, etc.). Works because we are an enabled
 * NotificationListenerService — that is what grants access to getActiveSessions().
 *
 * Emits the session that is currently playing, falling back to the most recent one.
 */
class MediaWatcher(
    context: Context,
    private val listenerComponent: ComponentName,
    private val onChange: (NowPlaying?) -> Unit,
) {
    private val msm = context.getSystemService(MediaSessionManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val callbacks = mutableMapOf<MediaController, MediaController.Callback>()
    private var last: NowPlaying? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        track(list.orEmpty())
    }

    fun start() {
        try {
            msm.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, handler)
            track(msm.getActiveSessions(listenerComponent))
        } catch (e: SecurityException) {
            Log.w(TAG, "No media session access (notification access off?)", e)
        }
    }

    fun stop() {
        try { msm.removeOnActiveSessionsChangedListener(sessionsListener) } catch (_: Exception) {}
        callbacks.forEach { (c, cb) -> c.unregisterCallback(cb) }
        callbacks.clear()
        last = null
    }

    fun current(): NowPlaying? = last

    private fun track(controllers: List<MediaController>) {
        val tokens = controllers.map { it.sessionToken }.toSet()
        callbacks.keys.filter { it.sessionToken !in tokens }.forEach { c ->
            callbacks.remove(c)?.let { c.unregisterCallback(it) }
        }
        val known = callbacks.keys.map { it.sessionToken }.toSet()
        controllers.filter { it.sessionToken !in known }.forEach { c ->
            val cb = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) = emit()
                override fun onPlaybackStateChanged(state: PlaybackState?) = emit()
                override fun onSessionDestroyed() {
                    callbacks.remove(c)?.let { c.unregisterCallback(it) }
                    emit()
                }
            }
            c.registerCallback(cb, handler)
            callbacks[c] = cb
        }
        emit()
    }

    private fun emit() {
        val controllers = callbacks.keys
        val chosen = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull { it.metadata != null }
        val np = chosen?.let { snapshot(it) }
        if (np == last) return
        last = np
        onChange(np)
    }

    private fun snapshot(c: MediaController): NowPlaying? {
        val md = c.metadata ?: return null
        val title = md.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: return null
        val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
        val art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val ps = c.playbackState
        return NowPlaying(
            packageName = c.packageName,
            title = title,
            artist = artist,
            art = art,
            isPlaying = ps?.state == PlaybackState.STATE_PLAYING,
            durationMs = md.getLong(MediaMetadata.METADATA_KEY_DURATION),
            positionMs = ps?.position ?: 0L,
            positionUpdatedAt = ps?.lastPositionUpdateTime ?: 0L,
            speed = ps?.playbackSpeed ?: 1f,
        )
    }

    companion object { private const val TAG = "CabGlyph.Media" }
}
