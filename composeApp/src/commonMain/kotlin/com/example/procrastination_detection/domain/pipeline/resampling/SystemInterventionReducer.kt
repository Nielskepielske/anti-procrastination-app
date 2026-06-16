package com.example.procrastination_detection.domain.pipeline.resampling

import com.example.procrastination_detection.domain.event.SensorPayload

class SystemInterventionReducer : WindowedReducer<SensorPayload.SystemIntervention> {
    override fun reduce(bucketTimestamp: Long, items: List<SensorPayload.SystemIntervention>): Float {
        // Simply return how many interventions happened in this window
        return items.size.toFloat()
    }
}
