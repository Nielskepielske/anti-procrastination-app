package com.example.procrastination_detection.domain.pipeline.resampling

import com.example.procrastination_detection.domain.event.SensorPayload

class SwitchCountReducer : WindowedReducer<SensorPayload.AppSwitch> {
    override fun reduce(bucketTimestamp: Long, items: List<SensorPayload.AppSwitch>): Float {
        // Simply return how many switches happened in this window
        return items.size.toFloat()
    }
}