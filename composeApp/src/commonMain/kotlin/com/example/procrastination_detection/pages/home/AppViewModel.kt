package com.example.procrastination_detection.pages.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.procrastination_detection.engine.TrackingEngine
import com.example.procrastination_detection.helpers.getActiveApp
import com.example.procrastination_detection.helpers.getActiveGuiApps
import com.example.procrastination_detection.globals.DefaultRules
import com.example.procrastination_detection.helpers.ProcrastinationEvaluator
import com.example.procrastination_detection.interfaces.AppRepository
import com.example.procrastination_detection.interfaces.ConfigRepository
import com.example.procrastination_detection.interfaces.SessionRepository
import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.MonitoredProcess
import com.example.procrastination_detection.models.db.Process
import com.example.procrastination_detection.models.db.Rule
import com.example.procrastination_detection.models.db.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.collections.emptyList
import kotlin.collections.get
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AppListViewModel(
    private val sessionRepository: SessionRepository,
    private val configRepository: ConfigRepository,
    val trackingEngine: TrackingEngine,
) : ViewModel() {
    val monitoredProcesses = sessionRepository.monitoredProcesses

    val currentActiveApp = trackingEngine.currentActiveApp
    val isProcrastinating = trackingEngine.isProcrastinating
    val isTracking = trackingEngine.isTracking
    val consecutiveSeconds = trackingEngine.consecutiveSeconds

    val availableRules = configRepository.allRules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 2. Hold the currently selected rule
    var selectedRule by mutableStateOf<Rule?>(null)
        private set


    val defaultRule = DefaultRules.getRules()
    val interval = 1L

    init {
        viewModelScope.launch {
            availableRules.collect { rules ->
                if (selectedRule == null && rules.isNotEmpty()) {
                    selectedRule = rules.first()
                }
            }
        }
    }

    // 4. The Action: When the user picks a new rule from the dropdown
    fun onRuleSelected(rule: Rule) {
        selectedRule = rule

        val activeSessionId = trackingEngine.currentSessionId.value
        if (activeSessionId != null && isTracking.value) {
            viewModelScope.launch {
                sessionRepository.updateSessionRule(activeSessionId, rule.id)
            }
        }
    }

    // 5. Update your start tracking function to use the selected rule!
    fun toggleTracking() {
        selectedRule?.let { rule ->
            trackingEngine.toggleTracking(interval = interval, defaultRule = rule)
        }
    }
}