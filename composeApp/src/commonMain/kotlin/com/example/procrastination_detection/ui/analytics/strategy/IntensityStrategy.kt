package com.example.procrastination_detection.ui.analytics.strategy

import androidx.compose.ui.graphics.Color
import com.example.procrastination_detection.data.local.dao.SensorEventDao
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.model.analytics.ChartData
import com.example.procrastination_detection.domain.model.analytics.TimeRange
import com.example.procrastination_detection.domain.pipeline.resampling.WindowedReducer
import com.example.procrastination_detection.domain.repository.SensorEventRepository
import com.example.procrastination_detection.domain.repository.OptimizedDataResult
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.reflect.KClass

class IntensityStrategy(
    private val repository: SensorEventRepository,
    private val reducer: WindowedReducer<SensorPayload>
) : DashboardDataStrategy {

    override val dataTypeId: String = "event_intensity"
    override val displayName: String = "Event Intensity"
    override val chartType: KClass<out ChartData> = ChartData.Line::class
    override val compatibleEventTypes: Set<String>? = null // Compatible with ALL sensors

    override suspend fun generateChartData(
        startTime: Long,
        endTime: Long,
        timeRange: TimeRange,
        sensorId: String?,
        sessionId: String?
    ): ChartData.Line? {
        val result = repository.getOptimizedEventsForRange(startTime, endTime, sensorId, sessionId)
        val events = (result as? OptimizedDataResult.Raw)?.data?.map { it.payload } ?: return null
        
        val bucketSize = when (timeRange) {
            TimeRange.HOURLY -> 60_000L      // 1 minute
            TimeRange.DAILY -> 3_600_000L    // 1 hour
            TimeRange.WEEKLY -> 86_400_000L  // 1 day
        }

        // Use the uniform TimeSeriesResampler
        val timestampedEvents = (result as? OptimizedDataResult.Raw)?.data ?: return null

        val buckets = com.example.procrastination_detection.domain.pipeline.resampling.TimeSeriesResampler.bucketData(
            data = timestampedEvents,
            startTime = startTime,
            endTime = endTime,
            bucketSizeMillis = bucketSize,
            timestampSelector = { it.timestamp }
        )

        // Sort the buckets once so we guarantee points and labels stay perfectly aligned
        val sortedEntries = buckets.entries.sortedBy { it.key }

        // 3. Reduce each bucket into points
        val points = sortedEntries.map { (time, items) ->
            reducer.reduce(time, items = items.map { it.payload })
        }

        // 4. Generate a label for EACH bucket
        val xCategories = sortedEntries.map { (time, _) ->
            time
        }


        val dataset = ChartData.Line.LineDataset(
            name = "Intensity (${sensorId ?: "All"})",
            points = points,
            color = Color(0xFFE91E63) // Pinkish
        )

        val maxPoint = points.max()
        
        return ChartData.Line(
            lines = listOf(dataset),
            maxPoint = maxPoint,
            xCategories =  xCategories,
            valueSuffix = "ev"
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
        // Reuse same label logic as others
        return when {
            durationMillis <= 3_600_000L -> listOf("Start", "30m", "End")
            durationMillis <= 86_400_000L -> listOf("Start", "12h", "End")
            else -> listOf("Start", "Mid", "End")
        }
    }
}
