package com.example.procrastination_detection.domain.pipeline.stream

import com.example.procrastination_detection.domain.event.SensorPayload

class TabHoppingAnalyzer : SlidingWindowAnalyzer<SensorPayload.TitleChange> {
    override val payloadClass = SensorPayload.TitleChange::class
    override val windowSize = 5 // Look at the last 5 title changes

    override fun analyzeWindow(window: List<SensorPayload.TitleChange>): StreamInsight? {
        val uniqueTitles = window.map { it.windowData.windowTitle }.toSet()

        // If they jumped across 4 different titles in just 5 events
        if (uniqueTitles.size >= 4) {
            return StreamInsight(
                title = "Rapid Context Switching",
                description = "You are rapidly hopping between different tabs/documents.",
                severity = 3
            )
        }
        return null
    }
}