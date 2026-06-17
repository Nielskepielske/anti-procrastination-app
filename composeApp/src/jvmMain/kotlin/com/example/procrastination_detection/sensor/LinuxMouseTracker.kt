package com.example.procrastination_detection.sensor

import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.pipeline.EventPipeline
import com.example.procrastination_detection.domain.sensor.BasePollingSensor
import com.example.procrastination_detection.domain.sensor.SensorType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class LinuxMouseTracker(
    private val eventPipeline: EventPipeline,
    scope: CoroutineScope
) : BasePollingSensor(scope, intervalMs = 1000L) {

    override val type: SensorType = SensorType.MOUSE_TRACKER

    private var lastX: Double = -1.0
    private var lastY: Double = -1.0
    private var currentHoverDurationMillis: Long = 0L

    override suspend fun onStart() {
        lastX = -1.0
        lastY = -1.0
        currentHoverDurationMillis = 0L
    }

    override suspend fun pollData() {
        val (x, y) = getMouseCoordinates() ?: return

        // Initialization
        if (lastX == -1.0 && lastY == -1.0) {
            lastX = x
            lastY = y
            return
        }

        val distance = sqrt((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY))
        
        if (distance == 0.0) {
            currentHoverDurationMillis += 1000L
        } else {
            currentHoverDurationMillis = 0L
        }

        lastX = x
        lastY = y

        eventPipeline.emitRawEvent(
            SensorPayload.MouseMetrics(
                hoverDurationMillis = currentHoverDurationMillis,
                clicks = 0, // Clicks not currently supported in global polling
                distanceTraveled = distance,
                sensorId = type.name
            )
        )
    }

    override fun onStop() {
        lastX = -1.0
        lastY = -1.0
        currentHoverDurationMillis = 0L
    }

    private suspend fun getMouseCoordinates(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("hyprctl", "cursorpos")
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            // Output looks like: "1369, 2490"
            val parts = output.split(",")
            if (parts.size == 2) {
                val x = parts[0].trim().toDoubleOrNull()
                val y = parts[1].trim().toDoubleOrNull()
                if (x != null && y != null) {
                    return@withContext Pair(x, y)
                }
            }
        } catch (e: Exception) {
            println("LinuxMouseTracker: Failed to get cursor position: ${e.message}")
        }
        null
    }
}
