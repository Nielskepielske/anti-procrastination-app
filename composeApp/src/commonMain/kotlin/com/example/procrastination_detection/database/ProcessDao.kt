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
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessDao {
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

    // When a new app is detected, you'll need to insert all parts:
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProcess(process: ProcessEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonitoredEntry(entry: MonitoredProcessEntity)
}