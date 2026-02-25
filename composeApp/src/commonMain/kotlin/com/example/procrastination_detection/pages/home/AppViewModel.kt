package com.example.procrastination_detection.pages.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.procrastination_detection.engine.TrackingEngine
import com.example.procrastination_detection.helpers.getActiveApp
import com.example.procrastination_detection.helpers.getActiveGuiApps
import com.example.procrastination_detection.globals.DefaultRules
import com.example.procrastination_detection.helpers.ProcrastinationEvaluator
import com.example.procrastination_detection.interfaces.AppRepository
import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.MonitoredProcess
import com.example.procrastination_detection.models.db.Process
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

class AppListViewModel(private val repository: AppRepository, private val trackingEngine: TrackingEngine) : ViewModel() {
    val monitoredProcesses = repository.monitoredProcesses

    val currentActiveApp = trackingEngine.currentActiveApp
    val isProcrastinating = trackingEngine.isProcrastinating
    val isTracking = trackingEngine.isTracking
    val consecutiveSeconds = trackingEngine.consecutiveSeconds


    val defaultRule = DefaultRules.getRules()
    val interval = 1L

    fun toggleTracking(){
        trackingEngine.toggleTracking(interval, defaultRule.first())
    }
}