package com.example.procrastination_detection.domain.pipeline.resampling

import com.example.procrastination_detection.domain.event.SensorPayload

class MouseIdleReducer : WindowedReducer<SensorPayload> {
    override fun reduce(bucketTimestamp: Long, items: List<SensorPayload>): Float {
        // Count how many times the mouse was perfectly still (0.0 distance).
        // Since the mouse tracker polls every 1 second, each zero-distance event equals 1 second of idle time.
        val idleEventsCount = items.filterIsInstance<SensorPayload.MouseMetrics>()
            .count { it.distanceTraveled == 0.0 }
        
        return idleEventsCount.toFloat()
    }
}
