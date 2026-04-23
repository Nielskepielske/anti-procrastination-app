package com.example.procrastination_detection.ui.analytics

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.procrastination_detection.data.local.dao.AppUsageDao
import com.example.procrastination_detection.data.local.dao.SensorEventDao
import com.example.procrastination_detection.domain.model.analytics.ChartData
import com.example.procrastination_detection.domain.dictionary.DictionaryEngine
import com.example.procrastination_detection.domain.model.analytics.*
import com.example.procrastination_detection.ui.analytics.strategy.DashboardDataStrategy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock

class FlexibleAnalyticsViewModel(
    strategies: Set<DashboardDataStrategy>
) : ViewModel() {

    private val strategyMap = strategies.associateBy { it.dataTypeId }
    private val _blocks = MutableStateFlow<List<DashboardBlock>>(emptyList())
    val blocks: StateFlow<List<DashboardBlock>> = _blocks.asStateFlow()

    // 1. Keep track of the live update loop
    private var liveUpdateJob: Job? = null

    init {
        _blocks.value = listOf(
            SingleChartBlock("1", "Focus Score", TimeRange.DAILY, "PROGRESS", "focus_score"),
            SingleChartBlock("2", "Top Activities", TimeRange.DAILY, "BAR", "top_activities"),
            SingleChartBlock("3", "Switch Frequency", TimeRange.HOURLY, "LINE", "switch_frequency"),
        )

        // 2. Start the infinite ticker instead of doing a one-time fetch
        startLiveUpdateLoop()
    }

    private fun startLiveUpdateLoop() {
        liveUpdateJob?.cancel()

        liveUpdateJob = viewModelScope.launch {
            while (isActive) {
                // Quietly refresh the data for every single block
                _blocks.value.forEach { block ->
                    if (block is SingleChartBlock) {
                        fetchBlockDataQuietly(block)
                    }
                }

                // Sleep for 30 seconds, then recalculate.
                // You can drop this to 10 seconds for testing!
                delay(30_000L)
            }
        }
    }

    fun updateTimeRange(blockId: String, newRange: TimeRange) {
        val currentBlocks = _blocks.value.toMutableList()
        val index = currentBlocks.indexOfFirst { it.id == blockId }
        if (index == -1) return

        val block = currentBlocks[index]
        if (block is SingleChartBlock) {
            // Because this is a user action, we DO want to show a loading state
            // by setting chartData = null immediately.
            val updatedBlock = block.copy(timeRange = newRange, chartData = null)
            currentBlocks[index] = updatedBlock
            _blocks.value = currentBlocks

            // Force an immediate fetch so the user doesn't wait for the next 30s tick
            viewModelScope.launch {
                fetchBlockDataQuietly(updatedBlock)
            }
        }
    }

    /**
     * Fetches data and updates the state WITHOUT setting chartData to null first.
     * This ensures the UI updates smoothly without flashing a loading spinner.
     */
    private suspend fun fetchBlockDataQuietly(block: SingleChartBlock) {
        val (start, end) = calculateTimeWindow(block.timeRange)

        // 1. Find the strategy for this block's data type
        val strategy = strategyMap[block.dataType]

        // 2. Let the strategy fetch and format the data
        val data = strategy?.generateChartData(start, end)

        // 3. Update the state
        val currentBlocks = _blocks.value.toMutableList()
        val index = currentBlocks.indexOfFirst { it.id == block.id }
        if (index != -1) {
            currentBlocks[index] = (currentBlocks[index] as SingleChartBlock).copy(chartData = data)
            _blocks.value = currentBlocks
        }
    }

    private fun calculateTimeWindow(range: TimeRange): Pair<Long, Long> {
        val now = Clock.System.now().toEpochMilliseconds()
        val start = when (range) {
            TimeRange.HOURLY -> now - 3_600_000L
            TimeRange.DAILY -> now - 86_400_000L
            TimeRange.WEEKLY -> now - (86_400_000L * 7)
        }
        return Pair(start, now)
    }

    // --- Data Fetching (Adapted to return generic ChartData) ---

    private suspend fun fetchFocusScore(start: Long, end: Long): ChartData.Progress {
        // Replace with actual DB query calculation
        return ChartData.Progress(percentage = 78, label = "Focus", detail = "+5% vs previous")
    }

    private suspend fun fetchTopApps(start: Long, end: Long): ChartData.Bar {
        // Replace with actual DB query calculation
        return ChartData.Bar(
            maxValue = 3600f,
            items = listOf(
                ChartData.Bar.BarItem("IntelliJ", "45m", 2700f, Color(0xFF4CAF50)),
                ChartData.Bar.BarItem("Chrome", "15m", 900f, Color(0xFFF44336))
            )
        )
    }

    private suspend fun fetchSwitchFrequency(start: Long, end: Long): ChartData.Line {
        // Replace with actual DB query calculation
        val mockPoints = listOf(2f, 15f, 5f, 20f, 10f, 3f)
        return ChartData.Line(
            points = mockPoints,
            maxPoint = mockPoints.maxOrNull() ?: 20f,
            labels = listOf("Start", "Mid", "End")
        )
    }
}