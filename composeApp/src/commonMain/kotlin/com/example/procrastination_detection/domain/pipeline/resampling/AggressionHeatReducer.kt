package com.example.procrastination_detection.domain.pipeline.resampling

import com.example.procrastination_detection.domain.event.SensorPayload

class AggressionHeatReducer : WindowedReducer<SensorPayload.AggressionHeat> {
    override fun reduce(bucketTimestamp: Long, items: List<SensorPayload.AggressionHeat>): Float {
        if (items.isEmpty()) return 0f

        // We want to plot the maximum heat reached in this time bucket.
        // Alternatively, we could plot the average or the last known value, 
        // but max gives a good representation of the peak distraction in that minute/hour.
        val maxHeat = items.maxOf { it.score }

        return maxHeat.toFloat()
    }
}
