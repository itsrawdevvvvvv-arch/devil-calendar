package com.devil.calendar.data.repository

import com.devil.calendar.data.local.DayLogDao
import com.devil.calendar.data.local.DayLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DayRepository @Inject constructor(
    private val dao: DayLogDao,
) {
    /** Set of crossed-out dates (`yyyy-MM-dd`). */
    val crossedDays: Flow<Set<String>> = dao.observeAll().map { it.toSet() }

    /** Toggles a date; returns the new crossed state. */
    suspend fun toggle(date: String): Boolean {
        return if (dao.contains(date)) {
            dao.delete(date)
            false
        } else {
            dao.insert(DayLogEntity(date))
            true
        }
    }

    suspend fun crossedOnce(): Set<String> = dao.allOnce().toSet()
}
