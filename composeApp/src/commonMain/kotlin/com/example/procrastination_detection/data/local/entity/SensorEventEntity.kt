package com.example.procrastination_detection.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.procrastination_detection.domain.event.SensorPayload

@Entity(tableName = "sensor_events")
data class SensorEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val payloadType: String,   // e.g. "APP_SWITCH", "TITLE_CHANGE", "BROWSER_OCR" — indexed for fast behavioral queries
    val payload: SensorPayload
)