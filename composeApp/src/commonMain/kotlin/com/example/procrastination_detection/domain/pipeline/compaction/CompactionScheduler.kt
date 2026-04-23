package com.example.procrastination_detection.domain.pipeline.compaction

interface CompactionScheduler {
    /** * Schedules the background job to run periodically (e.g., every hour)
     * to compact raw data into hourly, and hourly into daily.
     */
    fun schedulePeriodicCompaction()
}