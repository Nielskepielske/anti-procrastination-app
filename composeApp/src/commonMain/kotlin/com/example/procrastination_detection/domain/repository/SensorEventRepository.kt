package com.example.procrastination_detection.domain.repository

import com.example.procrastination_detection.data.local.AppDatabase
import com.example.procrastination_detection.data.local.entity.SensorEventEntity
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.event.AggregatedPayload
import com.example.procrastination_detection.domain.event.Timestamped

sealed interface OptimizedDataResult {
    data class Raw(val data: List<Timestamped<SensorPayload>>) : OptimizedDataResult
    data class Aggregated(val data: List<Timestamped<AggregatedPayload>>) : OptimizedDataResult
}
interface SensorEventRepository {
    suspend fun saveEvent(payload: SensorPayload, timestamp: Long)
    suspend fun pruneOldEvents(olderThanTimestamp: Long)

    // NEW: Fetch data optimized for the requested time range
    suspend fun getOptimizedEventsForRange(start: Long, end: Long): OptimizedDataResult
}


// Implementation

class SensorEventRepositoryImpl(
    private val database: AppDatabase
) : SensorEventRepository {
    private val rawDao = database.sensorEventDao()
    private val compactionDao = database.compactionDao()

    override suspend fun saveEvent(payload: SensorPayload, timestamp: Long) {
        val payloadType = when (payload) {
            is SensorPayload.AppSwitch        -> "APP_SWITCH"
            is SensorPayload.TitleChange      -> "TITLE_CHANGE"
            is SensorPayload.BrowserOCRContext -> "BROWSER_OCR"
            is SensorPayload.MouseMetrics     -> "MOUSE_METRICS"
            is SensorPayload.KeyboardMetrics  -> "KEYBOARD_METRICS"
        }
        val entity = SensorEventEntity(
            timestamp = timestamp,
            payloadType = payloadType,
            payload = payload
        )
        database.sensorEventDao().insertEvent(entity)
    }

    override suspend fun pruneOldEvents(olderThanTimestamp: Long) {
        database.sensorEventDao().deleteEventsBefore(olderThanTimestamp)
    }

    override suspend fun getOptimizedEventsForRange(start: Long, end: Long): OptimizedDataResult {
        val duration = end - start
        val oneDayMillis = 86_400_000L

        return if (duration <= oneDayMillis) {
            val rawList = rawDao.getEventsBetween(start, end).map { entity ->
                Timestamped(timestamp = entity.timestamp, payload = entity.payload)
            }
            OptimizedDataResult.Raw(rawList)
        } else {
            val aggregatedList = compactionDao.getHourlyEventsBetween(start, end).map { entity ->
                // Map the hourTimestamp to the generic timestamp field
                Timestamped(timestamp = entity.hourTimestamp, payload = entity.payload)
            }
            OptimizedDataResult.Aggregated(aggregatedList)
        }
    }
}