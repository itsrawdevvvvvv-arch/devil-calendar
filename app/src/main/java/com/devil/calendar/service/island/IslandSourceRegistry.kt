package com.devil.calendar.service.island

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Merges the three places sources come from: this app (rest timer), other
 * apps' notifications, and the media session.
 *
 * Only metadata needed to draw the pill is kept, in memory, and only while the
 * source is live. Nothing is written to disk and nothing leaves the device.
 */
@Singleton
class IslandSourceRegistry @Inject constructor() {

    private val own = MutableStateFlow<List<IslandSource>>(emptyList())
    private val notifications = MutableStateFlow<List<IslandSource>>(emptyList())
    private val media = MutableStateFlow<List<IslandSource>>(emptyList())

    val merged: Flow<List<IslandSource>> =
        combine(own, notifications, media) { a, b, c -> a + b + c }

    fun setOwn(sources: List<IslandSource>) {
        own.value = sources
    }

    fun setNotifications(sources: List<IslandSource>) {
        notifications.value = sources
    }

    fun setMedia(sources: List<IslandSource>) {
        media.value = sources
    }

    fun clearAll() {
        own.value = emptyList()
        notifications.value = emptyList()
        media.value = emptyList()
    }
}
