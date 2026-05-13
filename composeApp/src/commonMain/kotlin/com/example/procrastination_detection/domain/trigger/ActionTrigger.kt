package com.example.procrastination_detection.domain.trigger

import com.example.procrastination_detection.domain.model.Category

interface ActionTrigger {
    val category: Category
    val id: String

    fun start()
    fun stop()
}