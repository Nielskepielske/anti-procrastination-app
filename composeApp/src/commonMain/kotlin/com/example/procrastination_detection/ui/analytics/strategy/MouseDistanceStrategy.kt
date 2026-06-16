package com.example.procrastination_detection.ui.analytics.strategy

import androidx.compose.ui.graphics.Color
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.model.analytics.ChartData
import com.example.procrastination_detection.domain.model.analytics.TimeRange
import com.example.procrastination_detection.domain.pipeline.resampling.WindowedReducer
import com.example.procrastination_detection.domain.pipeline.resampling.MouseDistanceReducer
import com.example.procrastination_detection.domain.repository.SensorEventRepository
import com.example.procrastination_detection.domain.repository.OptimizedDataResult
import com.example.procrastination_detection.domain.sensor.SensorType
import kotlin.reflect.KClass

class MouseDistanceStrategy(
    private val repository: SensorEventRepository,
    private val reducer: WindowedReducer<SensorPayload>
) : DashboardDataStrategy {

    override val dataTypeId: String = "mouse_distance"
    override val displayName: String = "Mouse Activity (Distance)"
    override val chartType: KClass<out ChartData> = ChartData.Line::class
    override val compatibleEventTypes: Set<String>? = setOf(SensorType.MOUSE_TRACKER.name)

    override suspend fun generateChartData(
        startTime: Long,
        endTime: Long,
        timeRange: TimeRange,
        sensorId: String?,
        sessionId: String?
    ): ChartData.Line? {
        val result = repository.getOptimizedEventsForRange(startTime, endTime, sensorId, sessionId)
        val timestampedEvents = (result as? OptimizedDataResult.Raw)?.data ?: return null
        
        val bucketSize = when (timeRange) {
            TimeRange.HOURLY -> 60_000L      // 1 minute
            TimeRange.DAILY -> 3_600_000L    // 1 hour
            TimeRange.WEEKLY -> 86_400_000L  // 1 day
        }

        val buckets = com.example.procrastination_detection.domain.pipeline.resampling.TimeSeriesResampler.bucketData(
            data = timestampedEvents,
            startTime = startTime,
            endTime = endTime,
            bucketSizeMillis = bucketSize,
            timestampSelector = { it.timestamp }
        )

        val sortedEntries = buckets.entries.sortedBy { it.key }

        val points = sortedEntries.map { (time, items) ->
            reducer.reduce(time, items = items.map { it.payload })
        }

        val xCategories = sortedEntries.map { (time, _) ->
            time
        }

        val dataset = ChartData.Line.LineDataset(
            name = "Distance",
            points = points,
            color = Color(0xFF2196F3) // Blueish
        )

        val maxPoint = points.maxOrNull() ?: 0f
        
        return ChartData.Line(
            lines = listOf(dataset),
            maxPoint = maxPoint,
            xCategories = xCategories,
            valueSuffix = "px"
        )
    }
}
