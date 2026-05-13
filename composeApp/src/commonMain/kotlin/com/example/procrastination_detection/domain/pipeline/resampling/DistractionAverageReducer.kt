package com.example.procrastination_detection.domain.pipeline.resampling

import com.example.procrastination_detection.domain.event.ProcessedEvent
import com.example.procrastination_detection.domain.model.Category

class DistractionAverageReducer : WindowedReducer<ProcessedEvent> {
    override fun reduce(bucketTimestamp: Long, items: List<ProcessedEvent>): Float {
        if (items.isEmpty()) return 0f

        // Count how many events in this minute/hour were categorized as DISTRACTING
        val distractingCount = items.count { it.category == Category.DISTRACTING }

        // Return a percentage (0.0 to 1.0) representing distraction density
        return (distractingCount.toFloat() / items.size.toFloat()) * 100f
    }
}