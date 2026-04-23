package com.example.procrastination_detection.ui.analytics.strategy

import com.example.procrastination_detection.domain.model.analytics.ChartData

interface DashboardDataStrategy {
    /** The unique ID linking this strategy to a DashboardBlock (e.g., "switch_frequency") */
    val dataTypeId: String

    /** * Fetches the data from the repository and formats it into the agnostic ChartData.
     * Returns null if there's no data for this period.
     */
    suspend fun generateChartData(startTime: Long, endTime: Long): ChartData?
}