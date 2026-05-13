package com.example.procrastination_detection.domain.pipeline.resampling

import com.example.procrastination_detection.domain.event.ProcessedEvent
import com.example.procrastination_detection.domain.event.SensorPayload

class IntensityCountReducer : WindowedReducer<SensorPayload> {
    override fun reduce(
        bucketTimestamp: Long,
        items: List<SensorPayload>
    ): Float {
        return items.size.toFloat()
    }
}