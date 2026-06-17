package com.example.procrastination_detection.ui.analytics.strategy

import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.model.analytics.ChartData
import com.example.procrastination_detection.domain.model.analytics.TimeRange
import com.example.procrastination_detection.domain.pipeline.resampling.AggressionHeatReducer
import com.example.procrastination_detection.domain.pipeline.resampling.TimeSeriesResampler
import com.example.procrastination_detection.domain.repository.OptimizedDataResult
import com.example.procrastination_detection.domain.repository.SensorEventRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.reflect.KClass

class AggressionHeatStrategy(
    private val repository: SensorEventRepository,
    private val reducer: AggressionHeatReducer
) : DashboardDataStrategy {

    override val dataTypeId = "aggression_heat"
    override val displayName = "Aggression Heat"
    override val chartType: KClass<out ChartData> = ChartData.Line::class
    // This explicitly only works with the SYSTEM sensor ID that emits these payloads
    override val compatibleEventTypes: Set<String>? = setOf("SYSTEM")

    override suspend fun generateChartData(
        startTime: Long,
        endTime: Long,
        timeRange: TimeRange,
        sensorId: String?,
        sessionId: String?
    ): ChartData.Line? {
        // Force the sensor ID to be SYSTEM to get only our AggressionHeat events
        val result = repository.getOptimizedEventsForRange(startTime, endTime, "SYSTEM", sessionId)
        
        val heatEvents = (result as? OptimizedDataResult.Raw)?.data?.filter { 
            it.payload is SensorPayload.AggressionHeat 
        } ?: return null

        if (heatEvents.isEmpty()) return null

        val bucketSize = when (timeRange) {
            TimeRange.HOURLY -> 60_000L      // 1 minute
            TimeRange.DAILY -> 3_600_000L    // 1 hour
            TimeRange.WEEKLY -> 86_400_000L  // 1 day
        }

        val buckets = TimeSeriesResampler.bucketData(
            data = heatEvents,
            startTime = startTime,
            endTime = endTime,
            bucketSizeMillis = bucketSize,
            timestampSelector = { it.timestamp }
        )

        val sortedEntries = buckets.entries.sortedBy { it.key }

        val points = sortedEntries.map { (time, items) ->
            val payloadItems = items.map { it.payload as SensorPayload.AggressionHeat }
            reducer.reduce(time, payloadItems)
        }

        val xCategories = sortedEntries.map { (time, _) ->
            time
        }

        return ChartData.Line(
            lines = listOf(
                ChartData.Line.LineDataset(
                    name = "Heat",
                    points = points,
                    color = androidx.compose.ui.graphics.Color(0xFFF44336) // Red
                )
            ),
            maxPoint = points.max().coerceAtLeast(5f) ?: 5f, // At least scale to 5
            xCategories = xCategories,
            valueSuffix = " Heat"
        )
    }
}
