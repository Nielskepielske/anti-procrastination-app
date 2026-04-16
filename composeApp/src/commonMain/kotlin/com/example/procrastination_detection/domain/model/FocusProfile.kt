package com.example.procrastination_detection.domain.model

data class FocusProfile(
    val id: String,
    val name: String,
    val thresholdMinutes: Int,
    val escalationLevel: EscalationLevel,
    /** Maps each escalation level to the list of InterventionStrategy IDs that fire at that level. */
    val strategyMap: Map<EscalationLevel, List<String>>,
    /** BehaviorSensor IDs that should be running while this profile is active. */
    val requiredSensorIds: List<String>
)

enum class EscalationLevel { GENTLE, FIRM, AGGRESSIVE }

/** Factory for the out-of-the-box profile created on first launch. */
fun defaultFocusProfile(id: String = (0..1000000).random().toString()) = FocusProfile(
    id = id,
    name = "Deep Work",
    thresholdMinutes = 5,
    escalationLevel = EscalationLevel.GENTLE,
    strategyMap = mapOf(
        EscalationLevel.GENTLE     to listOf("LINUX_NUDGE"),
        EscalationLevel.FIRM       to listOf("LINUX_NUDGE", "OPACITY_FADE"),
        EscalationLevel.AGGRESSIVE to listOf("LINUX_NUDGE", "OPACITY_FADE", "APP_KILLER"),
    ),
    requiredSensorIds = listOf("WINDOW_TRACKER")
)
