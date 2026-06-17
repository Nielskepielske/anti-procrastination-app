package com.example.procrastination_detection.domain.sensor

import kotlinx.coroutines.*

abstract class BasePollingSensor(
    private val scope: CoroutineScope,
    private val intervalMs: Long = 2000L
) : BehaviorSensor {

    private var trackingJob: Job? = null

    override fun start() {
        if (trackingJob?.isActive == true) return

        trackingJob = scope.launch(Dispatchers.IO) {
            onStart()
            while (isActive) {
                try {
                    pollData()
                } catch (e: Exception) {
                    println("Sensor [${type.name}] encountered an error during poll: ${e.message}")
                    // Optionally log to an analytics service or event bus
                }
                delay(intervalMs)
            }
        }
    }

    override fun stop() {
        trackingJob?.cancel()
        onStop()
    }

    /** Called once before polling begins. */
    protected open suspend fun onStart() {}
    
    /** Called repeatedly every [intervalMs] milliseconds. Implement your sensor logic here. */
    protected abstract suspend fun pollData()
    
    /** Called when the sensor is stopped. Clean up resources here. */
    protected open fun onStop() {}
}
