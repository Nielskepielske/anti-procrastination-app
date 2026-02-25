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

    fun evaluateProcrastination(monitoredProcess: MonitoredProcess): Boolean {
        val category = monitoredProcess.process.category

        // 1. If it's a productive app, they are never procrastinating.
        if (category.isProductive) {
            return false
        }

        // 2. Define our thresholds (in seconds).
        // In the future, we will pull these from your RuleLine database!
        val maxConsecutiveAllowance = 5 * 60 // 5 minutes of continuous distraction allowed
        val maxTotalAllowance = 15 * 60      // 15 minutes of total distraction allowed per session

        // 3. Check if they have breached either limit
        val isOverConsecutiveLimit = monitoredProcess.consecutiveSeconds >= maxConsecutiveAllowance
        val isOverTotalLimit = monitoredProcess.totalSeconds >= maxTotalAllowance

        // 4. If they broke either rule while on an unproductive app, they are procrastinating!
        return isOverConsecutiveLimit || isOverTotalLimit
    }
}