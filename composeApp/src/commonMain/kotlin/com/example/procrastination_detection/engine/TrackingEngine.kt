package com.example.procrastination_detection.engine

import com.example.procrastination_detection.helpers.ProcrastinationEvaluator
import com.example.procrastination_detection.helpers.getActiveApp
import com.example.procrastination_detection.interfaces.AppRepository
import com.example.procrastination_detection.interfaces.SessionRepository
import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.MonitoredProcess
import com.example.procrastination_detection.models.db.Process
import com.example.procrastination_detection.models.db.Rule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TrackingEngine(
    private val sessionRepository: SessionRepository
) {
    // 1. The Engine's lifecycle (lives as long as the TrackingEngine object does)
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitoringJob: Job? = null

    // 2. The Public States for the UI to observe
    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private val _currentActiveApp = MutableStateFlow("None")
    val currentActiveApp = _currentActiveApp.asStateFlow()

    private val _isProcrastinating = MutableStateFlow(false)
    val isProcrastinating = _isProcrastinating.asStateFlow()

    private val _consecutiveSeconds = MutableStateFlow(0L)
    val consecutiveSeconds = _consecutiveSeconds.asStateFlow()

    private val _currentSessionId = MutableStateFlow("")
    val currentSessionId = _currentSessionId.asStateFlow()

    // 3. The Control Function
    fun toggleTracking(interval: Long, defaultRule: Rule) {
        if (_isTracking.value) {
            monitoringJob?.cancel()
            _isTracking.value = false
            _currentActiveApp.value = "Paused"
            _isProcrastinating.value = false
        } else {
            _isTracking.value = true
            startMonitoringLoop(interval, defaultRule)
        }
    }

    // 4. The Monitoring Loop
    @OptIn(ExperimentalUuidApi::class)
    private fun startMonitoringLoop(interval: Long, defaultRule: Rule) {
        monitoringJob = engineScope.launch {
            val sessionName = "Session " + Uuid.random().toString().take(4)
            val session = sessionRepository.getOrCreateSession(sessionName, defaultRule)
            _currentSessionId.value = session.id
            var previousActiveApp = ""

            while (isActive) {
                val currentActiveApp = getActiveApp() ?: "default"
                val activeRule = sessionRepository.getActiveRuleForSession(session.id)

                // Inside your start() loop, right after you calculate 'currentActiveApp':
                _currentActiveApp.value = currentActiveApp ?: "None"

                if(currentActiveApp != previousActiveApp){
                    sessionRepository.resetConsecutiveTimeForOtherApps(session.id, currentActiveApp ?: "default")
                    previousActiveApp = currentActiveApp
                }

                // Using the evaluator we built previously (or checking the rule logic directly):
                val currentProcess = sessionRepository.getMonitoredProcess(currentActiveApp ?: "default", session.id)
                val isProd = currentProcess?.process?.category?.isProductive ?: true
                //_isProcrastinating.value = !isProd

                // 1. READ: Get the existing object from the repository
                val existingProcess = sessionRepository.getMonitoredProcess(currentActiveApp, session.id)

                // 2. MODIFY (or Create): Update the time
                val processToSave = if (existingProcess != null) {
                    existingProcess.copy(
                        totalSeconds = existingProcess.totalSeconds + interval,
                        consecutiveSeconds = existingProcess.consecutiveSeconds + interval
                    )
                } else {
                    // Create a brand new one if it wasn't in the repo
                    MonitoredProcess(
                        id = Uuid.random().toString(),
                        process = Process(
                            id = Uuid.random().toString(),
                            name = currentActiveApp,
                            category = Category(
                                id = Uuid.random().toString(),
                                name = "default",
                                isProductive = true
                            )
                        ),
                        sessionId = session.id,
                        consecutiveSeconds = interval,
                        totalSeconds = interval
                    )
                }

                // 3. WRITE: Give the whole object back to the repository
                sessionRepository.saveMonitoredProcess(processToSave)

                _isProcrastinating.value = ProcrastinationEvaluator.evaluateProcrastination(processToSave, activeRule)
                _consecutiveSeconds.value = processToSave.consecutiveSeconds

                delay(interval * 1000L)
            }
        }
    }
}