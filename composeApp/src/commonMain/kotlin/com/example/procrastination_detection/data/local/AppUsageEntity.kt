package com.example.procrastination_detection.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayIndex: Long, // Days since Epoch (timestamp / 86400000)
    val processName: String,
    val windowTitle: String,
    val totalSeconds: Long
)
