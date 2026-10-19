package com.example.cabglyph

import android.content.Context

/** Feature toggles. Both music features are opt-in so the cab behaviour is unchanged by default. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("cabglyph", Context.MODE_PRIVATE)

    var musicGlyph: Boolean
        get() = sp.getBoolean(KEY_MUSIC_GLYPH, false)
        set(v) = sp.edit().putBoolean(KEY_MUSIC_GLYPH, v).apply()

    var lockWallpaper: Boolean
        get() = sp.getBoolean(KEY_LOCK_WALLPAPER, false)
        set(v) = sp.edit().putBoolean(KEY_LOCK_WALLPAPER, v).apply()

    private companion object {
        const val KEY_MUSIC_GLYPH = "music_glyph"
        const val KEY_LOCK_WALLPAPER = "lock_wallpaper"
    }
}
