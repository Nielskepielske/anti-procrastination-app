package com.example.procrastination_detection.ui.analytics.strategy

import com.example.procrastination_detection.domain.model.analytics.ChartData
import kotlin.reflect.KClass

interface DashboardDataStrategy {
    /** The unique ID linking this strategy to a DashboardBlock (e.g., "switch_frequency") */
    val dataTypeId: String

    /** Human-readable display name for the UI picker */
    val displayName: String

    /**
     * The Kotlin class of ChartData this strategy produces.
     * Used to enforce that only same-type blocks can be merged (e.g. Line + Line).
     */
    val chartType: KClass<out ChartData>

    /**
     * The set of sensor event type strings this strategy is compatible with (e.g., "APP_SWITCH").
     * null means the strategy works with ALL sensors / event types.
     */
    val compatibleEventTypes: Set<String>?

    /** Fetches the data from the repository and formats it into the agnostic ChartData.
     * Returns null if there's no data for this period.
     * @param sensorId If non-null, only data from this sensor is considered.
     */
    suspend fun generateChartData(startTime: Long, endTime: Long, sensorId: String? = null): ChartData?
}