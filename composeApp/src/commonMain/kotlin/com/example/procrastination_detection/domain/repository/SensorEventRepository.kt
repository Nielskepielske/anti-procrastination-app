package com.example.procrastination_detection.domain.repository

import com.example.procrastination_detection.data.local.AppDatabase
import com.example.procrastination_detection.data.local.SensorEventEntity
import com.example.procrastination_detection.domain.event.SensorPayload

interface SensorEventRepository {
    suspend fun saveEvent(payload: SensorPayload, timestamp: Long)
    suspend fun pruneOldEvents(olderThanTimestamp: Long)
}


// Implementation

class SensorEventRepositoryImpl(
    private val database: AppDatabase
) : SensorEventRepository {
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
}