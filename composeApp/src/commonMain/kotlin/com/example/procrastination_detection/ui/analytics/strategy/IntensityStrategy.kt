package com.example.procrastination_detection.ui.analytics.strategy

import androidx.compose.ui.graphics.Color
import com.example.procrastination_detection.data.local.dao.SensorEventDao
import com.example.procrastination_detection.domain.model.analytics.ChartData
import kotlin.reflect.KClass

class IntensityStrategy(
    private val sensorEventDao: SensorEventDao
) : DashboardDataStrategy {

    override val dataTypeId: String = "event_intensity"
    override val displayName: String = "Event Intensity"
    override val chartType: KClass<out ChartData> = ChartData.Line::class
    override val compatibleEventTypes: Set<String>? = null // Compatible with ALL sensors

    override suspend fun generateChartData(
        startTime: Long,
        endTime: Long,
        sensorId: String?
    ): ChartData? {
        val events = sensorEventDao.getEventsBetween(startTime, endTime, sensorId)
        
        val duration = endTime - startTime
        val bucketSize = if (duration <= 3_600_000L) 60_000L else 3_600_000L

        // Use the uniform TimeSeriesResampler
        val buckets = com.example.procrastination_detection.domain.pipeline.resampling.TimeSeriesResampler.bucketData(
            data = events,
            startTime = startTime,
            endTime = endTime,
            bucketSizeMillis = bucketSize,
            timestampSelector = { it.timestamp }
        )
        
        val points = buckets.entries.sortedBy { it.key }.map { it.value.size.toFloat() }
        
        val maxPoint = points.maxOrNull() ?: 10f
        
        val dataset = ChartData.Line.LineDataset(
            name = "Intensity (${sensorId ?: "All"})",
            points = points,
            color = Color(0xFFE91E63) // Pinkish
        )
        
        return ChartData.Line(
            lines = listOf(dataset),
            maxPoint = maxPoint,
            labels = generateXAxisLabels(startTime, endTime)
        )
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
