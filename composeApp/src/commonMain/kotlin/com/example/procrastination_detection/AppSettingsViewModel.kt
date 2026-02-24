package com.example.procrastination_detection

import androidx.lifecycle.ViewModel
import com.example.procrastination_detection.interfaces.AppScanner
import com.example.procrastination_detection.models.ProcessInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppListViewModel(private val scanner: AppScanner) : ViewModel() {
    private val _apps = MutableStateFlow<List<ProcessInfo>>(emptyList())
    private val _apps2 = MutableStateFlow<List<String>>(emptyList())
    val apps = _apps.asStateFlow()
    val apps2 = _apps2.asStateFlow()

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        _apps.value = scanner.getRunningAppNames()
        _apps2.value = getActiveGuiApps()
    }
}