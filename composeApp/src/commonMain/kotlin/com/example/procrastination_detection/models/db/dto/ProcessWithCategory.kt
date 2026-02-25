package com.example.procrastination_detection.models.db.dto

import androidx.room.Embedded
import androidx.room.Relation

data class ProcessWithCategory(
    @Embedded val process: ProcessEntity,
    @Relation(
        entity = CategoryEntity::class,
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity
)