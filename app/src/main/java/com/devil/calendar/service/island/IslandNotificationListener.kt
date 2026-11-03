package com.devil.calendar.service.island

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.devil.calendar.data.prefs.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reads other apps' notifications for the island.
 *
 * **What Android actually gives us:** a notification's title, text, progress,
 * chronometer, category, ongoing flag and actions. We never see an app's
 * internal state, and apps that post nothing (or hide content on the lock
 * screen) simply cannot appear. That is a platform limit, not a shortcut.
 *
 * **Privacy:** notification content is held in memory only, is never logged,
 * and is never written to disk or sent anywhere. It is cleared the moment the
 * listener disconnects.
 */
@AndroidEntryPoint
class IslandNotificationListener : NotificationListenerService() {

    @Inject lateinit var registry: IslandSourceRegistry
    @Inject lateinit var prefs: UserPreferences
    @Inject lateinit var media: IslandMediaWatcher
    @Inject lateinit var tracker: IslandPackageTracker

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var allowed: Set<String> = emptySet()

    private val listenerComponent: ComponentName
        get() = ComponentName(this, IslandNotificationListener::class.java)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            prefs.islandAllowedPackages.collectLatest { packages ->
                allowed = packages
                rebuild()
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = rebuild()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = rebuild()

    override fun onListenerConnected() {
        super.onListenerConnected()
        rebuild()
    }

    override fun onListenerDisconnected() {
        registry.setNotifications(emptyList())
        registry.setMedia(emptyList())
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        registry.setNotifications(emptyList())
        registry.setMedia(emptyList())
        scope.cancel()
        super.onDestroy()
    }

    private fun rebuild() {
        val sources = runCatching {
            activeNotifications.orEmpty().mapNotNull { it.toSource() }
        }.getOrDefault(emptyList())
        registry.setNotifications(sources)
        tracker.record(
            runCatching {
                activeNotifications.orEmpty()
                    .map { it.packageName }
                    .filter { it != applicationContext.packageName }
                    .toSet()
            }.getOrDefault(emptySet()),
        )
        // Media sessions are only readable with notification access, so they
        // are refreshed alongside the notifications.
        media.refresh(listenerComponent)
    }

    private fun StatusBarNotification.toSource(): IslandSource? {
        if (packageName == applicationContext.packageName) return null
        if (allowed.isNotEmpty() && packageName !in allowed) return null

        val n = notification ?: return null
        val extras = n.extras ?: return null

        val ongoing = n.flags and Notification.FLAG_ONGOING_EVENT != 0
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val showChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
        val category = n.category

        val interesting = ongoing || progressMax > 0 || showChronometer ||
            category == Notification.CATEGORY_CALL ||
            category == Notification.CATEGORY_ALARM ||
            category == Notification.CATEGORY_STOPWATCH ||
            category == Notification.CATEGORY_NAVIGATION
        if (!interesting) return null

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val progress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val countDown = extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false)

        val kind = when (category) {
            Notification.CATEGORY_CALL -> IslandKind.CALL
            Notification.CATEGORY_ALARM, Notification.CATEGORY_STOPWATCH -> IslandKind.TIMER
            else -> IslandKind.OTHER
        }

        return IslandSource(
            id = "n:$key",
            kind = kind,
            title = title.ifBlank { packageName },
            subtitle = text,
            progress = if (progressMax > 0) progress.toFloat() / progressMax else null,
            endMillis = if (showChronometer && countDown) n.`when` else null,
            packageName = packageName,
        )
    }
}
