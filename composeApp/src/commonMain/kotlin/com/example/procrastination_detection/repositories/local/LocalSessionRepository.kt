package com.example.procrastination_detection.repositories.local

import com.example.procrastination_detection.database.ProcessDao
import com.example.procrastination_detection.database.RuleDao
import com.example.procrastination_detection.database.SessionDao
import com.example.procrastination_detection.interfaces.SessionRepository
import com.example.procrastination_detection.mappers.toDomain
import com.example.procrastination_detection.mappers.toEntity
import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.MonitoredProcess
import com.example.procrastination_detection.models.db.Rule
import com.example.procrastination_detection.models.db.Session
import com.example.procrastination_detection.models.db.dto.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LocalSessionRepository(
    private val sessionDao: SessionDao,
    private val processDao: ProcessDao,
    private val ruleDao: RuleDao
) : SessionRepository {

    private val activeSessionId = MutableStateFlow<String?>(null)

    override val monitoredProcesses: Flow<List<MonitoredProcess>> = activeSessionId
        .filterNotNull()
        .flatMapLatest { sessionId ->
            processDao.getFullProcessesBySession(sessionId)
        }
        .map { list -> list.map { it.toDomain() } }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getOrCreateSession(
        name: String,
        defaultRule: Rule
    ): Session {
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

    override suspend fun getMonitoredProcess(
        appName: String,
        sessionId: String
    ): MonitoredProcess? {
        // Fetch the joined DB object and convert it to our Domain model
        val entity = processDao.getFullProcessByNameAndSession(appName, sessionId)
        return entity?.toDomain()
    }

    override suspend fun saveMonitoredProcess(process: MonitoredProcess) {
        // 1. Check if the category already exists in the database by name
        val existingCategory = processDao.getCategoryByName(process.process.category.name)

        // 2. Figure out the true ID we should be using
        val actualCategoryId = if (existingCategory != null) {
            // It exists! Ignore the random UUID from the ViewModel and use the real one.
            existingCategory.id
        } else {
            // It's genuinely new. Insert it and use its ID.
            val newCategoryEntity = process.process.category.toEntity()
            processDao.insertCategory(newCategoryEntity)
            newCategoryEntity.id
        }

        // 3. Map the Process to an Entity, but FORCE it to use the correct Category ID
        var originalProcessEntity = process.process.toEntity()
        // Check for duplicate by name
        val existingProcess = processDao.getProcessByName(originalProcessEntity.name)

        existingProcess?.let {
            originalProcessEntity = it.toDomain().toEntity()
        }
        val correctedProcessEntity = originalProcessEntity.copy(categoryId = actualCategoryId)

        // 4. Map the MonitoredEntry
        val monitoredEntity = process.copy(process = existingProcess?.toDomain() ?: process.process).toEntity()


        // 5. Save them to the database
        processDao.insertProcess(correctedProcessEntity)
        processDao.insertMonitoredEntry(monitoredEntity)
    }

    override suspend fun updateProcessCategory(
        process: MonitoredProcess,
        category: Category
    ) {
        processDao.updateProcessCategory(process.id, category.id)
    }

    override suspend fun updateSessionRule(sessionId: String, ruleId: String) {
        sessionDao.updateSessionRule(sessionId, ruleId)
    }

    override suspend fun getActiveRuleForSession(sessionId: String): Rule {
        val sessionDto = sessionDao.getSessionById(sessionId)
        return sessionDto?.rule?.toDomain() ?: throw Exception("Session or Rule not found")
    }

    override suspend fun resetConsecutiveTimeForOtherApps(sessionId: String, activeAppName: String) {
        processDao.resetConsecutiveTimeForOtherApps(sessionId, activeAppName)

    }

}