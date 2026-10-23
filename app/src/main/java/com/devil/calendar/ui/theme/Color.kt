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
    /** Second stop of the takeover background pulse. */
    val TakeoverRedPulse = Color(0xFFFF4A2E)
    val Muted = Color(0xFF9A9A9A)

    /** Inactive labels on the dark segmented control. */
    val SegInactiveLabel = Color(0xFFC8C8C8)

    /** Hairline border for cards that sit on the ink background. */
    val CardBorder = Color(0xFF333333)

    /** Outer stop of the takeover vignette. */
    val VignetteEdge = Color(0xFF8F1A0C)

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
