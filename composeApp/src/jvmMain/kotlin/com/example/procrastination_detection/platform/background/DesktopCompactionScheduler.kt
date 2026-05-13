package com.example.procrastination_detection.platform.background

import com.example.procrastination_detection.data.local.dao.CompactionDao
import com.example.procrastination_detection.domain.pipeline.compaction.CompactionEngine
import com.example.procrastination_detection.domain.pipeline.compaction.CompactionScheduler
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.hours

class DesktopCompactionScheduler(
    private val engine: CompactionEngine,
    private val compactionDao: CompactionDao,
    // Inject a dedicated scope so it survives UI teardowns (e.g., closing the main window but keeping the app in the tray)
    private val applicationScope: CoroutineScope
) : CompactionScheduler {

    private var compactionJob: Job? = null

    override fun schedulePeriodicCompaction() {
        // Prevent launching multiple loops if called twice
        if (compactionJob?.isActive == true) return

        compactionJob = applicationScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    runCompaction()
                } catch (e: Exception) {
                    println("Compaction failed: ${e.message}")
                    // On desktop, we just catch the error and let the loop try again in an hour
                }

                // Sleep for 1 hour without blocking the thread
                delay(1.hours)
            }
        }
    }

    private suspend fun runCompaction() {
        val twentyFourHoursAgo = System.currentTimeMillis() - 86_400_000

        // 1. Fetch
        val rawEvents = compactionDao.getRawEventsOlderThan(twentyFourHoursAgo)
        if (rawEvents.isEmpty()) return

        // 2. Compact using the commonMain engine
        val hourlyEvents = engine.compactToHourly(rawEvents)

        // 3. Save & Delete
        compactionDao.compactRawToHourly(twentyFourHoursAgo, hourlyEvents)

        println("Successfully compacted ${rawEvents.size} raw events into ${hourlyEvents.size} hourly buckets.")
    }

    fun stopScheduler() {
        compactionJob?.cancel()
    }
}