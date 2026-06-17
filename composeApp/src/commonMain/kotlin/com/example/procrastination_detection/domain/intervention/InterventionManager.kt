package com.example.procrastination_detection.domain.intervention

import com.example.procrastination_detection.domain.pipeline.EventPipeline
import com.example.procrastination_detection.domain.event.SensorPayload

class InterventionManager(
    private val availableStrategies: List<InterventionStrategy>,
    private val pipeline: EventPipeline
) {
    suspend fun trigger(profile: com.example.procrastination_detection.domain.model.FocusProfile) {
        // 1. Get the list of strategy IDs for the current escalation level
        val strategyIds = profile.strategyMap[profile.escalationLevel] ?: return

        // 2. Fire each one in sequence
        strategyIds.forEach { id ->
            val strategy = availableStrategies.find { it.id == id }
            strategy?.executeIntervention()
            
            if (strategy != null) {
                // Log the intervention directly into the pipeline!
                pipeline.emitRawEvent(
                    SensorPayload.SystemIntervention(
                        strategyId = id,
                        aggressionLevel = profile.escalationLevel
                    )
                )
            }
        }
    }

    fun resetAll(){
        availableStrategies.forEach { it.reset() }
    }
}