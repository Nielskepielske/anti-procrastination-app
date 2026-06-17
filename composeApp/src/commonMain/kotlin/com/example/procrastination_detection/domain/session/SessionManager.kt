package com.example.procrastination_detection.domain.session

import com.example.procrastination_detection.data.local.dao.SessionDao
import com.example.procrastination_detection.data.local.entity.SessionEntity
import com.example.procrastination_detection.domain.model.FocusProfile
import com.example.procrastination_detection.domain.pipeline.CsvExportEngine
import com.example.procrastination_detection.domain.repository.FocusProfileRepository
import com.example.procrastination_detection.domain.sensor.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Clock

class SessionManager(
    private val sessionDao: SessionDao,
    private val sensorManager: SensorManager,
    private val focusProfileRepository: FocusProfileRepository,
    private val csvExportEngine: CsvExportEngine,
    private val scope: CoroutineScope
) {
    private val _activeSessionFlow = MutableStateFlow<SessionEntity?>(null)
    val activeSessionFlow: StateFlow<SessionEntity?> = _activeSessionFlow.asStateFlow()

    private val _orphanedSessionFlow = MutableStateFlow<SessionEntity?>(null)
    val orphanedSessionFlow: StateFlow<SessionEntity?> = _orphanedSessionFlow.asStateFlow()

    init {
        scope.launch {
            // Check for orphaned session on startup
            val orphaned = sessionDao.getActiveSession()
            if (orphaned != null) {
                _orphanedSessionFlow.value = orphaned
            }
        }
    }

    suspend fun startSession() {
        if (_activeSessionFlow.value != null) return

        val profile = focusProfileRepository.activeProfileFlow.first()
        val granularity = profile?.csvGranularity?.name ?: "RAW"
        
        val newSession = SessionEntity(
            id = "sess_${Clock.System.now().toEpochMilliseconds()}",
            startTime = Clock.System.now().toEpochMilliseconds(),
            endTime = null,
            status = "ACTIVE",
            csvFilePath = null,
            granularity = granularity
        )
        
        sessionDao.insertSession(newSession)
        _activeSessionFlow.value = newSession
        sensorManager.startAllActiveSensors()
    }

    suspend fun stopSession() {
        val currentSession = _activeSessionFlow.value ?: return
        
        sensorManager.stopAll()
        
        val completedSession = currentSession.copy(
            endTime = Clock.System.now().toEpochMilliseconds(),
            status = "COMPLETED"
        )
        sessionDao.updateSession(completedSession)
        _activeSessionFlow.value = null
        
        // Trigger background export
        csvExportEngine.exportSession(completedSession)
    }

    suspend fun pauseSession() {
        val currentSession = _activeSessionFlow.value ?: return
        if (currentSession.status == "PAUSED") return
        
        sensorManager.stopAll()
        val pausedSession = currentSession.copy(status = "PAUSED")
        sessionDao.updateSession(pausedSession)
        _activeSessionFlow.value = pausedSession
    }

    suspend fun resumeSession() {
        val currentSession = _activeSessionFlow.value ?: return
        if (currentSession.status != "PAUSED") return
        
        val activeSession = currentSession.copy(status = "ACTIVE")
        sessionDao.updateSession(activeSession)
        _activeSessionFlow.value = activeSession
        sensorManager.startAllActiveSensors()
    }

    suspend fun continueOrphanedSession() {
        val orphaned = _orphanedSessionFlow.value ?: return
        _orphanedSessionFlow.value = null
        _activeSessionFlow.value = orphaned
        sensorManager.startAllActiveSensors()
    }

    suspend fun finishOrphanedSession() {
        val orphaned = _orphanedSessionFlow.value ?: return
        _orphanedSessionFlow.value = null
        
        val completedSession = orphaned.copy(
            endTime = Clock.System.now().toEpochMilliseconds(),
            status = "COMPLETED"
        )
        sessionDao.updateSession(completedSession)
        
        // Trigger background export
        csvExportEngine.exportSession(completedSession)
    }
}
