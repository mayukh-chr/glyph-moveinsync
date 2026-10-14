package com.example.cabglyph

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Watches every notification (the MoveInSync message usually arrives as an SMS,
 * so it shows up under your Messages app) and shows OTP + cab digits on the matrix.
 */
class CabNotificationListener : NotificationListenerService() {
    private lateinit var display: GlyphDisplay
    private var lastKey: String? = null

    override fun onListenerConnected() {
        display = GlyphDisplay(this).also { it.start() }
        instance = this
    }

    override fun onListenerDisconnected() {
        instance = null
        if (::display.isInitialized) display.stop()
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
        display.show(info)
    }

    fun showTest(info: CabInfo) = display.show(info)
    fun clearMatrix() = display.clear()

    companion object {
        @Volatile var instance: CabNotificationListener? = null
    }
}
