package com.example.cabglyph

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val sample = "OTP 5518, Hi , Your ETP:07:24 for Shift:08:00, CabNo:KA-03-AP-9098, " +
        "Vendor:ACT, CabType:SUV. Helpline: 8147629938 -MoveInSync"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = TextView(this).apply { textSize = 16f; setPadding(0, 0, 0, 32) }

        fun button(label: String, onClick: () -> Unit) = Button(this).apply {
            text = label; setOnClickListener { onClick() }
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
            addView(button("Clear matrix") { CabNotificationListener.instance?.clearMatrix() })
        }
        setContentView(layout)
        this.status = status
    }

    private var status: TextView? = null

    override fun onResume() {
        super.onResume()
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(ComponentName(this, CabNotificationListener::class.java).flattenToString()) == true
        status?.text = if (enabled) "Listening for MoveInSync messages ✓"
                       else "Notification access is OFF — tap button 1"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
