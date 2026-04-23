package com.example.procrastination_detection.domain.pipeline.compaction.strategies

import com.example.procrastination_detection.domain.event.AggregatedPayload
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.pipeline.compaction.SensorCompactionStrategy

class AppSwitchCompactionStrategy : SensorCompactionStrategy<SensorPayload.AppSwitch, AggregatedPayload.Frequency> {
    override val payloadClass = SensorPayload.AppSwitch::class

    override fun compact(events: List<SensorPayload.AppSwitch>): AggregatedPayload.Frequency? {
        if (events.isEmpty()) return null

        // Count how many unique apps were used in this hour
        val uniqueApps = events.map { it.windowData.processName }.toSet().size

        return AggregatedPayload.Frequency(
            totalOccurrences = events.size,
            uniqueContexts = uniqueApps
        )
    }
}