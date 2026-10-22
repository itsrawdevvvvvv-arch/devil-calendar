package com.devil.calendar.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.EntryPointAccessors

/** Toggles a day from the widget and redraws every instance. */
class WidgetDayAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val date = parameters[dateKey] ?: return
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        entryPoint.dayRepository().toggle(date)
        CalendarWidget().updateAll(context)
    }

    companion object {
        val dateKey = ActionParameters.Key<String>("widget_date")
    }
}
