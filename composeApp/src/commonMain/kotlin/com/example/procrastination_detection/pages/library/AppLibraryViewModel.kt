package com.example.procrastination_detection.pages.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.procrastination_detection.interfaces.AppRepository
import com.example.procrastination_detection.interfaces.ConfigRepository
import com.example.procrastination_detection.interfaces.ProcessRepository
import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.Process
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppLibraryViewModel(
    private val processRepository: ProcessRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    val apps = processRepository.allProcesses.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val categories = configRepository.allCategories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun assignCategoryToApp(process: Process, category: Category) {
        viewModelScope.launch {
            processRepository.updateProcessCategory(process, category)
        }
    }
}

