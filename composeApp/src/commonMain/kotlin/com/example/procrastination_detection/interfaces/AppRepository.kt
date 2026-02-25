package com.example.procrastination_detection.interfaces

import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.MonitoredProcess
import com.example.procrastination_detection.models.db.Process
import com.example.procrastination_detection.models.db.Rule
import com.example.procrastination_detection.models.db.Session
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    val monitoredProcesses: Flow<List<MonitoredProcess>>
    val allProcesses: Flow<List<Process>>
    val allCategories: Flow<List<Category>>

    suspend fun getOrCreateSession(name: String, defaultRule: Rule): Session

    suspend fun getMonitoredProcess(appName: String, sessionId: String): MonitoredProcess?

    suspend fun saveMonitoredProcess(process: MonitoredProcess)

    suspend fun updateProcessCategory(process: Process, category: Category)
    suspend fun createCategory(name: String, isProductive: Boolean): Boolean
    suspend fun resetConsecutiveTimeForOtherApps(sessionId: String, activeAppName: String)
}