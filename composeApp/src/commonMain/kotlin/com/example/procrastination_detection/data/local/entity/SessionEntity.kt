package com.example.procrastination_detection.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val startTime: Long,
    val endTime: Long?,
    val status: String, // "ACTIVE", "PAUSED", "COMPLETED"
    val csvFilePath: String?,
    val granularity: String // "RAW", "SECOND", "MINUTE", "HOUR"
)
