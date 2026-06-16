package com.example.procrastination_detection.platform.desktop

import com.example.procrastination_detection.data.local.PayloadConverter
import com.example.procrastination_detection.data.local.entity.SensorEventEntity
import com.example.procrastination_detection.domain.pipeline.CsvReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.zip.GZIPInputStream

class DesktopCsvReader : CsvReader {
    private val payloadConverter = PayloadConverter()

    override suspend fun readSessionEvents(filePath: String): List<SensorEventEntity> = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            println("DesktopCsvReader: File not found - $filePath")
            return@withContext emptyList()
        }

        val events = mutableListOf<SensorEventEntity>()
        
        try {
            FileInputStream(file).use { fis ->
                GZIPInputStream(fis).use { gzip ->
                    val reader = gzip.bufferedReader()
                    // Skip header
                    reader.readLine()
                    
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line ?: continue
                        // We use a simple CSV parsing: split by ',' but respect quotes.
                        // Since payload is the last column and might contain commas inside JSON,
                        // we can split with a limit of 5.
                        val parts = currentLine.split(",", limit = 5)
                        if (parts.size >= 5) {
                            val id = parts[0].toLongOrNull() ?: 0L
                            val timestamp = parts[1].toLongOrNull() ?: 0L
                            val payloadType = parts[2]
                            val sensorId = parts[3]
                            // The payload is wrapped in quotes, and internal quotes are escaped as ""
                            var jsonPayload = parts[4]
                            if (jsonPayload.startsWith("\"") && jsonPayload.endsWith("\"")) {
                                jsonPayload = jsonPayload.substring(1, jsonPayload.length - 1)
                            }
                            jsonPayload = jsonPayload.replace("\"\"", "\"")
                            
                            try {
                                val payload = payloadConverter.toPayload(jsonPayload)
                                events.add(
                                    SensorEventEntity(
                                        id = id,
                                        timestamp = timestamp,
                                        payloadType = payloadType,
                                        sensorId = sensorId,
                                        sessionId = "UNKNOWN_CSV", // We only use it in memory
                                        payload = payload
                                    )
                                )
                            } catch (e: Exception) {
                                println("DesktopCsvReader: Failed to parse payload: $jsonPayload")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("DesktopCsvReader: Error reading file $filePath: ${e.message}")
        }
        
        events
    }
}
