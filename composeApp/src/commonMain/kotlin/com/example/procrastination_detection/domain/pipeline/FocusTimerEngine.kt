package com.example.procrastination_detection.domain.pipeline

import com.example.procrastination_detection.domain.intervention.InterventionManager
import com.example.procrastination_detection.domain.model.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import com.example.procrastination_detection.domain.model.FocusProfile
import com.example.procrastination_detection.domain.model.EscalationLevel

class FocusTimerEngine(
    private val pipeline: EventPipeline,
    private val interventionManager: InterventionManager,
    private val sensorManager: com.example.procrastination_detection.domain.sensor.SensorManager,
    private val scope: CoroutineScope,
    private val focusProfileRepository: com.example.procrastination_detection.domain.repository.FocusProfileRepository
) {
    private var activeJob: Job? = null
    private var aggressionScore = 0
    private var currentState: Category = Category.NEUTRAL

    fun startListening(){
        scope.launch {
            // 1. Listen to the stream of categorized events
            pipeline.processedEvents.collect { event ->
                // Only process events if tracking is active
                if (!sensorManager.isTrackingFlow.value) return@collect

                println("Processing event: $event")
                
                if (event.category == Category.DISTRACTING && currentState != Category.DISTRACTING) {
                    currentState = Category.DISTRACTING
                    handleDistraction()
                } else if (event.category == Category.PRODUCTIVE && currentState != Category.PRODUCTIVE) {
                    currentState = Category.PRODUCTIVE
                    handleProductive()
                }
            }
        }

        // 2. Listen to the global tracking state
        scope.launch {
            sensorManager.isTrackingFlow.collect { isTracking ->
                if (!isTracking) {
                    println("Tracking stopped globally. Resetting FocusTimerEngine.")
                    activeJob?.cancel()
                    interventionManager.resetAll()
                    if (aggressionScore > 0) {
                        aggressionScore = 0
                        pipeline.emitRawEvent(com.example.procrastination_detection.domain.event.SensorPayload.AggressionHeat(0))
                    }
                    currentState = Category.NEUTRAL
                }
            }
        }
    }

    private fun getDelayForScore(score: Int, baseThresholdMs: Long): Long {
        return when {
            score < 3 -> baseThresholdMs.coerceAtLeast(10_000L)
            score < 6 -> (baseThresholdMs / 2).coerceAtLeast(5_000L) // Minimum 5 seconds
            else -> (baseThresholdMs / 4).coerceAtLeast(2_000L) // Minimum 2 seconds
        }
    }

    private fun getLevelForScore(score: Int): EscalationLevel {
        return when {
            score < 3 -> EscalationLevel.GENTLE
            score < 6 -> EscalationLevel.FIRM
            else -> EscalationLevel.AGGRESSIVE
        }
    }

    private fun handleDistraction() {
        activeJob?.cancel()

        activeJob = scope.launch {
            val profile = focusProfileRepository.activeProfileFlow.first() 
                ?: return@launch // Fallback if no profile is active

            val baseDelay = profile.thresholdMinutes * 60_000L

            println("Distraction active. Starting heat loop at score $aggressionScore...")
            
            while (isActive) {
                val currentDelay = getDelayForScore(aggressionScore, baseDelay)
                
                // Wait before escalating
                delay(currentDelay)
                if (!isActive) break

                aggressionScore++
                pipeline.emitRawEvent(com.example.procrastination_detection.domain.event.SensorPayload.AggressionHeat(aggressionScore))
                
                val currentEscalation = getLevelForScore(aggressionScore)
                
                println("Distraction continues! Heat: $aggressionScore, Triggering $currentEscalation intervention.")
                interventionManager.trigger(profile.copy(escalationLevel = currentEscalation))
            }
        }
    }

    private fun handleProductive(){
        activeJob?.cancel()
        interventionManager.resetAll()

        if (aggressionScore == 0) {
            println("User returned to productive task. Heat is already 0.")
            return
        }

        activeJob = scope.launch {
            val profile = focusProfileRepository.activeProfileFlow.first() 
                ?: return@launch 
            val baseDelay = profile.thresholdMinutes * 60_000L

            println("Productive active. Starting cooldown loop. Current heat: $aggressionScore")

            while (isActive && aggressionScore > 0) {
                // Cool down at the same rate it would rise at this score
                val currentDelay = getDelayForScore(aggressionScore, baseDelay)
                
                delay(currentDelay)
                if (!isActive) break

                aggressionScore--
                pipeline.emitRawEvent(com.example.procrastination_detection.domain.event.SensorPayload.AggressionHeat(aggressionScore))
                println("Cooling down... Heat dropped to $aggressionScore")
            }
            if (aggressionScore == 0) {
                println("Cooldown complete. Heat is 0.")
            }
        }
    }
}