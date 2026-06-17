package com.example.procrastination_detection.domain.pipeline

import com.example.procrastination_detection.data.local.dao.SensorEventDao
import com.example.procrastination_detection.data.local.dao.SessionDao
import com.example.procrastination_detection.data.local.entity.SessionEntity
import com.example.procrastination_detection.data.local.PayloadConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CsvExportEngine(
    private val sensorEventDao: SensorEventDao,
    private val sessionDao: SessionDao,
    private val fileExporter: FileExporter,
    private val scope: CoroutineScope
) {
    private val payloadConverter = PayloadConverter()
    fun exportSession(session: SessionEntity) {
        scope.launch {
            try {
                println("CsvExportEngine: Starting export for session ${session.id} with granularity ${session.granularity}")
                
                val events = sensorEventDao.getEventsForSession(session.id)
                if (events.isEmpty()) {
                    println("CsvExportEngine: No events found for session ${session.id}. Skipping export.")
                    return@launch
                }

                val bucketMs = when (session.granularity.uppercase()) {
                    "SECOND" -> 1000L
                    "MINUTE" -> 60_000L
                    "HOUR" -> 3_600_000L
                    else -> 1L // RAW
                }

                val csvBuilder = StringBuilder()
                csvBuilder.append("id,timestamp,payloadType,sensorId,payload\n")

                if (bucketMs > 1L) {
                    // Bucketing logic: group by truncated timestamp and take the first event in the bucket 
                    // for simplicity. In the future, true aggregation (e.g., sum distance, mode window) can be added here.
                    val grouped = events.groupBy { it.timestamp / bucketMs }
                    for ((_, bucketEvents) in grouped) {
                        val representative = bucketEvents.first()
                        val jsonPayload = payloadConverter.fromPayload(representative.payload).replace("\"", "\"\"")
                        csvBuilder.append("${representative.id},${representative.timestamp},${representative.payloadType},${representative.sensorId},\"${jsonPayload}\"\n")
                    }
                } else {
                    for (event in events) {
                        val jsonPayload = payloadConverter.fromPayload(event.payload).replace("\"", "\"\"")
                        csvBuilder.append("${event.id},${event.timestamp},${event.payloadType},${event.sensorId},\"${jsonPayload}\"\n")
                    }
                }

                val csvContent = csvBuilder.toString()
                val filePath = fileExporter.saveCompressedCsv(session.id, csvContent)

                println("CsvExportEngine: Successfully exported to \$filePath. Updating session and deleting raw data.")

                val updatedSession = session.copy(csvFilePath = filePath)
                sessionDao.updateSession(updatedSession)
                sensorEventDao.deleteEventsForSession(session.id)
                
            } catch (e: Exception) {
                println("CsvExportEngine: Failed to export session ${session.id}: \${e.message}")
            }
        }
    }
}
