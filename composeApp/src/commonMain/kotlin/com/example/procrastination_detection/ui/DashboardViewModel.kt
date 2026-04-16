package com.example.procrastination_detection.ui

import androidx.lifecycle.ViewModel
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.model.Category
import com.example.procrastination_detection.domain.pipeline.EventPipeline
import com.example.procrastination_detection.domain.sensor.SensorManager
import kotlinx.coroutines.flow.map

class DashboardViewModel(
    private val pipeline: EventPipeline,
    private val sensorManager: SensorManager,
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

    fun startTracking(){
        sensorManager.startAllActiveSensors()
    }

    fun stopTracking(){
        sensorManager.stopAll()
    }
}