package com.example.procrastination_detection.ui.analytics.strategy

import androidx.compose.ui.graphics.Color
import com.example.procrastination_detection.data.local.dao.SensorEventDao
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.model.analytics.ChartData
import com.example.procrastination_detection.domain.pipeline.resampling.WindowedReducer
import com.example.procrastination_detection.domain.sensor.SensorType
import kotlin.reflect.KClass

class MouseIdleStrategy(
    private val sensorEventDao: SensorEventDao,
    private val reducer: WindowedReducer<SensorPayload>
) : DashboardDataStrategy {

    override val dataTypeId: String = "mouse_idle"
    override val displayName: String = "Mouse Idle Time (s)"
    override val chartType: KClass<out ChartData> = ChartData.Line::class
    override val compatibleEventTypes: Set<String>? = setOf(SensorType.MOUSE_TRACKER.name)

    override suspend fun generateChartData(
        startTime: Long,
        endTime: Long,
        sensorId: String?
    ): ChartData? {
        val events = sensorEventDao.getEventsBetween(startTime, endTime, sensorId)
        
        val duration = endTime - startTime
        val bucketSize = if (duration <= 3_600_000L) 60_000L else 3_600_000L

        val buckets = com.example.procrastination_detection.domain.pipeline.resampling.TimeSeriesResampler.bucketData(
            data = events,
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
            name = "Idle Time (s)",
            points = points,
            color = Color(0xFFFF9800) // Orange
        )

        val maxPoint = points.maxOrNull() ?: 0f
        
        return ChartData.Line(
            lines = listOf(dataset),
            maxPoint = maxPoint,
            xCategories = xCategories,
            valueSuffix = "s"
        )
    }
}
