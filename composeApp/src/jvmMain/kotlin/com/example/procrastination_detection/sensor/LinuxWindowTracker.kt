package com.example.procrastination_detection.sensor

import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.model.WindowData
import com.example.procrastination_detection.domain.pipeline.EventPipeline
import com.example.procrastination_detection.domain.sensor.BehaviorSensor
import com.example.procrastination_detection.helpers.getActiveApp
import com.example.procrastination_detection.helpers.getMyAppProcessName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LinuxWindowTracker(
    private val eventPipeline: EventPipeline,
    private val scope: CoroutineScope
) : BehaviorSensor {

    private var trackingJob: Job? = null
    // Track process and title separately so we can distinguish the type of change
    private var lastProcessName: String = ""
    private var lastWindowTitle: String = ""

    override val id: String = "WINDOW_TRACKER"

    override fun start() {
        if (trackingJob?.isActive == true) return

        trackingJob = scope.launch(Dispatchers.IO) {
            val myAppName = getMyAppProcessName()
            while (isActive) {
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
                                eventPipeline.emitRawEvent(SensorPayload.AppSwitch(windowData))
                                println("Tracker [AppSwitch]: $currentClass — $currentTitle")
                            }
                            // Case 2: Same app, but the title changed (e.g., new file in IDE, new tab in browser)
                            currentTitle != lastWindowTitle -> {
                                lastWindowTitle = currentTitle
                                eventPipeline.emitRawEvent(SensorPayload.TitleChange(windowData))
                                println("Tracker [TitleChange]: $currentClass — $currentTitle")
                            }
                            // Case 3: Nothing changed — no emission
                        }
                    }
                }

                delay(2000)
            }
        }
    }

    override fun stop() {
        trackingJob?.cancel()
        lastProcessName = ""
        lastWindowTitle = ""
    }
}