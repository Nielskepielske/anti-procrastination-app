package com.example.procrastination_detection.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.procrastination_detection.domain.event.AggregatedPayload

@Entity(tableName = "sensor_events_hourly", primaryKeys = ["hourTimestamp", "sensorId"])
data class HourlySensorEventEntity(
    val hourTimestamp: Long, // e.g., Timestamp truncated to the nearest hour
    val payloadType: String,
    val sensorId: String,
    val payload: AggregatedPayload
)

@Entity(tableName = "sensor_events_daily", primaryKeys = ["dayTimestamp", "sensorId"])
data class DailySensorEventEntity(
    val dayTimestamp: Long, // e.g., Timestamp truncated to the nearest day (midnight)
    val payloadType: String,
    val sensorId: String,
    val payload: AggregatedPayload
)