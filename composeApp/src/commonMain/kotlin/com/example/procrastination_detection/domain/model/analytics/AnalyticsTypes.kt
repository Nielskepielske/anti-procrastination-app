package com.example.procrastination_detection.domain.model.analytics

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

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
        val lines: List<LineDataset>,
        val maxPoint: Float,
        val labels: List<String>
    ) : ChartData {
        data class LineDataset(
            val name: String,
            val points: List<Float>,
            val color: Color
        )
    }
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
    val sensorId: String? = null,
    val colorHex: String? = null,
    val chartData: ChartData? = null
) : DashboardBlock

data class CombinedChartBlock(
    override val id: String,
    override val title: String,
    override val timeRange: TimeRange,
    override val combinationGroup: String,
    val childBlocks: List<SingleChartBlock>
) : DashboardBlock

// --- Persistent Config Models ---
@Serializable
sealed interface DashboardBlockConfig {
    val id: String
    val title: String
    val timeRange: TimeRange
    val combinationGroup: String
}

@Serializable
@SerialName("single")
data class SingleChartBlockConfig(
    override val id: String,
    override val title: String,
    override val timeRange: TimeRange,
    override val combinationGroup: String,
    val dataType: String,
    val sensorId: String? = null,
    val colorHex: String? = null
) : DashboardBlockConfig

@Serializable
@SerialName("combined")
data class CombinedChartBlockConfig(
    override val id: String,
    override val title: String,
    override val timeRange: TimeRange,
    override val combinationGroup: String,
    val childBlocks: List<SingleChartBlockConfig>
) : DashboardBlockConfig

// Extensions to map between Config and UI models
fun SingleChartBlock.toConfig() = SingleChartBlockConfig(id, title, timeRange, combinationGroup, dataType, sensorId, colorHex)
fun CombinedChartBlock.toConfig() = CombinedChartBlockConfig(id, title, timeRange, combinationGroup, childBlocks.map { it.toConfig() })
fun DashboardBlock.toConfig(): DashboardBlockConfig = when(this) {
    is SingleChartBlock -> this.toConfig()
    is CombinedChartBlock -> this.toConfig()
}

fun SingleChartBlockConfig.toBlock() = SingleChartBlock(id, title, timeRange, combinationGroup, dataType, sensorId, colorHex, null)
fun CombinedChartBlockConfig.toBlock() = CombinedChartBlock(id, title, timeRange, combinationGroup, childBlocks.map { it.toBlock() })
fun DashboardBlockConfig.toBlock(): DashboardBlock = when(this) {
    is SingleChartBlockConfig -> this.toBlock()
    is CombinedChartBlockConfig -> this.toBlock()
}