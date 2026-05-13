package com.example.procrastination_detection.domain.trigger

import com.example.procrastination_detection.domain.model.Category

abstract class IAmbiguousActionTrigger : ActionTrigger {
    override val category: Category = Category.AMBIGUOUS
}