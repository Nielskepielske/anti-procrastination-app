package com.example.procrastination_detection.domain.pipeline.compaction

import com.example.procrastination_detection.data.local.entity.HourlySensorEventEntity
import com.example.procrastination_detection.data.local.entity.SensorEventEntity
import com.example.procrastination_detection.domain.event.SensorPayload

class CompactionEngine(
    strategies: Set<SensorCompactionStrategy<out SensorPayload, *>>
) {
    // O(1) lookup map for fast routing
    private val strategyMap = strategies.associateBy { it.payloadClass }

    /**
     * Converts a raw list of database entities into a compact list of hourly entities.
     */
    @Suppress("UNCHECKED_CAST")
    fun compactToHourly(rawEvents: List<SensorEventEntity>): List<HourlySensorEventEntity> {
        val compactedResults = mutableListOf<HourlySensorEventEntity>()

        // 1. Group events by the exact Hour they occurred in
        // (Truncate the timestamp down to the nearest hour boundary)
        val oneHourMillis = 3_600_000L
        val eventsByHour = rawEvents.groupBy { event ->
            event.timestamp - (event.timestamp % oneHourMillis)
        }

        // 2. Process each hour independently
        for ((hourTimestamp, eventsInThisHour) in eventsByHour) {

            // 3. Inside this hour, group by the specific payload type
            val eventsByType = eventsInThisHour.groupBy { it.payload::class }

            for ((payloadClass, typeGroup) in eventsByType) {
                // Find the correct strategy
                val strategy = strategyMap[payloadClass] as? SensorCompactionStrategy<SensorPayload, *>
                    ?: continue // Skip if we don't have a strategy for this sensor yet

                // Extract just the payloads
                val payloads = typeGroup.map { it.payload }

                // Let the strategy do the math
                val aggregatedPayload = strategy.compact(payloads)

                // 4. Wrap it in the new database entity and save
                if (aggregatedPayload != null) {
                    val dbPayloadType = typeGroup.first().payloadType // e.g., "APP_SWITCH"

                    compactedResults.add(
                        HourlySensorEventEntity(
                            hourTimestamp = hourTimestamp,
                            payloadType = dbPayloadType,
                            payload = aggregatedPayload
                        )
                    )
                }
            }
        }

        return compactedResults
    }
}