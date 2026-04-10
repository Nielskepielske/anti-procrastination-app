package com.example.procrastination_detection.factories

import com.example.procrastination_detection.helpers.DesktopAwtStyleManager
import com.example.procrastination_detection.helpers.LinuxHyprlandStyleManager
import com.example.procrastination_detection.interfaces.WindowStyleManager
import java.awt.Window

object WindowStyleManagerFactory {

    fun create(window: Window? = null): WindowStyleManager {
        val osName = System.getProperty("os.name").lowercase()

        return when {
            // If it's Android (Requires some KMP expect/actual magic or specific build flavors usually)
            // System.getProperty("java.vendor").contains("Android") -> {
                // AndroidStyleManager()
            // }

            // If it's Windows
            osName.contains("win") -> {
                DesktopAwtStyleManager(window!!)
            }

            // If it's Linux, we can get clever and check for Hyprland specifically
            osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> {
                if (System.getenv("HYPRLAND_INSTANCE_SIGNATURE") != null) {
                    LinuxHyprlandStyleManager()
                } else {
                    // Fallback to generic AWT for GNOME/KDE/X11
                    DesktopAwtStyleManager(window!!)
                }
            }

            // Fallback for macOS or unknowns
            else -> {
                if (window != null) DesktopAwtStyleManager(window)
                else DummyStyleManager() // A fallback that does nothing safely
            }
        }
    }
}

class DummyStyleManager : WindowStyleManager {
    override suspend fun setWindowOpacity(opacity: Float, targetWindowId: String?) {}
}