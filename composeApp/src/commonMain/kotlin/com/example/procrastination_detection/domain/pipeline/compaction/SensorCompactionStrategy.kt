package com.example.procrastination_detection.domain.pipeline.compaction

import com.example.procrastination_detection.domain.event.AggregatedPayload
import com.example.procrastination_detection.domain.event.SensorPayload
import kotlin.reflect.KClass

interface SensorCompactionStrategy<T : SensorPayload, A : AggregatedPayload> {
    /** Used by the engine to quickly route the correct payloads here */
    val payloadClass: KClass<T>

    /**
     * Takes a list of granular payloads (all guaranteed to be from the same hour)
     * and returns a single, summarized payload. Returns null if the list is empty.
     */
    fun compact(events: List<T>): A?
}