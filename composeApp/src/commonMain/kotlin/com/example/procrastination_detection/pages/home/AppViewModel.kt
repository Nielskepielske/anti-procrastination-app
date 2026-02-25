package com.example.procrastination_detection.pages.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.procrastination_detection.helpers.getActiveApp
import com.example.procrastination_detection.helpers.getActiveGuiApps
import com.example.procrastination_detection.globals.DefaultRules
import com.example.procrastination_detection.interfaces.AppRepository
import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.MonitoredProcess
import com.example.procrastination_detection.models.db.Process
import com.example.procrastination_detection.models.db.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.emptyList
import kotlin.collections.get
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AppListViewModel(private val repository: AppRepository) : ViewModel() {
    private val _apps = MutableStateFlow(mutableListOf<String>())
    val apps = _apps.asStateFlow()

    val monitoredProcesses = repository.monitoredProcesses.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val interval = 1L
    val defaultRule = DefaultRules.getRules().first()

    @OptIn(ExperimentalUuidApi::class)
    fun start() {
        viewModelScope.launch(Dispatchers.IO) {
            val session = repository.getOrCreateSession("Test Session", defaultRule)

            while (true) {
                loadInstalledApps() // Assuming this updates _apps
                val currentActiveApp = getActiveApp() ?: "default"

                // 1. READ: Get the existing object from the repository
                val existingProcess = repository.getMonitoredProcess(currentActiveApp, session.id)

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
                repository.saveMonitoredProcess(processToSave)

                delay(interval * 1000L)
            }
        }
    }

    private fun loadInstalledApps() {
        _apps.value = getActiveGuiApps().toMutableList()
    }
}