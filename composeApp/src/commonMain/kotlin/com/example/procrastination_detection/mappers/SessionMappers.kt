package com.example.procrastination_detection.mappers

import com.example.procrastination_detection.models.db.Session
import com.example.procrastination_detection.models.db.dto.SessionEntity
import com.example.procrastination_detection.models.db.dto.SessionFull

// shared/src/commonMain/kotlin/mappers/SessionMappers.kt

fun SessionFull.toDomain(): Session {
    return Session(
        id = this.session.id,
        name = this.session.name,
        // Using the mappers we defined in the previous steps
        rule = this.rule.toDomain(),
        processes = this.processes.map { it.toDomain() }
    )
}

fun Session.toEntity(): SessionEntity {
    return SessionEntity(
        id = this.id,
        name = this.name,
        ruleId = this.rule.id
    )
}