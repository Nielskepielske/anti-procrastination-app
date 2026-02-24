package com.example.procrastination_detection.models.db

data class MonitoredProcess(
    val id: String,
    val process: Process,
    val sessionId: String,
    val consecutiveSeconds: Long,
    val totalSeconds: Long
)
