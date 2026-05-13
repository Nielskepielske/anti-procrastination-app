package com.example.procrastination_detection.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.procrastination_detection.domain.intervention.InterventionManager
import com.example.procrastination_detection.domain.model.EscalationLevel
import com.example.procrastination_detection.domain.model.FocusProfile
import com.example.procrastination_detection.domain.repository.FocusProfileRepository
import com.example.procrastination_detection.domain.sensor.SensorManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val sensorManager: SensorManager,
    private val interventionManager: InterventionManager,
    private val focusProfileRepository: FocusProfileRepository
) : ViewModel() {

    val allProfiles = focusProfileRepository.allProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile = focusProfileRepository.activeProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val availableSensors = sensorManager.sensors
    // For now we don't have a formal "availableInterventions" list in a repo, 
    // so we hardcode the known IDs or fetch from InterventionManager if it exposed them.
    val availableStrategies = listOf("LINUX_NUDGE", "OPACITY_FADE", "APP_KILLER")

    fun setActiveProfile(id: String) {
        viewModelScope.launch {
            focusProfileRepository.setActiveProfile(id)
        }
    }

    fun saveProfile(profile: FocusProfile) {
        viewModelScope.launch {
            focusProfileRepository.updateProfile(profile)
        }
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            val newProfile = FocusProfile(
                id = (0..1000000).random().toString(),
                name = name,
                thresholdMinutes = 5,
                escalationLevel = EscalationLevel.GENTLE,
                strategyMap = mapOf(
                    EscalationLevel.GENTLE to listOf("LINUX_NUDGE"),
                    EscalationLevel.FIRM to listOf("LINUX_NUDGE"),
                    EscalationLevel.AGGRESSIVE to listOf("LINUX_NUDGE")
                ),
                requiredSensorIds = listOf("WINDOW_TRACKER")
            )
            focusProfileRepository.addProfile(newProfile)
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            focusProfileRepository.deleteProfile(id)
        }
    }
}