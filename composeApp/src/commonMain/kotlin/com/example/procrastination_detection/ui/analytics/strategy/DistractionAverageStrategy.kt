package com.example.procrastination_detection.ui.analytics.strategy

import com.example.procrastination_detection.domain.dictionary.DictionaryEngine
import com.example.procrastination_detection.domain.event.ProcessedEvent
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.event.Timestamped
import com.example.procrastination_detection.domain.model.WindowData
import com.example.procrastination_detection.domain.model.analytics.ChartData
import com.example.procrastination_detection.domain.pipeline.resampling.DistractionAverageReducer
import com.example.procrastination_detection.domain.pipeline.resampling.TimeSeriesResampler
import com.example.procrastination_detection.domain.repository.OptimizedDataResult
import com.example.procrastination_detection.domain.repository.SensorEventRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.reflect.KClass

class DistractionAverageStrategy(
    private val repository: SensorEventRepository,
    private val dictionaryEngine: DictionaryEngine,
    private val reducer: DistractionAverageReducer
) : DashboardDataStrategy {

    override val dataTypeId = "distraction_average"
    override val displayName = "Distraction Average"
    override val chartType: KClass<out ChartData> = ChartData.Line::class
    // Works with all event types that carry app context
    override val compatibleEventTypes: Set<String>? = null

    override suspend fun generateChartData(startTime: Long, endTime: Long, sensorId: String?): ChartData.Line? {
        val result = repository.getOptimizedEventsForRange(startTime, endTime, sensorId)

        // 1. Extract, map to ProcessedEvent via Dictionary
        val processedEvents = (result as? OptimizedDataResult.Raw)?.data
            ?.mapNotNull {
            val payload = it.payload
            
            // Re-categorize the event to determine if it is distracting
            val windowData = when (payload) {
                is SensorPayload.AppSwitch -> payload.windowData
                is SensorPayload.TitleChange -> payload.windowData
                is SensorPayload.BrowserOCRContext -> WindowData("browser", payload.windowTitle)
                else -> null
            }
            
            if (windowData != null) {
                val categoryMatch = dictionaryEngine.categorize(windowData)
                ProcessedEvent(it.timestamp, payload, categoryMatch.category)
            } else {
                null
            }
        } ?: return null

        val duration = endTime - startTime
        val bucketSize = if (duration <= 3_600_000L) 60_000L else 3_600_000L

        val buckets = TimeSeriesResampler.bucketData(
            data = processedEvents,
            startTime = startTime,
            endTime = endTime,
            bucketSizeMillis = bucketSize,
            timestampSelector = { it.timestamp }
        )

        // Sort the buckets once so we guarantee points and labels stay perfectly aligned
        val sortedEntries = buckets.entries.sortedBy { it.key }

        // 3. Reduce each bucket into points
        val points = sortedEntries.map { (time, items) ->
            reducer.reduce(time, items)
        }

        // 4. Generate a label for EACH bucket
        val xCategories = sortedEntries.map { (time, _) ->
            time
        }

        return ChartData.Line(
            lines = listOf(
                ChartData.Line.LineDataset(
                    name = "Distraction",
                    points = points,
                    color = androidx.compose.ui.graphics.Color(0xFFF44336)
                )
            ),
            maxPoint = points.max().coerceAtLeast(5f) ?: 5f,
            xCategories = xCategories,
            valueSuffix = "%"
        )
    }

    // Extracted formatting logic to apply per-bucket
    private fun formatBucketTimestamp(timestamp: Long, totalDurationMillis: Long): String {
        val timeZone = TimeZone.currentSystemDefault()
        val localDateTime = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone)

        return if (totalDurationMillis <= 86_400_000L) { // 24 hours or less -> Time format
            val hour = localDateTime.hour.toString().padStart(2, '0')
            val minute = localDateTime.minute.toString().padStart(2, '0')
            "$hour:$minute"
        } else { // More than 24 hours -> Date format
            val month = localDateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
            "$month $day"
        }
    }

    private fun generateXAxisLabels(startTime: Long, endTime: Long): List<String> {
        val durationMillis = endTime - startTime
        val timeZone = TimeZone.currentSystemDefault()

        fun formatTimestamp(timestamp: Long, formatAsTime: Boolean): String {
            val localDateTime = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone)

            return if (formatAsTime) {
                val hour = localDateTime.hour.toString().padStart(2, '0')
                val minute = localDateTime.minute.toString().padStart(2, '0')
                "$hour:$minute"
            } else {
                val month = localDateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
                "$month $day"
            }
        }

        return when {
            durationMillis <= 3_600_000L -> listOf(
                formatTimestamp(startTime, formatAsTime = true),
                formatTimestamp(startTime + (durationMillis / 2), formatAsTime = true),
                formatTimestamp(endTime, formatAsTime = true)
            )
            durationMillis <= 86_400_000L -> listOf(
                formatTimestamp(startTime, formatAsTime = true),
                formatTimestamp(startTime + (durationMillis / 2), formatAsTime = true),
                formatTimestamp(endTime, formatAsTime = true)
            )
            else -> listOf(
                formatTimestamp(startTime, formatAsTime = false),
                formatTimestamp(startTime + (durationMillis / 2), formatAsTime = false),
                formatTimestamp(endTime, formatAsTime = false)
            )
        }
    }
}
