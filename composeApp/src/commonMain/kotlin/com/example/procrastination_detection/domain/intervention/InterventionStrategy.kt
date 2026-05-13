package com.example.procrastination_detection.domain.intervention

interface InterventionStrategy {
    val id : String
    suspend fun executeIntervention()
    fun reset()
}