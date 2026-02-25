package com.example.procrastination_detection.models.db.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("monitored_processes")
data class MonitoredProcessEntity(
    @PrimaryKey
    val id: String,
    val processId: String,
    val sessionId: String,
    val consecutiveSeconds: Long,
    var totalSeconds: Long
)