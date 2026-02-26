package com.example.procrastination_detection.mappers

import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.MonitoredProcess
import com.example.procrastination_detection.models.db.Process
import com.example.procrastination_detection.models.db.Rule
import com.example.procrastination_detection.models.db.RuleLine
import com.example.procrastination_detection.models.db.dto.CategoryEntity
import com.example.procrastination_detection.models.db.dto.MonitoredProcessEntity
import com.example.procrastination_detection.models.db.dto.ProcessEntity
import com.example.procrastination_detection.models.db.dto.RuleEntity
import com.example.procrastination_detection.models.db.dto.RuleLineEntity

fun Category.toEntity() = CategoryEntity(
    id = this.id,
    name = this.name,
    isProductive = this.isProductive
)

fun Process.toEntity() = ProcessEntity(
    id = this.id,
    name = this.name,
    categoryId = this.category.id
)

fun MonitoredProcess.toEntity() = MonitoredProcessEntity(
    id = this.id,
    processId = this.process.id,
    sessionId = this.sessionId,
    consecutiveSeconds = this.consecutiveSeconds,
    totalSeconds = this.totalSeconds
)

fun Rule.toEntity() = RuleEntity(id = this.id, name = this.name)

fun RuleLine.toEntity() = RuleLineEntity(
    id = this.id,
    ruleId = this.ruleId,
    name = this.name,
    categoryId = this.category.id,
    isProductive = this.isProductive,
    maxConsecutiveSeconds = this.maxConsecutiveSeconds,
    maxTotalSeconds = this.maxTotalSeconds
)