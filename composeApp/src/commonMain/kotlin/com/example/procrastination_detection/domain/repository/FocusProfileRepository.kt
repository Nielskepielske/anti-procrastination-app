package com.example.procrastination_detection.domain.repository

import com.example.procrastination_detection.data.local.ActiveProfileStore
import com.example.procrastination_detection.data.local.FocusProfileDao
import com.example.procrastination_detection.data.local.FocusProfileEntity
import com.example.procrastination_detection.domain.model.EscalationLevel
import com.example.procrastination_detection.domain.model.FocusProfile
import com.example.procrastination_detection.domain.sensor.SensorManager
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FocusProfileRepository(
    private val focusProfileDao: FocusProfileDao,
    private val activeProfileStore: ActiveProfileStore,
    private val sensorManager: SensorManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    val allProfilesFlow: Flow<List<FocusProfile>> = focusProfileDao.getAllProfiles()
        .onEach { entities ->
            if (entities.isEmpty()) {
                val default = com.example.procrastination_detection.domain.model.defaultFocusProfile()
                addProfile(default)
                setActiveProfile(default.id)
            }
        }
        .map { entities -> entities.map { it.toDomain() } }

    val activeProfileFlow: Flow<FocusProfile?> = activeProfileStore.activeProfileIdFlow
        .flatMapLatest { id ->
            if (id == null) {
                // If no active ID but profiles exist, pick the first one
                allProfilesFlow.map { it.firstOrNull() }
            } else {
                flow {
                    emit(focusProfileDao.getProfileById(id)?.toDomain())
                }
            }
        }.distinctUntilChanged()

    suspend fun addProfile(profile: FocusProfile) {
        focusProfileDao.upsertProfile(profile.toEntity())
    }

    suspend fun updateProfile(profile: FocusProfile) {
        focusProfileDao.upsertProfile(profile.toEntity())
        // If this was the active profile, re-apply it to sensors immediately
        activeProfileStore.activeProfileIdFlow.first()?.let { activeId ->
            if (activeId == profile.id) {
                sensorManager.applyProfile(profile)
            }
        }
    }

    suspend fun deleteProfile(id: String) {
        focusProfileDao.deleteProfile(id)
    }

    suspend fun setActiveProfile(id: String) {
        activeProfileStore.setActiveProfileId(id)
        val profile = focusProfileDao.getProfileById(id)?.toDomain()
        if (profile != null) {
            sensorManager.applyProfile(profile)
        }
    }

    private fun FocusProfileEntity.toDomain(): FocusProfile {
        return FocusProfile(
            id = id,
            name = name,
            thresholdMinutes = thresholdMinutes,
            escalationLevel = EscalationLevel.valueOf(escalationLevel),
            strategyMap = json.decodeFromString(strategyMapJson),
            requiredSensorIds = json.decodeFromString(activeSensorIdsJson)
        )
    }

    private fun FocusProfile.toEntity(): FocusProfileEntity {
        return FocusProfileEntity(
            id = id,
            name = name,
            thresholdMinutes = thresholdMinutes,
            escalationLevel = escalationLevel.name,
            strategyMapJson = json.encodeToString(strategyMap),
            activeSensorIdsJson = json.encodeToString(requiredSensorIds)
        )
    }
}
