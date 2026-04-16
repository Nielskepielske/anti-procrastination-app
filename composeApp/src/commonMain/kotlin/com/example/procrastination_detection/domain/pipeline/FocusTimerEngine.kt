package com.example.procrastination_detection.domain.pipeline

import com.example.procrastination_detection.domain.intervention.InterventionManager
import com.example.procrastination_detection.domain.model.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.procrastination_detection.domain.model.FocusProfile

class FocusTimerEngine(
    private val pipeline: EventPipeline,
    private val interventionManager: InterventionManager,
    private val sensorManager: com.example.procrastination_detection.domain.sensor.SensorManager,
    private val scope: CoroutineScope,
    private val focusProfileRepository: com.example.procrastination_detection.domain.repository.FocusProfileRepository
) {
    private var distractionJob: Job? = null

    fun startListening(){
        scope.launch {
            // 1. Listen to the stream of categorized events
            pipeline.processedEvents.collect { event ->
                // Only process events if tracking is active
                if (!sensorManager.isTrackingFlow.value) return@collect

                println("Processing event: $event")
                when(event.category) {
                    Category.DISTRACTING -> handleDistraction()
                    Category.PRODUCTIVE -> handleProductive()
                    else -> {}
                }
            }
        }

        // 2. Listen to the global tracking state
        scope.launch {
            sensorManager.isTrackingFlow.collect { isTracking ->
                if (!isTracking) {
                    println("Tracking stopped globally. Resetting FocusTimerEngine.")
                    distractionJob?.cancel()
                    interventionManager.resetAll()
                }
            }
        }
    }

    private fun handleDistraction() {
        if (distractionJob?.isActive == true) return

        distractionJob = scope.launch {
            // 1. Fetch the active profile to know the threshold
            val profile = focusProfileRepository.activeProfileFlow.first() 
                ?: return@launch // Fallback if no profile is active

            println("Distraction detected. Timer started for ${profile.thresholdMinutes}m...")

            // 2. Wait for the profile's threshold
            delay(profile.thresholdMinutes * 60_000L)

            // 3. Trigger intervention with the full profile context
            println("Threshold breached! Triggering intervention.")
            interventionManager.trigger(profile)
        }
    }

    private fun handleProductive(){
        if(distractionJob?.isActive == true){
            println("User returned to productive task. Timer canceled.")
            distractionJob?.cancel()
            interventionManager.resetAll()
        }
    }
}