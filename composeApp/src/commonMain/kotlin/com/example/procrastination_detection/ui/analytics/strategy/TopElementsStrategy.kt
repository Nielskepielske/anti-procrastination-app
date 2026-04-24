package com.example.procrastination_detection.ui.analytics.strategy

import androidx.compose.ui.graphics.Color
import com.example.procrastination_detection.data.local.dao.AppUsageDao
import com.example.procrastination_detection.domain.model.analytics.ChartData
import kotlinx.coroutines.flow.firstOrNull
import kotlin.reflect.KClass

class TopElementsStrategy(
    private val appUsageDao: AppUsageDao
) : DashboardDataStrategy {

    override val dataTypeId: String = "top_elements"
    override val displayName: String = "Top Applications"
    override val chartType: KClass<out ChartData> = ChartData.Bar::class
    override val compatibleEventTypes: Set<String>? = setOf("WINDOW_TRACKER", "app_usage")

    override suspend fun generateChartData(
        startTime: Long,
        endTime: Long,
        sensorId: String?
    ): ChartData? {
        val startDay = startTime / 86400000L
        val endDay = endTime / 86400000L

        val usages = appUsageDao.getUsageForRange(startDay, endDay).firstOrNull() ?: emptyList()

        if (usages.isEmpty()) return null

        // Aggregate by processName
        val aggregated = usages.groupBy { it.processName }
            .mapValues { entry -> entry.value.sumOf { it.totalSeconds } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        if (aggregated.isEmpty()) return null

        val maxSeconds = aggregated.maxOf { it.second }.toFloat()
        val colors = listOf(Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFFF44336))

        val items = aggregated.mapIndexed { index, (name, seconds) ->
            val color = colors[index % colors.size]
            val minutes = seconds / 60
            val detail = if (minutes > 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
            ChartData.Bar.BarItem(
                label = name,
                detail = detail,
                value = seconds.toFloat(),
                color = color
            )
        }

        return ChartData.Bar(items = items, maxValue = maxSeconds)
    }
}
