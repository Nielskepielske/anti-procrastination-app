package com.example.procrastination_detection.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.procrastination_detection.data.local.AppUsageDao
import com.example.procrastination_detection.data.local.AppUsageEntity
import com.example.procrastination_detection.data.local.HourlyCount
import com.example.procrastination_detection.data.local.SensorEventDao
import com.example.procrastination_detection.domain.dictionary.DictionaryEngine
import com.example.procrastination_detection.domain.model.Category
import com.example.procrastination_detection.domain.model.WindowData
import kotlinx.coroutines.flow.*

data class UsageRow(
    val processName: String,
    val windowTitle: String,
    val totalSeconds: Long,
    val category: Category
)

data class DaySummary(
    val dayIndex: Long,
    val productiveSeconds: Long,
    val distractingSeconds: Long,
    val otherSeconds: Long
)

class AnalyticsViewModel(
    private val appUsageDao: AppUsageDao,
    private val sensorEventDao: SensorEventDao,
    private val dictionaryEngine: DictionaryEngine
) : ViewModel() {

    private val todayIndex = getTodayIndex()

    val todayBreakdown: StateFlow<List<UsageRow>> = appUsageDao.getUsageForDate(todayIndex)
        .map { entities ->
            entities.map { entity ->
                UsageRow(
                    processName = entity.processName,
                    windowTitle = entity.windowTitle,
                    totalSeconds = entity.totalSeconds,
                    category = dictionaryEngine.categorize(WindowData(entity.processName, entity.windowTitle)).category
                )
            }.sortedByDescending { it.totalSeconds }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusScore: StateFlow<Int> = todayBreakdown.map { rows ->
        val productive = rows.filter { it.category == Category.PRODUCTIVE }.sumOf { it.totalSeconds }
        val distracting = rows.filter { it.category == Category.DISTRACTING }.sumOf { it.totalSeconds }
        if (productive + distracting == 0L) 0
        else (productive * 100 / (productive + distracting)).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val appSwitchesPerHour: StateFlow<List<HourlyCount>> = flow {
        val startOfDay = todayIndex * 86_400_000L
        val endOfDay = startOfDay + 86_399_999L
        emit(sensorEventDao.getEventCountsPerHour("APP_SWITCH", startOfDay, endOfDay))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weekSummary: StateFlow<List<DaySummary>> = appUsageDao.getUsageForRange(todayIndex - 6, todayIndex)
        .map { entities ->
            entities.groupBy { it.dayIndex }.map { (dayIndex, dayEntities) ->
                val productive = dayEntities.sumOf { 
                    if (dictionaryEngine.categorize(WindowData(it.processName, it.windowTitle)).category == Category.PRODUCTIVE) it.totalSeconds else 0L
                }
                val distracting = dayEntities.sumOf { 
                    if (dictionaryEngine.categorize(WindowData(it.processName, it.windowTitle)).category == Category.DISTRACTING) it.totalSeconds else 0L
                }
                val total = dayEntities.sumOf { it.totalSeconds }
                DaySummary(dayIndex, productive, distracting, total - productive - distracting)
            }.sortedBy { it.dayIndex }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getTodayIndex(): Long = System.currentTimeMillis() / 86_400_000L
}
