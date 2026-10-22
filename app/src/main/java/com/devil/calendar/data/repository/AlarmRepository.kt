package com.devil.calendar.data.repository

import com.devil.calendar.data.local.AlarmDao
import com.devil.calendar.data.local.AlarmEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val dao: AlarmDao,
) {
    val alarms: Flow<List<AlarmEntity>> = dao.observeAll()

    suspend fun enabled(): List<AlarmEntity> = dao.enabledOnce()
    suspend fun save(alarm: AlarmEntity): Long = dao.insert(alarm)
    suspend fun update(alarm: AlarmEntity) = dao.update(alarm)
    suspend fun delete(alarm: AlarmEntity) = dao.delete(alarm)
    suspend fun firstEnabled(): AlarmEntity? = dao.firstEnabled()
}
