package com.example.procrastination_detection.helpers

// commonMain
data class BrowserAnalyserConfig(
    val captureIntervalMs: Long = 5000L, // Default: 5 seconds
    val isEnabled: Boolean = true
    // You can add more options later, like:
    // val saveFailedScreenshotsForDebugging: Boolean = false
)

// The expect function that each platform will fulfill
expect suspend fun takeScreenshot(): ByteArray?