package com.example.procrastination_detection.helpers

import com.example.procrastination_detection.interfaces.WindowStyleManager

class LinuxHyprlandStyleManager : WindowStyleManager {
    // In your LinuxHyprlandStyleManager:
    override fun setWindowOpacity(opacity: Float) {
        val safeOpacity = opacity.coerceIn(0.0f, 1.0f)
        try {
            // We can pass multiple rules at once using a bash script,
            // or just apply them in sequence.
            // It is important here that the "noblur" rule is set. Because it prevents culling and makes it so the windows behind the overlay are still visible
            val commands = listOf(
                "hyprctl keyword windowrulev2 float, initialTitle:AggressiveOverlay",
                "hyprctl keyword windowrulev2 pin, initialTitle:AggressiveOverlay",
                "hyprctl keyword windowrulev2 size 100% 99%, initialTitle:AggressiveOverlay",
                "hyprctl keyword windowrulev2 noblur, initialTitle:AggressiveOverlay",
                "hyprctl keyword windowrulev2 center, initialTitle:AggressiveOverlay",
//                "hyprctl keyword windowrulev2 blur, initialTitle:AggressiveOverlay",
                "hyprctl keyword windowrulev2 ignorealpha 0.0, initialTitle:AggressiveOverlay"
            )

            for (cmd in commands) {
                Runtime.getRuntime().exec(cmd.split(" ").toTypedArray()).waitFor()
            }

            // To live update the opacity we can't use the windowrulev2, instead we use setprop

            val liveUpdateCommand = listOf(
                "hyprctl",
                "setprop",
                "initialTitle:AggressiveOverlay",
                "alpha",
                "$safeOpacity"
            )
            ProcessBuilder(liveUpdateCommand).start().waitFor()
        } catch (e: Exception) {
            println("Hyprland IPC failed: ${e.message}")
        }
    }
}