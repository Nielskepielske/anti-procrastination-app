package com.example.procrastination_detection.domain.event

import com.example.procrastination_detection.domain.model.WindowData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SensorPayload {

    @Serializable
    @SerialName("app_switch")
    data class AppSwitch(
        val windowData: WindowData
    ) : SensorPayload()

    /**
     * Emitted when the active window's title changes but the process name stays the same.
     * Example: switching from Main.kt to Utils.kt inside IntelliJ (same process, different file).
     *
     * This is kept separate from AppSwitch so that:
     * - AnalyticsTimerEngine can ignore intra-app transitions and not fragment durations.
     * - BehaviorAnalysisEngine can watch rapid TitleChanges on browser processes to detect tab-hopping.
     * - The DictionaryEngine still evaluates the new title for categorisation (e.g., browser URL changes).
     */
    @Serializable
    @SerialName("title_change")
    data class TitleChange(
        val windowData: WindowData
    ) : SensorPayload()

    @Serializable
    @SerialName("browser_ocr_context")
    data class BrowserOCRContext(
        val url: String,
        val windowTitle: String
    ) : SensorPayload()

    @Serializable
    @SerialName("mouse_metrics")
    data class MouseMetrics(
        val hoverDurationMillis: Long,
        val clicks: Int,
        val distanceTraveled: Double
    ) : SensorPayload()
    
    @Serializable
    @SerialName("keyboard_metrics")
    data class KeyboardMetrics(
        val cadenceWpm: Int
    ) : SensorPayload()
}