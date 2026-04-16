package com.example.procrastination_detection.engine

// commonMain
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.pipeline.EventPipeline
import com.example.procrastination_detection.helpers.BrowserAnalyserConfig
import com.example.procrastination_detection.helpers.LocalUrlExtractor
import com.example.procrastination_detection.helpers.takeScreenshot
import com.example.procrastination_detection.domain.sensor.BehaviorSensor
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

class BrowserAnalyserEngine(
    private val urlExtractor: LocalUrlExtractor,
    private val pipeline: EventPipeline,
    private val config: BrowserAnalyserConfig = BrowserAnalyserConfig(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : BehaviorSensor {
    override val id: String = "BROWSER_ANALYSER_SENSOR"
    private var trackingJob: Job? = null
    private val mutex = Mutex()

    /**
     * Call this from your existing application tracking system.
     */
    fun onApplicationForegrounded(category: String) {
        if (category.equals("browser", ignoreCase = true) && config.isEnabled) {
            start()
        } else {
            stop()
        }
    }

    override fun start() {
        scope.launch {
            mutex.withLock {
                // If it's already running, don't start a duplicate loop
                if (trackingJob?.isActive == true) return@withLock

                println("BrowserAnalyserEngine: 🟢 Browser detected. Starting analysis loop...")

                trackingJob = scope.launch {
                    trackingLoop()
                }
            }
        }
    }

    override fun stop() {
        scope.launch {
            mutex.withLock {
                if (trackingJob?.isActive == true) {
                    println("BrowserAnalyserEngine: 🔴 Browser left foreground. Stopping analysis...")
                    trackingJob?.cancel()
                    trackingJob = null
                }
            }
        }
    }

    private suspend fun trackingLoop() {
        // isActive checks if the coroutine has been cancelled (e.g., stopTracking was called)
        while (currentCoroutineContext().isActive) {
            try {
                val screenshotBytes = takeScreenshot()

                if (screenshotBytes != null) {
                    // Pull the current window title from the last processed event so the extractor
                    // can use it for domain cross-referencing in the fallback chain.
                    val currentTitle = pipeline.currentState.value?.payload?.let { payload ->
                        when (payload) {
                            is SensorPayload.AppSwitch  -> payload.windowData.windowTitle
                            is SensorPayload.TitleChange -> payload.windowData.windowTitle
                            else -> null
                        }
                    }

                    val url = urlExtractor.extractUrlFromImage(screenshotBytes, currentTitle)
                    if (url != null) {
                        println("BrowserAnalyserEngine: 🌐 Found URL -> $url")
                        // Feed the URL directly into the shared event pipeline as a first-class event
                        pipeline.emitRawEvent(
                            SensorPayload.BrowserOCRContext(
                                url = url,
                                windowTitle = currentTitle ?: url
                            )
                        )
                    } else {
                        println("BrowserAnalyserEngine: 👁️ No URL visible on screen.")
                    }
                } else {
                    println("BrowserAnalyserEngine: ⚠️ Failed to capture screenshot.")
                }
            } catch (e: CancellationException) {
                // Expected behavior when the job is cancelled. Just rethrow to exit the coroutine cleanly.
                throw e
            } catch (e: Exception) {
                // Catch any other exceptions (like out of memory or OCR crashing) so the loop doesn't die entirely
                println("BrowserAnalyserEngine: ❌ Error during analysis cycle: ${e.message}")
            }

            // Wait for the configured interval before taking the next screenshot
            delay(config.captureIntervalMs)
        }
    }
}