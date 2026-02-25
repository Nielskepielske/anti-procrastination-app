package com.example.procrastination_detection.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.procrastination_detection.models.db.Process
import com.example.procrastination_detection.models.db.dto.CategoryEntity
import com.example.procrastination_detection.models.db.dto.MonitoredProcessEntity
import com.example.procrastination_detection.models.db.dto.MonitoredProcessFull
import com.example.procrastination_detection.models.db.dto.ProcessEntity
import com.example.procrastination_detection.models.db.dto.ProcessWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessDao {
    // fetch queries
    @Transaction
    @Query("SELECT * FROM monitored_processes WHERE sessionId = :sessionId")
    fun getFullProcessesBySession(sessionId: String): Flow<List<MonitoredProcessFull>>

    @Transaction
    @Query("""
    SELECT mp.* FROM monitored_processes mp
    INNER JOIN processes p ON mp.processId = p.id
    WHERE p.name = :appName AND mp.sessionId = :sessionId LIMIT 1
""")
    suspend fun getFullProcessByNameAndSession(appName: String, sessionId: String): MonitoredProcessFull?

    @Query("SELECT * FROM processes WHERE LOWER(name) = LOWER(:appName) LIMIT 1")
    suspend fun getProcessByName(appName: String): ProcessWithCategory?
    @Query("SELECT * FROM processes")
    fun getAllProcesses(): Flow<List<ProcessWithCategory>>

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name)")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    // Insert functions
    // When a new app is detected, you'll need to insert all parts:
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProcess(process: ProcessEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonitoredEntry(entry: MonitoredProcessEntity)

    // Update functions
    @Query("UPDATE processes SET categoryId = :newCategoryId WHERE id = :processId")
    suspend fun updateProcessCategory(processId: String, newCategoryId: String)

    @Query("""
    UPDATE monitored_processes 
    SET consecutiveSeconds = 0 
    WHERE sessionId = :sessionId 
    AND processId IN (
        SELECT id FROM processes WHERE name != :activeAppName
    )
""")
    suspend fun resetConsecutiveTimeForOtherApps(sessionId: String, activeAppName: String)
}