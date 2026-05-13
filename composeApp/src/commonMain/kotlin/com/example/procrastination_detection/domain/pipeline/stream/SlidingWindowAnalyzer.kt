package com.example.procrastination_detection.domain.pipeline.stream

import com.example.procrastination_detection.domain.event.SensorPayload
import kotlin.reflect.KClass

/** Represents an insight or anomaly detected in real-time */
data class StreamInsight(
    val title: String,
    val description: String,
    val severity: Int // e.g., 1 to 5 for triggering instant interventions
)

interface SlidingWindowAnalyzer<T : SensorPayload> {
    val payloadClass: KClass<T>

    /** How many consecutive events this analyzer needs to evaluate a pattern */
    val windowSize: Int

    /** * Evaluates the sliding window.
     * Returns an insight if a pattern is detected, or null if everything is normal.
     */
    fun analyzeWindow(window: List<T>): StreamInsight?
}