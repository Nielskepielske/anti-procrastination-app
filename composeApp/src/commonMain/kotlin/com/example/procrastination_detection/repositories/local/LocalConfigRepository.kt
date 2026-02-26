package com.example.procrastination_detection.repositories.local

import com.example.procrastination_detection.database.ProcessDao
import com.example.procrastination_detection.database.RuleDao
import com.example.procrastination_detection.interfaces.ConfigRepository
import com.example.procrastination_detection.mappers.toDomain
import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.Rule
import com.example.procrastination_detection.models.db.RuleLine
import com.example.procrastination_detection.models.db.dto.CategoryEntity
import com.example.procrastination_detection.models.db.dto.RuleEntity
import com.example.procrastination_detection.models.db.dto.RuleLineEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LocalConfigRepository(
    private val processDao: ProcessDao,
    private val ruleDao: RuleDao
): ConfigRepository {
    override val allCategories: Flow<List<Category>> =
        processDao.getAllCategories().map { list -> list.map { it.toDomain() } }
    override val allRules: Flow<List<Rule>> =
        ruleDao.getAllRulesWithLines().map { list -> list.map { it.toDomain() } }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun createCategory(name: String, isProductive: Boolean): Category? {
        val existingCategory = processDao.getCategoryByName(name)
        if (existingCategory != null) return null // Fails, already exists

        val newId = Uuid.random().toString()
        val newCategoryEntity = CategoryEntity(id = newId, name = name, isProductive = isProductive)

        processDao.insertCategory(newCategoryEntity)
        return newCategoryEntity.toDomain() // Return the created category!
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun createRule(name: String, draftLines: List<RuleLine>) {
        val newRuleId = Uuid.random().toString()

        // 1. Create the parent entity
        val ruleEntity = RuleEntity(id = newRuleId, name = name)

        // 2. Map the domain RuleLines into Database Entities, assigning the new Rule ID
        val lineEntities = draftLines.map { draftLine ->
            RuleLineEntity(
                id = Uuid.random().toString(),
                ruleId = newRuleId,
                name = draftLine.name,
                categoryId = draftLine.category.id, // Extract the ID for the database
                isProductive = draftLine.isProductive,
                maxConsecutiveSeconds = draftLine.maxConsecutiveSeconds,
                maxTotalSeconds = draftLine.maxTotalSeconds
            )
        }

        // 3. Save to database atomically
        ruleDao.insertFullRule(ruleEntity, lineEntities)
    }

    override suspend fun deleteRule(ruleId: String) {
        ruleDao.deleteRule(ruleId)
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun saveRule(id: String?, name: String, draftLines: List<RuleLine>) {
        // If we have an ID, we are updating. If not, generate a new one!
        val ruleId = id ?: Uuid.random().toString()

        val ruleEntity = RuleEntity(id = ruleId, name = name)

        val lineEntities = draftLines.map { draftLine ->
            RuleLineEntity(
                id = Uuid.random().toString(),
                ruleId = ruleId,
                name = draftLine.name,
                categoryId = draftLine.category.id,
                isProductive = draftLine.isProductive,
                maxConsecutiveSeconds = draftLine.maxConsecutiveSeconds,
                maxTotalSeconds = draftLine.maxTotalSeconds
            )
        }

        if (id != null) {
            // We are updating an existing rule
            ruleDao.updateFullRule(ruleEntity, lineEntities, ruleId)
        } else {
            // We are creating a brand new rule
            ruleDao.insertFullRule(ruleEntity, lineEntities)
        }
    }
}