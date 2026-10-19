package com.example.cabglyph

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val sample = "OTP 5518, Hi , Your ETP:07:24 for Shift:08:00, CabNo:KA-03-AP-9098, " +
        "Vendor:ACT, CabType:SUV. Helpline: 8147629938 -MoveInSync"

    private lateinit var prefs: Prefs
    private var status: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        val status = TextView(this).apply { textSize = 16f; setPadding(0, 0, 0, 32) }

        fun button(label: String, onClick: () -> Unit) = Button(this).apply {
            text = label; setOnClickListener { onClick() }
        }

        fun toggle(label: String, initial: Boolean, onChange: (Boolean) -> Unit) = Switch(this).apply {
            text = label
            isChecked = initial
            setPadding(0, 24, 0, 24)
            setOnCheckedChangeListener { _, on ->
                onChange(on)
                CabNotificationListener.instance?.onSettingsChanged()
            }
        }

        fun header(label: String) = TextView(this).apply {
            text = label; textSize = 13f; alpha = 0.6f; setPadding(0, 48, 0, 8)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            addView(status)
            addView(button("1. Grant notification access") {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            })
            addView(button("2. Test with sample message") {
                val l = CabNotificationListener.instance
                val info = CabMessageParser.parse(sample)
                if (l == null || info == null) toast("Grant notification access first")
                else l.showTest(info)
            })
            addView(button("Clear cab code") { CabNotificationListener.instance?.clearMatrix() })

            addView(header("MUSIC"))
            addView(toggle("Now playing on Glyph Matrix", prefs.musicGlyph) { prefs.musicGlyph = it })
            addView(toggle("Generate lock screen wallpaper per song", prefs.lockWallpaper) {
                prefs.lockWallpaper = it
            })
            addView(button("Make wallpaper for current song") {
                val l = CabNotificationListener.instance
                when {
                    l == null -> toast("Grant notification access first")
                    !prefs.musicGlyph && !prefs.lockWallpaper -> toast("Turn on a music option first")
                    !l.applyWallpaperNow() -> toast("Nothing is playing")
                    else -> toast("Setting lock screen wallpaper…")
                }
            })
            addView(TextView(this@MainActivity).apply {
                textSize = 12f; alpha = 0.6f; setPadding(0, 16, 0, 0)
                text = "Cab codes always take priority over music on the matrix. " +
                    "The wallpaper changes once a song has played for 5 s, and replaces your " +
                    "current lock screen wallpaper — save it first if you want it back."
            })
        }
        setContentView(layout)
        this.status = status
    }

    override fun onResume() {
        super.onResume()
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(ComponentName(this, CabNotificationListener::class.java).flattenToString()) == true
        status?.text = if (enabled) "Listening for MoveInSync messages ✓"
                       else "Notification access is OFF — tap button 1"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
