package com.example.procrastination_detection.domain.model.analytics

import androidx.compose.ui.graphics.Color

enum class TimeRange(val displayName: String) {
    HOURLY("Hourly"),
    DAILY("Daily"),
    WEEKLY("Weekly")
}

enum class BlockDataType {
    FOCUS_SCORE, CATEGORY_BREAKDOWN, TOP_APPS, SWITCH_FREQUENCY
}

// --- The UI-Agnostic Chart Data ---
sealed interface ChartData {
    data class Progress(
        val percentage: Int,
        val label: String,
        val detail: String
    ) : ChartData

    data class Bar(
        val items: List<BarItem>,
        val maxValue: Float
    ) : ChartData {
        data class BarItem(
            val label: String,
            val detail: String,
            val value: Float,
            val color: Color
        )
    }

    data class Line(
        val points: List<Float>,
        val maxPoint: Float,
        val labels: List<String>
    ) : ChartData
}

// --- The Composite Block Architecture ---
sealed interface DashboardBlock {
    val id: String
    val title: String
    val timeRange: TimeRange
    val combinationGroup: String
}

data class SingleChartBlock(
    override val id: String,
    override val title: String,
    override val timeRange: TimeRange,
    override val combinationGroup: String,
    val dataType: String,
    val chartData: ChartData? = null
) : DashboardBlock

data class CombinedChartBlock(
    override val id: String,
    override val title: String,
    override val timeRange: TimeRange,
    override val combinationGroup: String,
    val childBlocks: List<SingleChartBlock>
) : DashboardBlock