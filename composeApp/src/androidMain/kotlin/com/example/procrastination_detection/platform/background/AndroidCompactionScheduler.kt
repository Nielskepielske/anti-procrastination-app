package com.example.procrastination_detection.platform.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.procrastination_detection.data.local.dao.CompactionDao
import com.example.procrastination_detection.domain.pipeline.compaction.CompactionEngine
import com.example.procrastination_detection.domain.pipeline.compaction.CompactionScheduler
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

class AndroidCompactionScheduler(
    private val context: Context
) : CompactionScheduler {

    override fun schedulePeriodicCompaction() {
        val request = PeriodicWorkRequestBuilder<CompactionWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sensor_data_compaction",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

// The actual Worker class (also in androidMain)
class CompactionWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val engine: CompactionEngine,
    private val compactionDao: CompactionDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val twentyFourHoursAgo = Clock.System.now().toEpochMilliseconds() - 86_400_000

            // 1. Fetch
            val rawEvents = compactionDao.getRawEventsOlderThan(twentyFourHoursAgo)
            if (rawEvents.isEmpty()) return Result.success()

            // 2. Compact using the commonMain engine
            val hourlyEvents = engine.compactToHourly(rawEvents)

            // 3. Save & Delete
            compactionDao.compactRawToHourly(twentyFourHoursAgo, hourlyEvents)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}