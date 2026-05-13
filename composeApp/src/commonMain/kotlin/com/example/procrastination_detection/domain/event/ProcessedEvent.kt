package com.example.procrastination_detection.domain.event

import com.example.procrastination_detection.domain.model.Category

data class ProcessedEvent(
    val timestamp: Long,
    val payload: SensorPayload,
    val category: Category
)
