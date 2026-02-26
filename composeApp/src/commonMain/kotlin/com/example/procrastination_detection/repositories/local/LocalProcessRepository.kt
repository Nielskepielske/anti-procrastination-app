package com.example.procrastination_detection.repositories.local

import com.example.procrastination_detection.database.ProcessDao
import com.example.procrastination_detection.interfaces.ProcessRepository
import com.example.procrastination_detection.mappers.toDomain
import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.Process
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalProcessRepository(
    private val processDao: ProcessDao
): ProcessRepository {
    override val allProcesses: Flow<List<Process>> =
        processDao.getAllProcesses().map { list -> list.map { it.toDomain() } }

    override suspend fun updateProcessCategory(
        process: Process,
        category: Category
    ) {
        processDao.updateProcessCategory(process.id, category.id)
    }
}