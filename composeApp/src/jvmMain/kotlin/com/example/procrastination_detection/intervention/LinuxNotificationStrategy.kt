package com.example.procrastination_detection.intervention

import com.example.procrastination_detection.domain.intervention.InterventionStrategy
import java.io.IOException

class LinuxNotificationStrategy(override val id: String = "LINUX_NUDGE") : InterventionStrategy {
    override suspend fun executeIntervention() {
        try {
            // Fires a native Linux system notification
            ProcessBuilder(
                "notify-send",
                "--urgency=critical",
                "Focus Alert",
                "You've been distracted for a while. Time to re-route!"
            ).start()
        } catch (e: IOException) {
            println("Failed to send notification. Is notify-send installed?")
        }
    }

    override fun reset() {
        // Left blank intentionally for now. Future implementations could group or cancel existing notifications here.
    }
}