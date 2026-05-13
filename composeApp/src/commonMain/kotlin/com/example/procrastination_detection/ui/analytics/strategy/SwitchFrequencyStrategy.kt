package com.example.procrastination_detection.ui.analytics.strategy

import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.event.Timestamped
import com.example.procrastination_detection.domain.model.analytics.ChartData
import com.example.procrastination_detection.domain.pipeline.resampling.SwitchCountReducer
import com.example.procrastination_detection.domain.pipeline.resampling.TimeSeriesResampler
import com.example.procrastination_detection.domain.repository.OptimizedDataResult
import com.example.procrastination_detection.domain.repository.SensorEventRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.reflect.KClass
import kotlin.time.Instant

class SwitchFrequencyStrategy(
    private val repository: SensorEventRepository,
    private val reducer: SwitchCountReducer // INJECTED VIA KOIN
) : DashboardDataStrategy {

    override val dataTypeId = "switch_frequency"
    override val displayName = "Switch Frequency"
    override val chartType: KClass<out ChartData> = ChartData.Line::class
    override val compatibleEventTypes: Set<String> = setOf("APP_SWITCH")

    override suspend fun generateChartData(startTime: Long, endTime: Long, sensorId: String?): ChartData.Line? {
        val result = repository.getOptimizedEventsForRange(startTime, endTime, sensorId)

        // 1. Extract and filter only the AppSwitches, keeping them wrapped in Timestamped
        val timestampedSwitches = (result as? OptimizedDataResult.Raw)?.data
            ?.mapNotNull {
                val payload = it.payload as? SensorPayload.AppSwitch
                if (payload != null) Timestamped(it.timestamp, payload) else null
            } ?: return null

        val duration = endTime - startTime
        val bucketSize = if (duration <= 3_600_000L) 60_000L else 3_600_000L

        // 2. Bucket the data! We now have access to the timestamp!
        val buckets = TimeSeriesResampler.bucketData(
            data = timestampedSwitches,
            startTime = startTime,
            endTime = endTime,
            bucketSizeMillis = bucketSize,
            timestampSelector = { it.timestamp } // <--- This now works perfectly!
        )


        // Sort the buckets once so we guarantee points and labels stay perfectly aligned
        val sortedEntries = buckets.entries.sortedBy { it.key }

        // 3. Reduce each bucket into points
        val points = sortedEntries.map { (time, items) ->
            reducer.reduce(time, items = items.map { it.payload })
        }

        // 4. Generate a label for EACH bucket
        val labels = sortedEntries.map { (time, _) ->
            time
        }


        return ChartData.Line(
            lines = listOf(
                ChartData.Line.LineDataset(
                    name = "Switches",
                    points = points,
                    color = androidx.compose.ui.graphics.Color(0xFF2196F3)
                )
            ),
            maxPoint = points.maxOrNull()?.coerceAtLeast(5f) ?: 5f,
            xCategories = labels
        )
    }
    private fun formatBucketTimestamp(timestamp: Long, totalDurationMillis: Long): String {
        val timeZone = TimeZone.currentSystemDefault()
        val localDateTime = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone)

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

        // 1. Helper function to safely format dates in KMP without java.time
        fun formatTimestamp(timestamp: Long, formatAsTime: Boolean): String {
            val localDateTime = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone)

            return if (formatAsTime) {
                // Formats to "HH:mm" (e.g., "08:30" or "14:00")
                val hour = localDateTime.hour.toString().padStart(2, '0')
                val minute = localDateTime.minute.toString().padStart(2, '0')
                "$hour:$minute"
            } else {
                // Formats to "Mon dd" (e.g., "Apr 23" or "Oct 05")
                val month = localDateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
                "$month $day"
            }
        }

        // 2. Determine what labels to show based on the total time range
        return when {
            // Hourly View (<= 1 Hour): Show "Start", "30 mins", "End" times
            durationMillis <= 3_600_000L -> listOf(
                formatTimestamp(startTime, formatAsTime = true),
                formatTimestamp(startTime + (durationMillis / 2), formatAsTime = true),
                formatTimestamp(endTime, formatAsTime = true)
            )

            // Daily View (<= 24 Hours): Show "Start", "12 hours", "End" times
            durationMillis <= 86_400_000L -> listOf(
                formatTimestamp(startTime, formatAsTime = true),
                formatTimestamp(startTime + (durationMillis / 2), formatAsTime = true),
                formatTimestamp(endTime, formatAsTime = true)
            )

            // Weekly/Monthly View (> 24 Hours): Show Dates instead of Times
            else -> listOf(
                formatTimestamp(startTime, formatAsTime = false),
                formatTimestamp(startTime + (durationMillis / 2), formatAsTime = false),
                formatTimestamp(endTime, formatAsTime = false)
            )
        }
    }
}