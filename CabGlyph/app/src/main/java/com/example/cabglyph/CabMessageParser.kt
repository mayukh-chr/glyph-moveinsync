package com.example.cabglyph

/**
 * Parses MoveInSync cab messages like:
 * "OTP 5518, Hi , Your ETP:07:24 for Shift:08:00, CabNo:KA-03-AP-9098, Vendor:ACT, ..."
 */
data class CabInfo(val otp: String, val cabLast4: String, val shift: String?)

object CabMessageParser {
    private val OTP = Regex("""OTP\s*[:\-]?\s*(\d{4,6})""", RegexOption.IGNORE_CASE)
    private val CAB = Regex("""CabNo\s*:\s*([A-Z0-9\- ]+?)\s*,""", RegexOption.IGNORE_CASE)
    private val SHIFT = Regex("""Shift\s*:\s*(\d{1,2}:\d{2})""", RegexOption.IGNORE_CASE)

    fun parse(text: String): CabInfo? {
        if (!text.contains("MoveInSync", ignoreCase = true)) return null
        val otp = OTP.find(text)?.groupValues?.get(1) ?: return null
        val cabDigits = CAB.find(text)?.groupValues?.get(1)?.filter { it.isDigit() } ?: return null
        if (cabDigits.length < 4) return null
        return CabInfo(
            otp = otp.takeLast(4),
            cabLast4 = cabDigits.takeLast(4),
            shift = SHIFT.find(text)?.groupValues?.get(1)
        )
    }
}
