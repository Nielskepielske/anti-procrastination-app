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

    private val _activeSensors = MutableStateFlow<List<SensorType>>(emptyList())
    val activeSensorsFlow: StateFlow<List<SensorType>> = _activeSensors.asStateFlow()

    /** Expose the sensors so the UI can build the toggle list. */
    val sensors: List<BehaviorSensor> get() = availableSensors

    private var currentProfile: FocusProfile? = null

    fun applyProfile(profile: FocusProfile) {
        currentProfile = profile
        
        if (_isTracking.value) {
            // 1. First, stop everything to reset the state
            availableSensors.forEach { it.stop() }

            // 2. Look at what the profile actually wants
            val sensorsToStart = availableSensors.filter { sensor ->
                profile.requiredSensorIds.contains(sensor.type.name)
            }

            // 3. Only start the requested ones!
            sensorsToStart.forEach { it.start() }
            _activeSensors.value = sensorsToStart.map { it.type }
        } else {
            _activeSensors.value = emptyList()
        }
    }

    fun startAllActiveSensors(){
        if (_isTracking.value) return
        
        val profile = currentProfile
        val started = if (profile != null) {
            val sensorsToStart = availableSensors.filter { sensor ->
                profile.requiredSensorIds.contains(sensor.type.name)
            }
            sensorsToStart.forEach { sensor ->
                sensor.start()
            }
            sensorsToStart
        } else {
            // Fallback just in case profile isn't loaded
            availableSensors.forEach { sensor ->
                sensor.start()
            }
            availableSensors
        }
        _isTracking.value = true
        _activeSensors.value = started.map { it.type }
    }

    fun stopAll(){
        availableSensors.forEach { sensor ->
            sensor.stop()
        }
        _isTracking.value = false
        _activeSensors.value = emptyList()
    }
}