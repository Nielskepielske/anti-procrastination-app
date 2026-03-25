package com.example.procrastination_detection.engine

// commonMain
import com.example.procrastination_detection.helpers.BrowserAnalyserConfig
import com.example.procrastination_detection.helpers.LocalUrlExtractor
import com.example.procrastination_detection.helpers.takeScreenshot
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

class BrowserAnalyserEngine(
    private val urlExtractor: LocalUrlExtractor,
    private val config: BrowserAnalyserConfig = BrowserAnalyserConfig(),
    // Use Dispatchers.Default for heavy CPU work (like OCR), and a SupervisorJob so child failures don't kill the scope
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private var trackingJob: Job? = null
    private val mutex = Mutex()

    /**
     * Call this from your existing application tracking system.
     */
    suspend fun onApplicationForegrounded(category: String) {
        if (category.equals("browser", ignoreCase = true) && config.isEnabled) {
            startTracking()
        } else {
            stopTracking()
        }
    }

    private suspend fun startTracking() = mutex.withLock {
        // If it's already running, don't start a duplicate loop
        if (trackingJob?.isActive == true) return@withLock

        println("BrowserAnalyserEngine: 🟢 Browser detected. Starting analysis loop...")

        trackingJob = scope.launch {
            trackingLoop()
        }
    }

    private suspend fun stopTracking() = mutex.withLock {
        if (trackingJob?.isActive == true) {
            println("BrowserAnalyserEngine: 🔴 Browser left foreground. Stopping analysis...")
            trackingJob?.cancel()
            trackingJob = null
        }
    }

    private suspend fun trackingLoop() {
        // isActive checks if the coroutine has been cancelled (e.g., stopTracking was called)
        while (currentCoroutineContext().isActive) {
            try {
                val screenshotBytes = takeScreenshot()

                if (screenshotBytes != null) {
                    val url = urlExtractor.extractUrlFromImage(screenshotBytes)
                    if (url != null) {
                        println("BrowserAnalyserEngine: 🌐 Found URL -> $url")
                        // TODO: Save to local database or send to procrastination detection logic
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