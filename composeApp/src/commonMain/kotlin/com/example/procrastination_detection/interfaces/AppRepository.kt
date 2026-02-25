package com.example.procrastination_detection.interfaces

import com.example.procrastination_detection.models.db.MonitoredProcess
import com.example.procrastination_detection.models.db.Rule
import com.example.procrastination_detection.models.db.Session
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    val monitoredProcesses: Flow<List<MonitoredProcess>>

    suspend fun getOrCreateSession(name: String, defaultRule: Rule): Session

    suspend fun getMonitoredProcess(appName: String, sessionId: String): MonitoredProcess?

    suspend fun saveMonitoredProcess(process: MonitoredProcess)
}