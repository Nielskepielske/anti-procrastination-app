package com.example.procrastination_detection.domain.event

/**
 * A domain-level wrapper that preserves the time context of a payload
 * without leaking database-specific entities (like SensorEventEntity) to the UI.
 */
data class Timestamped<out T>(
    val timestamp: Long,
    val payload: T
)