package com.example.procrastination_detection.helpers

import com.example.procrastination_detection.models.db.MonitoredProcess
import com.example.procrastination_detection.models.db.Process
import com.example.procrastination_detection.models.db.Rule

object ProcrastinationEvaluator {
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

    fun evaluateProcrastination(process: MonitoredProcess, activeRule: Rule): Boolean {
        val category = process.process.category

        // 1. Find the custom limit for this specific category in the active rule
        val ruleLine = activeRule.lines.find { it.category.id == category.id }

        // 2. Fallback: If the user didn't configure this category in the rule,
        // we just rely on the category's global "isProductive" default.
        if (ruleLine == null) {
            return !category.isProductive
        }

        // 3. If the rule explicitly says this category is Productive, they are safe!
        if (ruleLine.isProductive) {

            println(ruleLine.isProductive)
            return false
        }

        // 4. It is an unproductive category. Let's check the math!
        // (If the limit is 0, they get 0 seconds, meaning instant procrastination)
        val isOverConsecutive = process.consecutiveSeconds >= ruleLine.maxConsecutiveSeconds
        //val isOverTotal = process.totalSeconds >= ruleLine.maxTotalSeconds

        //return isOverConsecutive || isOverTotal
        print(ruleLine.maxConsecutiveSeconds)
        return isOverConsecutive
    }
}