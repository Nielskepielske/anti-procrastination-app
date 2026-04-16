package com.example.procrastination_detection.domain.intervention

class InterventionManager(
    private val availableStrategies: List<InterventionStrategy>
) {
    suspend fun trigger(profile: com.example.procrastination_detection.domain.model.FocusProfile) {
        // 1. Get the list of strategy IDs for the current escalation level
        val strategyIds = profile.strategyMap[profile.escalationLevel] ?: return

        // 2. Fire each one in sequence
        strategyIds.forEach { id ->
            val strategy = availableStrategies.find { it.id == id }
            strategy?.executeIntervention()
        }
    }

    fun resetAll(){
        availableStrategies.forEach { it.reset() }
    }
}