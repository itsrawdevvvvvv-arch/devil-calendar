package com.devil.calendar.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.devil.calendar.MainActivity
import com.devil.calendar.R
import com.devil.calendar.data.prefs.UserProfile
import com.devil.calendar.domain.StatsCalculator
import com.devil.calendar.util.DateUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.YearMonth

/**
 * The home-screen widget. It reads the same Room database as the app and
 * mirrors the calendar styling: a slash on crossed days and an ink-filled
 * cell for today.
 *
 * Note: Glance renders through RemoteViews, so a couple of visual details
 * (the rounded day cell radius, custom font) are limited by the platform —
 * see the README's "Widget limitations" section.
 */
class CalendarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val crossed = entryPoint.dayRepository().crossedOnce()
        val profile = entryPoint.profileRepository().profile.first()

        provideContent {
            WidgetContent(crossed = crossed, profile = profile)
        }
    }

    suspend fun refreshAll(context: Context) {
        runCatching { updateAll(context) }
    }
}

private val Ink = ColorProvider(Color(0xFF141414))
private val White = ColorProvider(Color(0xFFFFFFFF))
private val Yellow = ColorProvider(Color(0xFFF2D14B))
private val Grey = ColorProvider(Color(0xFF9A9A9A))

@Composable
private fun WidgetContent(crossed: Set<String>, profile: UserProfile) {
    val context = LocalContext.current
    val today = DateUtils.today()
    val month = YearMonth.from(today)
    val streak = StatsCalculator.streak(crossed)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Ink)
            .cornerRadius(28.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = DateUtils.monthTitle(month, today),
                style = TextStyle(color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = "$streak",
                style = TextStyle(color = Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp),
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    style = TextStyle(color = Grey, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        val cells = DateUtils.gridDates(month)
        cells.chunked(7).take(6).forEach { week ->
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(modifier = GlanceModifier.defaultWeight().height(30.dp))
                    } else {
                        val key = DateUtils.key(date)
                        val isToday = key == DateUtils.key(today)
                        val isCrossed = key in crossed
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .height(30.dp)
                                .clickable(
                                    actionRunCallback<WidgetDayAction>(
                                        actionParametersOf(WidgetDayAction.dateKey to key),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isToday) {
                                Box(
                                    modifier = GlanceModifier
                                        .size(28.dp)
                                        .background(Ink)
                                        .cornerRadius(9.dp),
                                ) {}
                            }
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = TextStyle(
                                    color = if (isToday) White else if (isCrossed) Grey else White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                            if (isCrossed) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_widget_slash),
                                    contentDescription = null,
                                    modifier = GlanceModifier.size(28.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
