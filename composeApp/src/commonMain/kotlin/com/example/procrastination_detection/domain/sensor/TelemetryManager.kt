package com.example.procrastination_detection.domain.sensor

import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.pipeline.EventPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SensorTelemetry(
    val currentApp: String = "Waiting for activity...",
    val currentTitle: String = "Waiting...",
    val currentUrl: String = "",
    val mouseDistanceTraveled: Double = 0.0,
    val mouseHoverDurationMs: Long = 0L,
    val isMouseIdle: Boolean = true,
    val keyboardCadenceWpm: Int = 0,
    val totalEventsProcessed: Int = 0
)

class TelemetryManager(
    private val pipeline: EventPipeline,
    private val sensorManager: SensorManager,
    private val scope: CoroutineScope
) {
    private val _liveTelemetry = MutableStateFlow(SensorTelemetry())
    val liveTelemetryFlow: StateFlow<SensorTelemetry> = _liveTelemetry.asStateFlow()

    init {
        // Reset telemetry when tracking stops
        scope.launch {
            sensorManager.isTrackingFlow.collect { isTracking ->
                if (!isTracking) {
                    _liveTelemetry.value = SensorTelemetry()
                }
            }
        }

        // Aggregate telemetry from processed events
        scope.launch {
            pipeline.processedEvents.collect { event ->
                if (!sensorManager.isTrackingFlow.value) return@collect

                _liveTelemetry.update { current ->
                    val nextEvents = current.totalEventsProcessed + 1
                    
                    when (val payload = event.payload) {
                        is SensorPayload.AppSwitch -> {
                            current.copy(
                                currentApp = payload.windowData.processName,
                                currentTitle = payload.windowData.windowTitle,
                                totalEventsProcessed = nextEvents
                            )
                        }
                        is SensorPayload.TitleChange -> {
                            current.copy(
                                currentApp = payload.windowData.processName,
                                currentTitle = payload.windowData.windowTitle,
                                totalEventsProcessed = nextEvents
                            )
                        }
                        is SensorPayload.BrowserOCRContext -> {
                            current.copy(
                                currentUrl = payload.url,
                                currentTitle = payload.windowTitle,
                                totalEventsProcessed = nextEvents
                            )
                        }
                        is SensorPayload.MouseMetrics -> {
                            current.copy(
                                mouseDistanceTraveled = current.mouseDistanceTraveled + payload.distanceTraveled,
                                mouseHoverDurationMs = payload.hoverDurationMillis,
                                isMouseIdle = payload.hoverDurationMillis > 0,
                                totalEventsProcessed = nextEvents
                            )
                        }
                        is SensorPayload.KeyboardMetrics -> {
                            current.copy(
                                keyboardCadenceWpm = payload.cadenceWpm,
                                totalEventsProcessed = nextEvents
                            )
                        }
                        is SensorPayload.SystemIntervention -> {
                            current.copy(
                                totalEventsProcessed = nextEvents
                            )
                        }
                        is SensorPayload.AggressionHeat -> {
                            current.copy(
                                totalEventsProcessed = nextEvents
                            )
                        }
                    }
                }
            }
        }
    }
}
