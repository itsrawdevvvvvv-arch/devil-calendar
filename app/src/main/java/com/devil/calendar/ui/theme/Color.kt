package com.devil.calendar.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The single source of truth for colour. Values are copied 1:1 from the
 * prototype's CSS custom properties.
 */
object DevilColors {
    val Ink = Color(0xFF141414)
    val Card = Color(0xFF242424)
    val Red = Color(0xFFF2552C)
    val RedText = Color(0xFFFF6A45)
    val Yellow = Color(0xFFF2D14B)
    val Grey = Color(0xFFE4E4E4)
    val White = Color(0xFFFFFFFF)
    val TakeoverRed = Color(0xFFE8321A)
    val Muted = Color(0xFF9A9A9A)

    /** Slash / strike colour used by the calendar and widget. */
    val Slash = Color(0xFFE03A12)

    // Supporting tones used only where the prototype needs a slightly
    // different shade (nav buttons, disabled day numbers, ring track).
    val NavIdle = Color(0xFF444444)
    val NavIconIdle = Color(0xFFDDDDDD)
    val DayDisabled = Color(0xFF999999)
    val WeekdayLabel = Color(0xFF777777)
    val RingTrack = Color(0xFF333333)
}
