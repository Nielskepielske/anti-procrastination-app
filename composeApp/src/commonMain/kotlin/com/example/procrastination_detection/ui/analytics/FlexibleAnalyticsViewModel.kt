package com.example.procrastination_detection.ui.analytics

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.procrastination_detection.domain.model.analytics.*
import com.example.procrastination_detection.ui.analytics.strategy.DashboardDataStrategy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import com.example.procrastination_detection.data.local.AnalyticsConfigStore

import com.example.procrastination_detection.domain.sensor.SensorManager

import com.example.procrastination_detection.data.local.dao.SessionDao
import com.example.procrastination_detection.data.local.entity.SessionEntity

import com.example.procrastination_detection.domain.pipeline.SessionDownloader
import com.example.procrastination_detection.domain.repository.SensorEventRepository
import com.example.procrastination_detection.domain.repository.OptimizedDataResult
import com.example.procrastination_detection.domain.event.SensorPayload

class FlexibleAnalyticsViewModel(
    strategies: Set<DashboardDataStrategy>,
    private val sensorManager: SensorManager,
    private val configStore: AnalyticsConfigStore,
    private val sessionDao: SessionDao,
    private val sessionDownloader: SessionDownloader,
    private val sensorEventRepository: SensorEventRepository
) : ViewModel() {

    private val strategyMap = strategies.associateBy { it.dataTypeId }
    /** List of (dataTypeId, displayName) pairs for the UI picker. */
    val availableStrategies: List<Pair<String, String>> = strategies.map { Pair(it.dataTypeId, it.displayName) }
    
    val availableSensors: StateFlow<List<Pair<String, String>>> =
        MutableStateFlow(sensorManager.sensors.map { Pair(it.type.name, it.type.name) })

    /**
     * Returns only the sensors whose emitted event types are compatible with the given strategy.
     * If the strategy has null compatibleEventTypes it accepts all sensors.
     */
    fun sensorsForStrategy(strategyId: String): List<Pair<String, String>> {
        println("All strategies: " + availableStrategies)
        val strategy = strategyMap[strategyId] ?: return availableSensors.value
        // If strategy is not restricted, all sensors are valid
        val compatibleTypes = strategy.compatibleEventTypes ?: return availableSensors.value
        
        // We match sensor id against the event-type set by convention:
        // sensors that have ever produced matching events pass through.
        return availableSensors.value.filter { (sensorId, _) ->
            compatibleTypes.contains(sensorId)
        }
    }

    fun strategiesForSensor(sensorId: String?): List<Pair<String, String>> {
        if (sensorId == null || sensorId == "all") return availableStrategies
        return strategyMap.values.filter { strategy ->
            strategy.compatibleEventTypes == null || strategy.compatibleEventTypes!!.contains(sensorId)
        }.map { Pair(it.dataTypeId, it.displayName) }
    }

    /**
     * Returns whether [blockId] can be combined with [otherId].
     * Both must be SingleChartBlocks of the same chartType.
     */
    fun canCombine(blockId: String, otherId: String): Boolean {
        val b1 = _blocks.value.find { it.id == blockId } ?: return false
        val b2 = _blocks.value.find { it.id == otherId } ?: return false

        val b1Type = when (b1) {
            is SingleChartBlock -> strategyMap[b1.dataType]?.chartType
            is CombinedChartBlock -> strategyMap[b1.childBlocks.firstOrNull()?.dataType]?.chartType
        }
        val b2Type = when (b2) {
            is SingleChartBlock -> strategyMap[b2.dataType]?.chartType
            is CombinedChartBlock -> strategyMap[b2.childBlocks.firstOrNull()?.dataType]?.chartType
        }

        return b1Type != null && b1Type == b2Type
    }

    private val _blocks = MutableStateFlow<List<DashboardBlock>>(emptyList())
    val blocks: StateFlow<List<DashboardBlock>> = _blocks.asStateFlow()

    // 1. Keep track of the live update loop
    private var liveUpdateJob: Job? = null

    private val _sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessions: StateFlow<List<SessionEntity>> = _sessions.asStateFlow()

    private val _selectedSessionId = MutableStateFlow<String?>(null)
    val selectedSessionId: StateFlow<String?> = _selectedSessionId.asStateFlow()

    fun selectSession(sessionId: String?) {
        _selectedSessionId.value = sessionId
        // Refresh data
        val currentBlocks = _blocks.value
        viewModelScope.launch {
            currentBlocks.forEach { block ->
                if (block is SingleChartBlock) {
                    fetchBlockDataQuietly(block)
                } else if (block is CombinedChartBlock) {
                    block.childBlocks.forEach { child -> fetchBlockDataQuietly(child) }
                }
            }
        }
    }

    fun downloadSelectedSession() {
        val sessionId = _selectedSessionId.value ?: return
        val session = _sessions.value.find { it.id == sessionId } ?: return
        val filePath = session.csvFilePath ?: return

        viewModelScope.launch {
            val suggestedName = "session_${sessionId.take(8)}.csv"
            sessionDownloader.downloadSession(filePath, suggestedName)
        }
    }

    init {
        viewModelScope.launch {
            val configs = configStore.configFlow.first()
            if (configs.isNotEmpty()) {
                val initialBlocks = configs.map { it.toBlock() }
                _blocks.value = initialBlocks
                initialBlocks.forEach { block ->
                    if (block is SingleChartBlock) {
                        fetchBlockDataQuietly(block)
                    } else if (block is CombinedChartBlock) {
                        block.childBlocks.forEach { fetchBlockDataQuietly(it) }
                    }
                }
            } else {
                val defaultBlocks = listOf(
                    SingleChartBlock(id = "1", title = "Focus Score", timeRange = TimeRange.DAILY, combinationGroup = "PROGRESS", dataType = "focus_score"),
                    SingleChartBlock(id = "2", title = "Top Activities", timeRange = TimeRange.DAILY, combinationGroup = "BAR", dataType = "top_activities"),
                    SingleChartBlock(id = "3", title = "Switch Frequency", timeRange = TimeRange.HOURLY, combinationGroup = "LINE", dataType = "switch_frequency")
                )
                _blocks.value = defaultBlocks
                saveCurrentConfig()
                defaultBlocks.forEach { fetchBlockDataQuietly(it) }
            }
            
            // Load sessions
            viewModelScope.launch {
                sessionDao.getAllSessionsFlow().collect { sessionsList ->
                    _sessions.value = sessionsList.sortedByDescending { it.startTime }
                }
            }

            startLiveUpdateLoop()
        }
    }

    private fun saveCurrentConfig() {
        viewModelScope.launch {
            configStore.saveConfig(_blocks.value.map { it.toConfig() })
        }
    }

    private fun startLiveUpdateLoop() {
        liveUpdateJob?.cancel()

        liveUpdateJob = viewModelScope.launch {
            while (isActive) {
                // Use a single consistent window for all blocks in this refresh cycle
                val currentBlocks = _blocks.value
                currentBlocks.forEach { block ->
                    if (block is SingleChartBlock) {
                        val (start, end) = calculateTimeWindow(block.timeRange)
                        fetchBlockDataQuietly(block, start, end)
                    } else if (block is CombinedChartBlock) {
                        val (start, end) = calculateTimeWindow(block.timeRange)
                        block.childBlocks.forEach { child ->
                            fetchBlockDataQuietly(child, start, end)
                        }
                    }
                }

                // Sleep for 30 seconds, then recalculate.
                delay(30_000L)
            }
        }
    }

    fun addBlock(title: String, dataType: String, sensorId: String? = null) {
        val newId = Clock.System.now().toEpochMilliseconds().toString()
        val finalSensorId = if (sensorId == "all") null else sensorId
        val newBlock = SingleChartBlock(
            id = newId,
            title = title,
            timeRange = TimeRange.DAILY, // Default
            combinationGroup = "",
            dataType = dataType,
            sensorId = finalSensorId
        )
        _blocks.value = _blocks.value + newBlock
        saveCurrentConfig()
        
        // Fetch data immediately
        viewModelScope.launch {
            val (start, end) = calculateTimeWindow(newBlock.timeRange)
            fetchBlockDataQuietly(newBlock, start, end)
        }
    }

    fun deleteBlock(blockId: String) {
        _blocks.value = _blocks.value.filter { it.id != blockId }
        saveCurrentConfig()
    }

    fun editBlockStrategy(blockId: String, newDataType: String) {
        val currentBlocks = _blocks.value.toMutableList()
        val index = currentBlocks.indexOfFirst { it.id == blockId }
        if (index == -1) return

        val block = currentBlocks[index]
        if (block is SingleChartBlock) {
            val updatedBlock = block.copy(dataType = newDataType, chartData = null)
            currentBlocks[index] = updatedBlock
            _blocks.value = currentBlocks
            saveCurrentConfig()

            viewModelScope.launch {
                fetchBlockDataQuietly(updatedBlock)
            }
        }
    }

    fun editBlockColor(blockId: String, colorHex: String?) {
        val currentBlocks = _blocks.value.toMutableList()
        val index = currentBlocks.indexOfFirst { it.id == blockId }
        if (index == -1) return

        val block = currentBlocks[index]
        if (block is SingleChartBlock) {
            currentBlocks[index] = block.copy(colorHex = colorHex)
            _blocks.value = currentBlocks
            saveCurrentConfig()
        }
    }

    fun updateChildInCombined(combinedId: String, childId: String, newStrategy: String, newColorHex: String?) {
        val currentBlocks = _blocks.value.toMutableList()
        val index = currentBlocks.indexOfFirst { it.id == combinedId }
        if (index == -1) return

        val block = currentBlocks[index]
        if (block is CombinedChartBlock) {
            val updatedChildren = block.childBlocks.map { child ->
                if (child.id == childId) {
                    val strategyChanged = child.dataType != newStrategy
                    child.copy(
                        dataType = newStrategy, 
                        colorHex = newColorHex, 
                        chartData = if (strategyChanged) null else child.chartData
                    )
                } else {
                    child
                }
            }
            currentBlocks[index] = block.copy(childBlocks = updatedChildren)
            _blocks.value = currentBlocks
            saveCurrentConfig()
            
            val (start, end) = calculateTimeWindow(block.timeRange)
            viewModelScope.launch {
                updatedChildren.forEach { child ->
                    fetchBlockDataQuietly(child, start, end)
                }
            }
        }
    }

    fun removeChildFromCombined(combinedId: String, childId: String) {
        val currentBlocks = _blocks.value.toMutableList()
        val index = currentBlocks.indexOfFirst { it.id == combinedId }
        if (index == -1) return

        val block = currentBlocks[index]
        if (block is CombinedChartBlock) {
            val childToRemove = block.childBlocks.find { it.id == childId } ?: return
            val remainingChildren = block.childBlocks.filter { it.id != childId }

            currentBlocks.removeAt(index)
            
            // Add the removed child back as a single block
            currentBlocks.add(childToRemove)

            if (remainingChildren.size == 1) {
                // If only one remains, convert the combination back to a single block
                currentBlocks.add(remainingChildren.first())
            } else if (remainingChildren.isNotEmpty()) {
                // Otherwise keep the combination
                currentBlocks.add(block.copy(childBlocks = remainingChildren))
            }

            _blocks.value = currentBlocks
            saveCurrentConfig()
        }
    }


    fun unmergeBlock(blockId: String) {
        val currentBlocks = _blocks.value.toMutableList()
        val index = currentBlocks.indexOfFirst { it.id == blockId }
        if (index == -1) return

        val block = currentBlocks[index]
        if (block is CombinedChartBlock) {
            currentBlocks.removeAt(index)
            // Add children back as single blocks
            currentBlocks.addAll(block.childBlocks)
            _blocks.value = currentBlocks
            saveCurrentConfig()
        }
    }

    fun renameBlock(blockId: String, newTitle: String) {
        val currentBlocks = _blocks.value.toMutableList()
        val index = currentBlocks.indexOfFirst { it.id == blockId }
        if (index == -1) return

        val block = currentBlocks[index]
        if (block is SingleChartBlock) {
            currentBlocks[index] = block.copy(title = newTitle)
        } else if (block is CombinedChartBlock) {
            currentBlocks[index] = block.copy(title = newTitle)
        }
        
        _blocks.value = currentBlocks
        saveCurrentConfig()
    }

    fun combineBlocks(blockId1: String, blockId2: String, newTitle: String) {
        val currentBlocks = _blocks.value.toMutableList()
        val index1 = currentBlocks.indexOfFirst { it.id == blockId1 }
        val index2 = currentBlocks.indexOfFirst { it.id == blockId2 }
        
        if (index1 == -1 || index2 == -1) return
        
        val block1 = currentBlocks[index1]
        val block2 = currentBlocks[index2]
        
        val childBlocks = mutableListOf<SingleChartBlock>()
        var timeRange = TimeRange.DAILY

        // Extract single blocks
        if (block1 is SingleChartBlock) {
            childBlocks.add(block1)
            timeRange = block1.timeRange
        } else if (block1 is CombinedChartBlock) {
            childBlocks.addAll(block1.childBlocks)
            timeRange = block1.timeRange
        }
        
        if (block2 is SingleChartBlock) {
            childBlocks.add(block2)
        } else if (block2 is CombinedChartBlock) {
            childBlocks.addAll(block2.childBlocks)
        }
        
        val newCombined = CombinedChartBlock(
            id = Clock.System.now().toEpochMilliseconds().toString(),
            title = if (block1 is CombinedChartBlock) block1.title else newTitle,
            timeRange = timeRange,
            combinationGroup = "Group1",
            childBlocks = childBlocks
        )
        
        val indicesToRemove = listOf(index1, index2).sortedDescending()
        indicesToRemove.forEach { currentBlocks.removeAt(it) }
        
        currentBlocks.add(newCombined)
        _blocks.value = currentBlocks
        saveCurrentConfig()

        // Fetch synchronized data for all children using a shared window
        val (start, end) = calculateTimeWindow(newCombined.timeRange)
        newCombined.childBlocks.forEach { child ->
            viewModelScope.launch {
                fetchBlockDataQuietly(child, start, end)
            }
        }
    }

    fun updateTimeRange(blockId: String, newRange: TimeRange) {
        val currentBlocks = _blocks.value.toMutableList()
        val index = currentBlocks.indexOfFirst { it.id == blockId }
        if (index == -1) return

        val block = currentBlocks[index]
        if (block is SingleChartBlock) {
            val updatedBlock = block.copy(timeRange = newRange, chartData = null)
            currentBlocks[index] = updatedBlock
            _blocks.value = currentBlocks
            saveCurrentConfig()

            viewModelScope.launch {
                fetchBlockDataQuietly(updatedBlock)
            }
        } else if (block is CombinedChartBlock) {
            val updatedChildren = block.childBlocks.map { it.copy(timeRange = newRange, chartData = null) }
            val updatedBlock = block.copy(timeRange = newRange, childBlocks = updatedChildren)
            currentBlocks[index] = updatedBlock
            _blocks.value = currentBlocks
            saveCurrentConfig()

            viewModelScope.launch {
                updatedChildren.forEach { fetchBlockDataQuietly(it) }
            }
        }
    }

    fun toggleInterventions(blockId: String) {
        val currentBlocks = _blocks.value.toMutableList()
        val index = currentBlocks.indexOfFirst { it.id == blockId }
        if (index == -1) return

        val block = currentBlocks[index]
        if (block is SingleChartBlock) {
            val updatedBlock = block.copy(showInterventions = !block.showInterventions, chartData = null)
            currentBlocks[index] = updatedBlock
            _blocks.value = currentBlocks
            saveCurrentConfig()

            viewModelScope.launch { fetchBlockDataQuietly(updatedBlock) }
        } else if (block is CombinedChartBlock) {
            val newState = !block.showInterventions
            val updatedChildren = block.childBlocks.map { it.copy(showInterventions = newState, chartData = null) }
            val updatedBlock = block.copy(showInterventions = newState, childBlocks = updatedChildren)
            currentBlocks[index] = updatedBlock
            _blocks.value = currentBlocks
            saveCurrentConfig()

            viewModelScope.launch {
                updatedChildren.forEach { fetchBlockDataQuietly(it) }
            }
        }
    }

    private suspend fun fetchInterventionOverlays(start: Long, end: Long): List<InterventionOverlay> {
        val result = sensorEventRepository.getOptimizedEventsForRange(start, end, sessionId = _selectedSessionId.value)
        return if (result is OptimizedDataResult.Raw) {
            result.data
                .filter { it.payload is SensorPayload.SystemIntervention }
                .map {
                    val intervention = it.payload as SensorPayload.SystemIntervention
                    InterventionOverlay(
                        timestamp = it.timestamp,
                        strategyId = intervention.strategyId,
                        aggressionLevel = intervention.aggressionLevel
                    )
                }
        } else {
            emptyList()
        }
    }

    /**
     * Fetches data and updates the state WITHOUT setting chartData to null first.
     * This ensures the UI updates smoothly without flashing a loading spinner.
     */
    private suspend fun fetchBlockDataQuietly(
        block: SingleChartBlock, 
        forcedStart: Long? = null, 
        forcedEnd: Long? = null
    ) {
        val (start, end) = if (forcedStart != null && forcedEnd != null) {
            Pair(forcedStart, forcedEnd)
        } else {
            calculateTimeWindow(block.timeRange)
        }

        // 1. Find the strategy for this block's data type
        val strategy = strategyMap[block.dataType]

        // 2. Let the strategy fetch and format the data passing the timeRange
        var data = strategy?.generateChartData(
            startTime = start,
            endTime = end,
            timeRange = block.timeRange,
            sensorId = block.sensorId,
            sessionId = _selectedSessionId.value
        )

        // 2b. Add Intervention Overlays if enabled and data is a Line chart
        if (block.showInterventions && data is ChartData.Line) {
            val overlays = fetchInterventionOverlays(start, end)
            data = data.copy(overlays = overlays)
        }

        // 3. Update the state
        val currentBlocks = _blocks.value.toMutableList()
        var updated = false

        // Search top-level
        val index = currentBlocks.indexOfFirst { it.id == block.id }
        if (index != -1) {
            currentBlocks[index] = (currentBlocks[index] as SingleChartBlock).copy(chartData = data)
            updated = true
        } else {
            // Search inside combined blocks
            for (i in currentBlocks.indices) {
                val b = currentBlocks[i]
                if (b is CombinedChartBlock) {
                    val childIndex = b.childBlocks.indexOfFirst { it.id == block.id }
                    if (childIndex != -1) {
                        val updatedChildren = b.childBlocks.toMutableList()
                        updatedChildren[childIndex] = updatedChildren[childIndex].copy(chartData = data)
                        currentBlocks[i] = b.copy(childBlocks = updatedChildren)
                        updated = true
                        break
                    }
                }
            }
        }

        if (updated) {
            _blocks.value = currentBlocks
        }
    }

    private fun calculateTimeWindow(range: TimeRange): Pair<Long, Long> {
        val now = Clock.System.now().toEpochMilliseconds()
        val sessionId = _selectedSessionId.value

        val (start, end) = if (sessionId != null) {
            val session = _sessions.value.find { it.id == sessionId }
            if (session != null) {
                Pair(session.startTime, session.endTime ?: now)
            } else {
                Pair(now - 86_400_000L, now)
            }
        } else {
            // All Data (Live + Archived) -> Look back at most 7 days
            Pair(now - 604_800_000L, now)
        }

        // Minimum range to plot (Hourly: 1h, Daily: 1d, Weekly: 7d)
        val minSpan = when (range) {
            TimeRange.HOURLY -> 3_600_000L
            TimeRange.DAILY -> 86_400_000L
            TimeRange.WEEKLY -> 604_800_000L
        }

        // Maximum range to plot per category for performance and readability bounds
        val maxSpan = when (range) {
            TimeRange.HOURLY -> 86_400_000L      // 24 hours max scrollback for minute-level buckets
            TimeRange.DAILY -> 604_800_000L      // 7 days max scrollback for hour-level buckets
            TimeRange.WEEKLY -> 2_592_000_000L   // 30 days max scrollback for day-level buckets
        }

        var adjustedStart = start
        val currentDuration = end - adjustedStart

        // 1. Zero-padding logic: pad start if session duration is shorter than minimum range
        if (currentDuration < minSpan) {
            adjustedStart = end - minSpan
        }

        // 2. Performance cap logic: limit maximum duration allowed
        if (end - adjustedStart > maxSpan) {
            adjustedStart = end - maxSpan
        }

        return Pair(adjustedStart, end)
    }
}