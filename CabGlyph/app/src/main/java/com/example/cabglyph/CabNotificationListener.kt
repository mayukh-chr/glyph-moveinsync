package com.example.cabglyph

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Watches every notification (the MoveInSync message usually arrives as an SMS,
 * so it shows up under your Messages app) and shows OTP + cab digits on the matrix.
 *
 * Being a notification listener also gives us access to media sessions, which drives
 * the optional now-playing matrix and the generated lock-screen wallpaper.
 */
class CabNotificationListener : NotificationListenerService() {
    private lateinit var prefs: Prefs
    private lateinit var matrix: MatrixController
    private lateinit var wallpaper: LockWallpaperUpdater
    private var media: MediaWatcher? = null
    private var lastKey: String? = null

    override fun onListenerConnected() {
        prefs = Prefs(this)
        matrix = MatrixController(this, prefs).also { it.start() }
        wallpaper = LockWallpaperUpdater(this)
        instance = this
        updateMediaWatcher()
    }

    override fun onListenerDisconnected() {
        instance = null
        media?.stop(); media = null
        if (::wallpaper.isInitialized) wallpaper.stop()
        if (::matrix.isInitialized) matrix.stop()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val text = buildString {
            append(extras.getCharSequence(Notification.EXTRA_TITLE) ?: "").append(' ')
            append(extras.getCharSequence(Notification.EXTRA_TEXT) ?: "").append(' ')
            append(extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: "").append(' ')
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { append(it).append(' ') }
        }
        val info = CabMessageParser.parse(text) ?: return
        val key = info.otp + info.cabLast4
        if (key == lastKey) return // same message re-posted
        lastKey = key
        matrix.showCab(info)
    }

    private fun onMusic(np: NowPlaying?) {
        matrix.onMusic(np)
        if (prefs.lockWallpaper) wallpaper.onMusic(np)
    }

    /** Only watch media sessions while at least one music feature is switched on. */
    private fun updateMediaWatcher() {
        val wanted = prefs.musicGlyph || prefs.lockWallpaper
        if (wanted && media == null) {
            media = MediaWatcher(this, ComponentName(this, CabNotificationListener::class.java)) { onMusic(it) }
                .also { it.start() }
        } else if (!wanted && media != null) {
            media?.stop(); media = null
            matrix.onMusic(null)
        }
        if (!prefs.lockWallpaper) wallpaper.stop()
        matrix.refresh()
    }

    // --- called from MainActivity ---
    fun onSettingsChanged() = updateMediaWatcher()
    fun showTest(info: CabInfo) = matrix.showCab(info)
    fun clearMatrix() = matrix.clearCab()

    /** Returns false when nothing is playing. */
    fun applyWallpaperNow(): Boolean {
        val np = media?.current() ?: return false
        wallpaper.applyNow(np)
        return true
    }

    companion object {
        @Volatile var instance: CabNotificationListener? = null
    }
}
