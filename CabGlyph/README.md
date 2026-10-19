# Cab Glyph (Nothing Phone 3)

Reads the MoveInSync cab SMS/notification and shows on the Glyph Matrix:

```
  5 5 1 8     <- OTP (bright)
  -------
  9 0 9 8     <- last 4 digits of CabNo (dimmer)
```

## Setup
1. Open this folder in Android Studio.
2. Download `glyph-matrix-sdk-2.0.aar` from
   https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit
   and put it in `app/libs/`.
3. Build & install on the Phone (3) (system version 20250801 or later).
4. Open the app → **Grant notification access** → enable *Cab Glyph*.
5. Tap **Test with sample message** — you should see 5518 / 9098 on the back.

## Behaviour
- Matches any notification containing "MoveInSync" with `OTP ####` and `CabNo:...`
  (works whether it comes as an SMS or an app notification).
- Stays on the matrix for 45 min (change `cabDisplayMillis` in `MatrixController.kt`), or until you tap *Clear cab code*.
- Pressing the Glyph Button (toy carousel) takes priority over app content — that's an SDK rule.

## Music (optional, off by default)
Turn these on in the app under **MUSIC**. They reuse the notification access you already granted,
which is also what lets the app see media sessions (Spotify, YT Music, …).

- **Now playing on Glyph Matrix** — album art as a greyscale disc with a progress ring
  around the edge that fills as the song plays. Cab codes always win: when a MoveInSync
  message arrives it replaces the music for 45 min (or until *Clear cab code*), then music returns.
- **Generate lock screen wallpaper per song** — builds a wallpaper from the album art
  (gradient + flowing ribbons + a 25×25 dot-matrix halftone of the cover) and sets it on the
  lock screen only. It waits until a song has played for 5 s, so skipping tracks doesn't
  re-theme Nothing OS (the lock clock and accent colours follow the wallpaper).
  It overwrites your current lock wallpaper — save it first if you want it back.
- **Make wallpaper for current song** — applies it immediately, handy for testing.

## If imports don't resolve
The SDK classes live in `com.nothing.ketchum`. If Android Studio can't find them,
open the AAR in the Project view and adjust the imports in `GlyphDisplay.kt`.
