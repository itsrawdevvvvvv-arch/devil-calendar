package com.devil.calendar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devil.calendar.R

/**
 * Product Sans Bold is the ONLY font in the app. Every [TextStyle] here
 * resolves to it and every weight is [FontWeight.Bold] because the bundled
 * file has a single weight. Font synthesis is disabled so the platform can
 * never fake a lighter/heavier cut.
 */
val ProductSans = FontFamily(
    Font(R.font.product_sans_bold, FontWeight.Bold),
)

private val Bold = FontWeight.Bold

private fun ps(size: Int, lineHeight: Int, letterSpacing: Double = 0.0) = TextStyle(
    fontFamily = ProductSans,
    fontWeight = Bold,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

val DevilTypography = Typography(
    displayLarge = ps(70, 74),      // alarm time picker
    displayMedium = ps(62, 66),     // ring countdown
    displaySmall = ps(46, 50),      // stats hero title
    headlineLarge = ps(40, 42),     // home greeting
    headlineMedium = ps(38, 42),    // section numbers
    headlineSmall = ps(26, 30),     // calendar month title
    titleLarge = ps(22, 26),        // goal text
    titleMedium = ps(20, 24),       // stat values
    titleSmall = ps(17, 22),        // tool labels, buttons
    bodyLarge = ps(16, 22),
    bodyMedium = ps(14, 20),
    bodySmall = ps(13, 18),
    labelLarge = ps(16, 20, 2.0),   // pill buttons, letter-spaced
    labelMedium = ps(13, 16, 3.0),  // takeover label
    labelSmall = ps(12, 16),        // weekday row
)
