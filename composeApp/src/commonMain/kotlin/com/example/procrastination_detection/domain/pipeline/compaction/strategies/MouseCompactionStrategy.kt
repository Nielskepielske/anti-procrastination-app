package com.example.procrastination_detection.domain.pipeline.compaction.strategies

import com.example.procrastination_detection.domain.event.AggregatedPayload
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.pipeline.compaction.SensorCompactionStrategy

class MouseCompactionStrategy : SensorCompactionStrategy<SensorPayload.MouseMetrics, AggregatedPayload.Mouse> {
    override val payloadClass = SensorPayload.MouseMetrics::class

    override fun compact(events: List<SensorPayload.MouseMetrics>): AggregatedPayload.Mouse? {
        if (events.isEmpty()) return null

        return AggregatedPayload.Mouse(
            totalHoverDurationMillis = events.sumOf { it.hoverDurationMillis },
            totalClicks = events.sumOf { it.clicks },
            totalDistanceTraveled = events.sumOf { it.distanceTraveled }
        )
    }
}