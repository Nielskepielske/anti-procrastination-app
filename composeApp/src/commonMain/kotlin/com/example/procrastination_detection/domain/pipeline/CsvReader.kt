package com.example.procrastination_detection.domain.pipeline

import com.example.procrastination_detection.data.local.entity.SensorEventEntity
import com.example.procrastination_detection.domain.event.SensorPayload
import kotlinx.serialization.json.Json

interface CsvReader {
    /**
     * Reads a compressed CSV file, parses it, and returns a list of SensorEventEntity or Timestamped payload.
     */
    suspend fun readSessionEvents(filePath: String): List<SensorEventEntity>
}
