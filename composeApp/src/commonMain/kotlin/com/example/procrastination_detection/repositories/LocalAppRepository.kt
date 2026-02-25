package com.example.procrastination_detection.repositories

import com.example.procrastination_detection.database.ProcessDao
import com.example.procrastination_detection.database.RuleDao
import com.example.procrastination_detection.database.SessionDao
import com.example.procrastination_detection.interfaces.AppRepository
import com.example.procrastination_detection.mappers.toDomain
import com.example.procrastination_detection.mappers.toEntity
import com.example.procrastination_detection.models.db.MonitoredProcess
import com.example.procrastination_detection.models.db.Rule
import com.example.procrastination_detection.models.db.Session
import com.example.procrastination_detection.models.db.dto.SessionEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LocalAppRepository(
    private val processDao: ProcessDao,
    private val sessionDao: SessionDao,
    private val ruleDao: RuleDao
) : AppRepository {

    // Keeps track of the current session ID to power the Flow
    private val activeSessionId = MutableStateFlow<String?>(null)

    // Automatically swaps to the new session's processes when activeSessionId changes
    @OptIn(ExperimentalCoroutinesApi::class)
    override val monitoredProcesses: Flow<List<MonitoredProcess>> = activeSessionId
        .filterNotNull()
        .flatMapLatest { sessionId ->
            processDao.getFullProcessesBySession(sessionId)
        }
        .map { list -> list.map { it.toDomain() } }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getOrCreateSession(name: String, defaultRule: Rule): Session {
        // 1. Check if the session already exists
        val existingSessionFull = sessionDao.getSessionByName(name)

        if (existingSessionFull != null) {
            activeSessionId.value = existingSessionFull.session.id
            return existingSessionFull.toDomain()
        }

        // 2. INSERT CATEGORIES FIRST!
        // A RuleLine cannot exist in the DB without its Category existing first.
        defaultRule.lines.forEach { line ->
            processDao.insertCategory(line.category.toEntity())
        }

        // 3. Insert Rule and RuleLines
        ruleDao.insertRule(defaultRule.toEntity())
        ruleDao.insertRuleLines(defaultRule.lines.map { it.toEntity() })

        // 4. Create and save the new Session
        val newSessionId = Uuid.random().toString()
        val sessionEntity = SessionEntity(
            id = newSessionId,
            name = name,
            ruleId = defaultRule.id
        )
        sessionDao.insertSession(sessionEntity)

        // 5. Update the active flow and return the domain model
        activeSessionId.value = newSessionId
        return Session(
            id = newSessionId,
            name = name,
            rule = defaultRule,
            processes = emptyList()
        )
    }

    override suspend fun getMonitoredProcess(appName: String, sessionId: String): MonitoredProcess? {
        // Fetch the joined DB object and convert it to our Domain model
        val entity = processDao.getFullProcessByNameAndSession(appName, sessionId)
        return entity?.toDomain()
    }

    override suspend fun saveMonitoredProcess(process: MonitoredProcess) {
        // Break down the rich Domain object into flat DB Entities
        val categoryEntity = process.process.category.toEntity()
        val processEntity = process.process.toEntity()
        val monitoredEntity = process.toEntity()

        // Insert them in order from least-dependent to most-dependent
        // to satisfy SQLite Foreign Key constraints
        processDao.insertCategory(categoryEntity)
        processDao.insertProcess(processEntity)
        processDao.insertMonitoredEntry(monitoredEntity)
    }
}
