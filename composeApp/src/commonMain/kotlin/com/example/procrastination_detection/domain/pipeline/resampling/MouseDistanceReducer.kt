package com.example.procrastination_detection.domain.pipeline.resampling

import com.example.procrastination_detection.domain.event.SensorPayload

class MouseDistanceReducer : WindowedReducer<SensorPayload> {
    override fun reduce(bucketTimestamp: Long, items: List<SensorPayload>): Float {
        val totalDistance = items.filterIsInstance<SensorPayload.MouseMetrics>()
            .sumOf { it.distanceTraveled }
        
        return totalDistance.toFloat()
    }
}
