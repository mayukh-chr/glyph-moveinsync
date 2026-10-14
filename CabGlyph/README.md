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
- Stays on the matrix for 45 min (change `displayMillis` in `GlyphDisplay.kt`), or until you tap Clear.
- Pressing the Glyph Button (toy carousel) takes priority over app content — that's an SDK rule.

## If imports don't resolve
The SDK classes live in `com.nothing.ketchum`. If Android Studio can't find them,
open the AAR in the Project view and adjust the imports in `GlyphDisplay.kt`.
