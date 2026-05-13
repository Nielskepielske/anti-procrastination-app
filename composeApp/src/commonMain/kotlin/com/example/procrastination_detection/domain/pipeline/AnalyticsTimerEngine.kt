package com.example.procrastination_detection.domain.pipeline

import com.example.procrastination_detection.data.local.dao.AppUsageDao
import com.example.procrastination_detection.data.local.entity.AppUsageEntity
import com.example.procrastination_detection.domain.event.SensorPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AnalyticsTimerEngine(
    private val pipeline: EventPipeline,
    private val appUsageDao: AppUsageDao,
    private val sensorManager: com.example.procrastination_detection.domain.sensor.SensorManager,
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private var lastUpdateTimestamp: Long = 0L
    private var currentProcessName: String = ""
    private var currentWindowTitle: String = ""

    fun startListening() {
        // 1. Listen to raw events
        scope.launch {
            pipeline.processedEvents.collect { event ->
                if (!sensorManager.isTrackingFlow.value) return@collect
                
                val now = event.timestamp
                mutex.withLock {
                    when (val payload = event.payload) {
                        is SensorPayload.AppSwitch -> {
                            pulse(now)
                            currentProcessName = payload.windowData.processName
                            currentWindowTitle = payload.windowData.windowTitle
                        }
                        is SensorPayload.TitleChange -> {
                            pulse(now)
                            currentWindowTitle = payload.windowData.windowTitle
                        }
                        is SensorPayload.BrowserOCRContext -> {
                            pulse(now)
                            currentWindowTitle = payload.url
                        }
                        else -> { /* Metrics payloads don't affect the timer */ }
                    }
                }
            }
        }

        // 2. Heartbeat: Flush segment every 30s
        scope.launch {
            while (isActive) {
                delay(30000)
                if (sensorManager.isTrackingFlow.value) {
                    mutex.withLock {
                        if (currentProcessName.isNotEmpty()) {
                            pulse(System.currentTimeMillis())
                        }
                    }
                }
            }
        }

        // 3. Listen to global tracking state
        scope.launch {
            sensorManager.isTrackingFlow.collect { isTracking ->
                mutex.withLock {
                    if (!isTracking) {
                        println("Tracking stopped. Flushing final analytics segment.")
                        pulse(System.currentTimeMillis())
                        // Crucial: Clear state so we don't count the "off" duration on restart
                        lastUpdateTimestamp = 0L
                        currentProcessName = ""
                        currentWindowTitle = ""
                    }
                }
            }
        }
    }

    private suspend fun pulse(now: Long) {
        if (lastUpdateTimestamp > 0L && currentProcessName.isNotEmpty()) {
            val durationSeconds = (now - lastUpdateTimestamp) / 1000L
            if (durationSeconds >= 1L) {
                logUsage(currentProcessName, currentWindowTitle, durationSeconds)
                lastUpdateTimestamp = now // Update our reference point
            }
        } else {
            lastUpdateTimestamp = now
        }
    }

    private suspend fun logUsage(processName: String, windowTitle: String, durationSeconds: Long) {
        val todayEpochDay = lastUpdateTimestamp / 86400000L

        val updatedRows = appUsageDao.incrementUsage(
            dayIndex = todayEpochDay,
            processName = processName,
            windowTitle = windowTitle,
            seconds = durationSeconds
        )

        // If it didn't exist, insert a new record
        if (updatedRows == 0) {
            appUsageDao.insertUsage(
                AppUsageEntity(
                    dayIndex = todayEpochDay,
                    processName = processName,
                    windowTitle = windowTitle,
                    totalSeconds = durationSeconds
                )
            )
        }
    }
}
