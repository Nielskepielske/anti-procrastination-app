package com.example.procrastination_detection.helpers

import com.example.procrastination_detection.models.db.Process
import com.example.procrastination_detection.models.db.Rule

class ProcrastinationEvaluator {
    /**
     * Determines if a process is productive based on the active Rule.
     * 1. Check if there is a RuleLine specifically for this Category.
     * 2. If yes, use the RuleLine's 'isProductive' override.
     * 3. If no, use the Category's default 'isProductive' value.
     */
    fun isCurrentlyProductive(process: Process, activeRule: Rule): Boolean {
        // Find a rule line that matches the category of the process
        val specificRuleLine = activeRule.lines.find { it.category.id == process.category.id }

        return if (specificRuleLine != null) {
            // Use the override from the rule
            specificRuleLine.isProductive
        } else {
            // Fallback to the global category default
            process.category.isProductive
        }
    }
}