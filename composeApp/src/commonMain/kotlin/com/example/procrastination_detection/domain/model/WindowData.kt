package com.example.procrastination_detection.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WindowData(
    val processName: String,
    val windowTitle: String
)
