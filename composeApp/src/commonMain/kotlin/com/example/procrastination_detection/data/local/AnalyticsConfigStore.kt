package com.example.procrastination_detection.data.local

import com.example.procrastination_detection.domain.model.analytics.DashboardBlockConfig
import kotlinx.coroutines.flow.Flow

expect class AnalyticsConfigStore() {
    val configFlow: Flow<List<DashboardBlockConfig>>
    suspend fun saveConfig(config: List<DashboardBlockConfig>)
}
