package com.example.procrastination_detection.sensor

import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.model.WindowData
import com.example.procrastination_detection.domain.pipeline.EventPipeline
import com.example.procrastination_detection.domain.sensor.BasePollingSensor
import com.example.procrastination_detection.domain.sensor.SensorType
import com.example.procrastination_detection.helpers.getActiveApp
import com.example.procrastination_detection.helpers.getMyAppProcessName
import kotlinx.coroutines.CoroutineScope

class LinuxWindowTracker(
    private val eventPipeline: EventPipeline,
    scope: CoroutineScope
) : BasePollingSensor(scope, intervalMs = 2000L) {

    // Track process and title separately so we can distinguish the type of change
    private var lastProcessName: String = ""
    private var lastWindowTitle: String = ""
    private var myAppName: String = ""

    override val type: SensorType = SensorType.WINDOW_TRACKER

    override suspend fun onStart() {
        myAppName = getMyAppProcessName()
    }

    override suspend fun pollData() {
        val activeApp = getActiveApp()

        if (activeApp != null) {
            val currentClass = activeApp.className
            val currentTitle = activeApp.title

            // Do not track our own app wrapper to prevent recursion loops
            if (!currentClass.contains(myAppName)) {
                val windowData = WindowData(
                    processName = currentClass,
                    windowTitle = currentTitle
                )

                when {
                    // Case 1: The user switched to a different application entirely
                    currentClass != lastProcessName -> {
                        lastProcessName = currentClass
                        lastWindowTitle = currentTitle
                        eventPipeline.emitRawEvent(SensorPayload.AppSwitch(windowData, sensorId = type.name))
                        println("Tracker [AppSwitch]: ${currentClass} — ${currentTitle}")
                    }
                    // Case 2: Same app, but the title changed (e.g., new file in IDE, new tab in browser)
                    currentTitle != lastWindowTitle -> {
                        lastWindowTitle = currentTitle
                        eventPipeline.emitRawEvent(SensorPayload.TitleChange(windowData, sensorId = type.name))
                        println("Tracker [TitleChange]: ${currentClass} — ${currentTitle}")
                    }
                    // Case 3: Nothing changed — no emission
                }
            }
        }
    }

    override fun onStop() {
        lastProcessName = ""
        lastWindowTitle = ""
    }
}