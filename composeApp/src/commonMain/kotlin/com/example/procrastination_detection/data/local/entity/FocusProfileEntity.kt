package com.example.procrastination_detection.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity storing a single Focus Profile in the catalogue.
 *
 * strategyMapJson: JSON-encoded Map<String, List<String>>
 *   Maps escalation level name → list of InterventionStrategy IDs.
 *   e.g. {"GENTLE":["LINUX_NUDGE"],"FIRM":["LINUX_NUDGE","OPACITY_FADE"]}
 *
 * activeSensorIdsJson: JSON-encoded List<String>
 *   BehaviorSensor IDs that should be running while this profile is active.
 *   e.g. ["WINDOW_TRACKER","MOUSE_TRACKER"]
 */
@Entity(tableName = "focus_profiles")
data class FocusProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val thresholdMinutes: Int,
    val escalationLevel: String,       // EscalationLevel enum name
    val strategyMapJson: String,       // JSON: Map<EscalationLevel.name, List<String>>
    val activeSensorIdsJson: String    // JSON: List<String>
)