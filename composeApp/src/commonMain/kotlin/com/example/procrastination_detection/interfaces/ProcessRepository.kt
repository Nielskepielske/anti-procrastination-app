package com.example.procrastination_detection.interfaces

import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.Process
import kotlinx.coroutines.flow.Flow

interface ProcessRepository {
    val allProcesses: Flow<List<Process>>
    suspend fun updateProcessCategory(process: Process, category: Category)
}