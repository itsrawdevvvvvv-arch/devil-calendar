package com.devil.calendar.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Small indirection so ViewModels and receivers can redraw the widget without
 * depending on the Glance class directly.
 */
@Singleton
class WidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun refresh() {
        runCatching { CalendarWidget().updateAll(context) }
    }
}
