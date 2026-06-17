package com.example.procrastination_detection.ui.dashboard

import androidx.lifecycle.ViewModel
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.model.Category
import com.example.procrastination_detection.domain.pipeline.EventPipeline
import com.example.procrastination_detection.domain.sensor.SensorManager
import com.example.procrastination_detection.domain.session.SessionManager
import com.example.procrastination_detection.domain.sensor.SensorType
import com.example.procrastination_detection.domain.sensor.SensorTelemetry
import com.example.procrastination_detection.domain.sensor.TelemetryManager
import com.example.procrastination_detection.engine.BrowserAnalyserEngine
import com.example.procrastination_detection.domain.repository.FocusProfileRepository
import kotlinx.coroutines.flow.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.procrastination_detection.domain.model.EscalationLevel

class DashboardViewModel(
    private val pipeline: EventPipeline,
    private val sessionManager: SessionManager,
    private val sensorManager: SensorManager,
    private val browserAnalyserEngine: BrowserAnalyserEngine,
    private val focusProfileRepository: FocusProfileRepository,
    private val telemetryManager: TelemetryManager
) : ViewModel() {

    private val _aggressionHeat = MutableStateFlow(0)
    val aggressionHeatFlow: StateFlow<Int> = _aggressionHeat.asStateFlow()

    val escalationLevelFlow: StateFlow<EscalationLevel> = _aggressionHeat
        .map { score ->
            when {
                score < 3 -> EscalationLevel.GENTLE
                score < 6 -> EscalationLevel.FIRM
                else -> EscalationLevel.AGGRESSIVE
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EscalationLevel.GENTLE)

    init {
        viewModelScope.launch {
            pipeline.processedEvents.collect { event ->
                val payload = event.payload
                if (payload is SensorPayload.AggressionHeat) {
                    _aggressionHeat.value = payload.score
                }
            }
        }
    }

    /** Human-readable label for the currently active window context. */
    val currentAppFlow = pipeline.currentState.map { event ->
        when (val payload = event?.payload) {
            is SensorPayload.AppSwitch   -> payload.windowData.windowTitle
            is SensorPayload.TitleChange -> payload.windowData.windowTitle
            is SensorPayload.BrowserOCRContext -> payload.url
            else -> "Waiting for activity..."
        }
    }

    val isTrackingFlow = sensorManager.isTrackingFlow

    /** The category string determined by the DictionaryEngine for the current event. */
    val currentCategoryFlow = pipeline.currentState.map { event ->
        event?.category ?: Category.UNCATEGORIZED
    }

    val activeProfileNameFlow: Flow<String> = focusProfileRepository.activeProfileFlow.map { 
        it?.name ?: "No Profile" 
    }

    val activeSensorsFlow: Flow<List<SensorType>> = combine(
        sensorManager.activeSensorsFlow,
        browserAnalyserEngine.isActiveFlow
    ) { standardActive, browserActive ->
        if (browserActive) {
            standardActive + SensorType.BROWSER_ANALYSER_SENSOR
        } else {
            standardActive
        }
    }

    val liveTelemetryFlow: StateFlow<SensorTelemetry> = telemetryManager.liveTelemetryFlow

    val activeSessionFlow = sessionManager.activeSessionFlow
    val orphanedSessionFlow = sessionManager.orphanedSessionFlow

    fun continueOrphanedSession() {
        viewModelScope.launch {
            sessionManager.continueOrphanedSession()
        }
    }

    fun finishOrphanedSession() {
        viewModelScope.launch {
            sessionManager.finishOrphanedSession()
        }
    }

    fun startTracking(){
        viewModelScope.launch {
            sessionManager.startSession()
        }
    }

    fun stopTracking(){
        viewModelScope.launch {
            sessionManager.stopSession()
            pipeline.resetTriggers()
        }
    }

    fun pauseTracking() {
        viewModelScope.launch {
            sessionManager.pauseSession()
        }
    }

    fun resumeTracking() {
        viewModelScope.launch {
            sessionManager.resumeSession()
        }
    }
}