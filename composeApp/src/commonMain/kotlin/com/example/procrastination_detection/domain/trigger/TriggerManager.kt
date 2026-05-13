package com.example.procrastination_detection.domain.trigger

class TriggerManager(
    val availableTriggers: List<ActionTrigger>
) {
    fun getTrigger(id: String): ActionTrigger? {
        return availableTriggers.find { it.id == id }
    }
}
