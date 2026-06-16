package com.example.procrastination_detection.ui.analytics.strategy

import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.event.Timestamped
import com.example.procrastination_detection.domain.model.analytics.ChartData
import com.example.procrastination_detection.domain.model.analytics.TimeRange
import com.example.procrastination_detection.domain.pipeline.resampling.SystemInterventionReducer
import com.example.procrastination_detection.domain.pipeline.resampling.TimeSeriesResampler
import com.example.procrastination_detection.domain.repository.OptimizedDataResult
import com.example.procrastination_detection.domain.repository.SensorEventRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.reflect.KClass
import kotlin.time.Instant

class SystemInterventionStrategy(
    private val repository: SensorEventRepository,
    private val reducer: SystemInterventionReducer // INJECTED VIA KOIN
) : DashboardDataStrategy {

    override val dataTypeId = "intervention_count"
    override val displayName = "Interventions Triggered"
    override val chartType: KClass<out ChartData> = ChartData.Line::class
    override val compatibleEventTypes: Set<String> = setOf("SYSTEM_INTERVENTION")

    override suspend fun generateChartData(
        startTime: Long,
        endTime: Long,
        timeRange: TimeRange,
        sensorId: String?,
        sessionId: String?
    ): ChartData.Line? {
        val result = repository.getOptimizedEventsForRange(startTime, endTime, sensorId, sessionId)

        // 1. Extract and filter only the SystemInterventions, keeping them wrapped in Timestamped
        val timestampedInterventions = (result as? OptimizedDataResult.Raw)?.data
            ?.mapNotNull {
                val payload = it.payload as? SensorPayload.SystemIntervention
                if (payload != null) Timestamped(it.timestamp, payload) else null
            } ?: return null

        val bucketSize = when (timeRange) {
            TimeRange.HOURLY -> 60_000L      // 1 minute
            TimeRange.DAILY -> 3_600_000L    // 1 hour
            TimeRange.WEEKLY -> 86_400_000L  // 1 day
        }

        // 2. Bucket the data
        val buckets = TimeSeriesResampler.bucketData(
            data = timestampedInterventions,
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
        val labels = sortedEntries.map { (time, _) ->
            time
        }

        return ChartData.Line(
            lines = listOf(
                ChartData.Line.LineDataset(
                    name = "Interventions",
                    points = points,
                    color = androidx.compose.ui.graphics.Color(0xFFE91E63) // Pink/Red color for interventions
                )
            ),
            maxPoint = points.maxOrNull()?.coerceAtLeast(3f) ?: 3f,
            xCategories = labels,
            valueSuffix = " actions"
        )
    }
}
