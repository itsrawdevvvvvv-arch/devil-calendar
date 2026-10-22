package com.devil.calendar.widget

import com.devil.calendar.data.repository.DayRepository
import com.devil.calendar.data.repository.ProfileRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Glance instantiates the widget itself, so it cannot use constructor
 * injection. This entry point is how the widget reaches the repositories.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun dayRepository(): DayRepository
    fun profileRepository(): ProfileRepository
}
