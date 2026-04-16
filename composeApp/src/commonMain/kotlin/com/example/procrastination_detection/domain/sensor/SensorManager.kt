package com.example.procrastination_detection.domain.sensor

import com.example.procrastination_detection.domain.model.FocusProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SensorManager(
    private val availableSensors: List<BehaviorSensor>
) {
    private val _isTracking = MutableStateFlow(false)
    val isTrackingFlow : StateFlow<Boolean> = _isTracking.asStateFlow()

    /** Expose the sensors so the UI can build the toggle list. */
    val sensors: List<BehaviorSensor> get() = availableSensors

    fun applyProfile(profile: FocusProfile) {
        // 1. First, stop everything to reset the state
        availableSensors.forEach { it.stop() }

        // 2. Look at what the profile actually wants
        // Example: profile.requiredSensorIds = ["WINDOW_TRACKER"]
        // (It left out "MOUSE_TRACKER")

        val sensorsToStart = availableSensors.filter { sensor ->
            profile.requiredSensorIds.contains(sensor.id)
        }

        // 3. Only start the requested ones!
        sensorsToStart.forEach { it.start() }
    }
    // In the future, this will take a FocusProfile to know exactly which ones to start
    fun startAllActiveSensors(){
        availableSensors.forEach { sensor ->
            sensor.start()
        }
        _isTracking.value = true
    }

    fun stopAll(){
        availableSensors.forEach { sensor ->
            sensor.stop()
        }
        _isTracking.value = false
    }
}