package com.devil.calendar.data.repository

import com.devil.calendar.data.local.MoodDao
import com.devil.calendar.data.local.MoodEntity
import com.devil.calendar.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoodRepository @Inject constructor(
    private val dao: MoodDao,
) {
    /** Most recent first, one score per day. */
    fun recent(): Flow<List<Int>> = dao.observeRecent().map { list -> list.map { it.score } }

    fun today(): Flow<Int?> =
        dao.observeRecent().map { list -> list.firstOrNull { it.dayKey == DateUtils.todayKey() }?.score }

    suspend fun save(score: Int) {
        dao.save(
            MoodEntity(
                dayKey = DateUtils.todayKey(),
                score = score.coerceIn(1, 5),
                recordedAt = System.currentTimeMillis(),
            ),
        )
    }
}
