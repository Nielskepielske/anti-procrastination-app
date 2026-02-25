package com.example.procrastination_detection.repositories

import com.example.procrastination_detection.interfaces.AppRepository
import com.example.procrastination_detection.models.db.MonitoredProcess
import com.example.procrastination_detection.models.db.Rule
import com.example.procrastination_detection.models.db.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TempAppRepository : AppRepository {
    // Stores our active processes in memory
    private val _monitoredApps = MutableStateFlow<Map<String, MonitoredProcess>>(emptyMap())

    // Converts the Map into a List for the UI to observe
    override val monitoredProcesses: Flow<List<MonitoredProcess>> =
        _monitoredApps.map { it.values.toList() }

    private var activeSession: Session? = null

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getOrCreateSession(name: String, defaultRule: Rule): Session {
        if (activeSession != null) return activeSession!!

        val newSession = Session(
            id = Uuid.random().toString(),
            name = name,
            rule = defaultRule,
            processes = emptyList()
        )
        activeSession = newSession
        return newSession
    }

    override suspend fun getMonitoredProcess(appName: String, sessionId: String): MonitoredProcess? {
        // We look it up by appName in our temporary map
        return _monitoredApps.value[appName]
    }

    override suspend fun saveMonitoredProcess(process: MonitoredProcess) {
        _monitoredApps.update { currentMap ->
            val updatedMap = currentMap.toMutableMap()
            // We use the app name as the key so it easily overwrites the old version
            updatedMap[process.process.name] = process
            updatedMap
        }
    }
}