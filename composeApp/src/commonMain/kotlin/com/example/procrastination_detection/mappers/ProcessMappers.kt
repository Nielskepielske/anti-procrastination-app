package com.example.procrastination_detection.mappers

import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.MonitoredProcess
import com.example.procrastination_detection.models.db.Process
import com.example.procrastination_detection.models.db.dto.MonitoredProcessEntity
import com.example.procrastination_detection.models.db.dto.MonitoredProcessFull

fun MonitoredProcessFull.toDomain(): MonitoredProcess {
    val catEntity = this.processWithCategory.category
    val procEntity = this.processWithCategory.process

    return MonitoredProcess(
        id = this.monitoredEntity.id,
        sessionId = this.monitoredEntity.sessionId,
        consecutiveSeconds = this.monitoredEntity.consecutiveSeconds,
        totalSeconds = this.monitoredEntity.totalSeconds,
        process = Process(
            id = procEntity.id,
            name = procEntity.name,
            category = Category(
                id = catEntity.id,
                name = catEntity.name,
                isProductive = catEntity.isProductive
            )
        )
    )
}