package com.example.procrastination_detection.domain.repository

import com.example.procrastination_detection.data.local.AppDatabase
import com.example.procrastination_detection.data.local.entity.SensorEventEntity
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.event.AggregatedPayload
import com.example.procrastination_detection.domain.event.Timestamped
import com.example.procrastination_detection.domain.session.SessionManager

sealed interface OptimizedDataResult {
    data class Raw(val data: List<Timestamped<SensorPayload>>) : OptimizedDataResult
    data class Aggregated(val data: List<Timestamped<AggregatedPayload>>) : OptimizedDataResult
}
interface SensorEventRepository {
    suspend fun saveEvent(payload: SensorPayload, timestamp: Long)
    suspend fun pruneOldEvents(olderThanTimestamp: Long)

    // Fetch data optimized for the requested time range, with optional sensor and session filtering
    suspend fun getOptimizedEventsForRange(start: Long, end: Long, sensorId: String? = null, sessionId: String? = null): OptimizedDataResult
}


// Implementation

class SensorEventRepositoryImpl(
    private val database: AppDatabase,
    private val sessionManagerProvider: () -> SessionManager,
    private val csvReader: com.example.procrastination_detection.domain.pipeline.CsvReader
) : SensorEventRepository {
    private val rawDao = database.sensorEventDao()
    private val sessionDao = database.sessionDao()

    override suspend fun saveEvent(payload: SensorPayload, timestamp: Long) {
        val payloadType = when (payload) {
            is SensorPayload.AppSwitch        -> "APP_SWITCH"
            is SensorPayload.TitleChange      -> "TITLE_CHANGE"
            is SensorPayload.BrowserOCRContext -> "BROWSER_OCR"
            is SensorPayload.MouseMetrics     -> "MOUSE_METRICS"
            is SensorPayload.KeyboardMetrics  -> "KEYBOARD_METRICS"
            is SensorPayload.SystemIntervention -> "SYSTEM_INTERVENTION"
            is SensorPayload.AggressionHeat   -> "AGGRESSION_HEAT"
        }
        val sessionId = sessionManagerProvider().activeSessionFlow.value?.id ?: "unknown_session"
        
        val entity = SensorEventEntity(
            timestamp = timestamp,
            payloadType = payloadType,
            sensorId = payload.sensorId,
            sessionId = sessionId,
            payload = payload
        )
        database.sensorEventDao().insertEvent(entity)
    }

    override suspend fun pruneOldEvents(olderThanTimestamp: Long) {
        database.sensorEventDao().deleteEventsBefore(olderThanTimestamp)
    }

    override suspend fun getOptimizedEventsForRange(start: Long, end: Long, sensorId: String?, sessionId: String?): OptimizedDataResult {
        val duration = end - start
        
        // Find relevant sessions
        val relevantSessions = if (sessionId != null) {
            val session = sessionDao.getSessionById(sessionId)
            if (session != null) listOf(session) else emptyList()
        } else {
            sessionDao.getSessionsOverlapping(start, end)
        }

        val allEvents = mutableListOf<SensorEventEntity>()

        for (session in relevantSessions) {
            if (session.status == "ACTIVE" || session.csvFilePath == null) {
                // Fetch from live database
                val dbEvents = rawDao.getEventsBetween(start, end, sensorId)
                    .filter { it.sessionId == session.id }
                allEvents.addAll(dbEvents)
            } else {
                // Fetch from CSV archive
                val csvFilePath = session.csvFilePath
                if (csvFilePath != null) {
                    val csvEvents = csvReader.readSessionEvents(csvFilePath)
                        .filter { it.timestamp in start..end }
                        .filter { sensorId == null || it.sensorId == sensorId }
                    allEvents.addAll(csvEvents)
                }
            }
        }

        val rawList = allEvents.map { entity ->
            Timestamped(timestamp = entity.timestamp, payload = entity.payload)
        }.sortedBy { it.timestamp }

        return OptimizedDataResult.Raw(rawList)
    }
}