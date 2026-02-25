package com.example.procrastination_detection.mappers

import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.dto.CategoryEntity

fun CategoryEntity.toDomain() : Category {
    return Category(
        id = this.id,
        name = this.name,
        isProductive = this.isProductive
    )
}