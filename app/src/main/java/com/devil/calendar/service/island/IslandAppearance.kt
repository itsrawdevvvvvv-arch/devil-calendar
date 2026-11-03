package com.devil.calendar.service.island

/** User-tunable look of the pill. */
data class IslandAppearance(
    val scale: Float = 1f,
    val cornerDp: Int = 28,
    val topOffsetDp: Int = 6,
    val haptics: Boolean = true,
    val tiltGlare: Boolean = true,
)
