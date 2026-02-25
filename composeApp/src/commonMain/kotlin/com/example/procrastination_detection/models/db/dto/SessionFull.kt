package com.example.procrastination_detection.models.db.dto

import androidx.room.Embedded
import androidx.room.Relation

data class SessionFull(
    @Embedded val session: SessionEntity,

    @Relation(
        entity = RuleEntity::class,
        parentColumn = "ruleId",
        entityColumn = "id"
    )
    val rule: RuleWithLines, // Reuses the Rule DTO we made earlier

    @Relation(
        entity = MonitoredProcessEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val processes: List<MonitoredProcessFull> // Reuses the MonitoredProcess DTO
)