package com.example.procrastination_detection.models.db.dto

import androidx.room.Embedded
import androidx.room.Relation

data class MonitoredProcessFull(
    @Embedded val monitoredEntity: MonitoredProcessEntity,
    @Relation(entity = ProcessEntity::class, parentColumn = "processId", entityColumn = "id")
    val processWithCategory: ProcessWithCategory
)
