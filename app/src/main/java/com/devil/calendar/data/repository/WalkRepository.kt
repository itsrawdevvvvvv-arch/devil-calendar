package com.devil.calendar.data.repository

import com.devil.calendar.data.local.walk.StepDayEntity
import com.devil.calendar.data.local.walk.WalkDao
import com.devil.calendar.data.local.walk.WalkEntity
import com.devil.calendar.domain.walk.DeadReckoning
import com.devil.calendar.domain.walk.StepBaseline
import com.devil.calendar.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalkRepository @Inject constructor(
    private val dao: WalkDao,
) {
    fun walks(): Flow<List<WalkEntity>> = dao.observeWalks()

    fun weekSteps(): Flow<List<Int>> = dao.observeWeek().map { list -> list.map { it.steps } }

    suspend fun stepsToday(rawSensor: Long): Int {
        val today = DateUtils.todayKey()
        val stored = dao.day(today)
        val result = StepBaseline.stepsToday(
            rawSensor = rawSensor,
            storedBaseline = stored?.sensorBaseline ?: rawSensor,
            storedSteps = stored?.steps ?: 0,
            isNewDay = stored == null,
        )
        if (result.needsPersist || stored == null) {
            dao.saveDay(
                StepDayEntity(
                    dayKey = today,
                    steps = result.stepsToday,
                    sensorBaseline = result.baseline,
                ),
            )
        }
        return result.stepsToday
    }

    suspend fun saveWalk(
        startedAt: Long,
        endedAt: Long,
        steps: Int,
        path: List<Pair<Double, Double>>,
        style: Int,
    ): Long = dao.insertWalk(
        WalkEntity(
            startedAt = startedAt,
            endedAt = endedAt,
            steps = steps,
            distanceMetres = steps * 0.7,
            polyline = DeadReckoning.encode(path),
            style = style,
        ),
    )
}
