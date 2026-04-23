package com.example.procrastination_detection.domain.pipeline.stream

import com.example.procrastination_detection.domain.event.SensorPayload
import kotlin.collections.removeFirst
import kotlin.reflect.KClass

class StreamProcessingEngine(
    analyzers: Set<SlidingWindowAnalyzer<out SensorPayload>>
) {
    // Fast routing map
    private val analyzerMap = analyzers.associateBy { it.payloadClass }

    // In-memory buffers mapped by payload type
    private val buffers = mutableMapOf<KClass<out SensorPayload>, MutableList<SensorPayload>>()

    @Suppress("UNCHECKED_CAST")
    fun processLiveEvent(payload: SensorPayload): StreamInsight? {
        val payloadClass = payload::class

        // 1. Find the analyzer. If we don't have one for this sensor, ignore it.
        val analyzer = analyzerMap[payloadClass] as? SlidingWindowAnalyzer<SensorPayload> ?: return null

        // 2. Get or create the memory buffer for this specific sensor type
        val buffer = buffers.getOrPut(payloadClass) { mutableListOf() }

        // 3. Add the new event and trim the buffer to the required window size
        buffer.add(payload)
        if (buffer.size > analyzer.windowSize) {
            buffer.removeFirst() // Keep the sliding window moving forward
        }

        // 4. Only analyze if the buffer is full
        if (buffer.size == analyzer.windowSize) {
            return analyzer.analyzeWindow(buffer)
        }

        return null
    }
}