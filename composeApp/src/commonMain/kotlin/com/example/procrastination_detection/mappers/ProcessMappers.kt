package com.example.procrastination_detection.mappers

import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.MonitoredProcess
import com.example.procrastination_detection.models.db.Process
import com.example.procrastination_detection.models.db.dto.MonitoredProcessEntity
import com.example.procrastination_detection.models.db.dto.MonitoredProcessFull
import com.example.procrastination_detection.models.db.dto.ProcessEntity
import com.example.procrastination_detection.models.db.dto.ProcessWithCategory

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

fun ProcessWithCategory.toDomain(): Process {
    return Process(
        id = this.process.id,
        name = this.process.name,
        category = Category(
            id = this.category.id,
            name = this.category.name,
            isProductive = this.category.isProductive
        )
    )
}

fun ProcessEntity.toDomain(): Process {
    return Process(
        id = this.id,
        name = this.name,
        category = Category(
            id = this.categoryId,
            name = "",
            isProductive = false
        )
    )
}