package com.example.procrastination_detection.ui.dashboard

import androidx.lifecycle.ViewModel
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.model.Category
import com.example.procrastination_detection.domain.pipeline.EventPipeline
import com.example.procrastination_detection.domain.sensor.SensorManager
import com.example.procrastination_detection.domain.sensor.SensorType
import com.example.procrastination_detection.domain.sensor.SensorTelemetry
import com.example.procrastination_detection.domain.sensor.TelemetryManager
import com.example.procrastination_detection.engine.BrowserAnalyserEngine
import com.example.procrastination_detection.domain.repository.FocusProfileRepository
import kotlinx.coroutines.flow.*

class DashboardViewModel(
    private val pipeline: EventPipeline,
    private val sensorManager: SensorManager,
    private val browserAnalyserEngine: BrowserAnalyserEngine,
    private val focusProfileRepository: FocusProfileRepository,
    private val telemetryManager: TelemetryManager
) : ViewModel() {

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

    fun startTracking(){
        sensorManager.startAllActiveSensors()
    }

    fun stopTracking(){
        sensorManager.stopAll()
        pipeline.resetTriggers()
    }
}