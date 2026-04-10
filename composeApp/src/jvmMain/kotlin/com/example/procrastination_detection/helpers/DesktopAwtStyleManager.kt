package com.example.procrastination_detection.helpers

import com.example.procrastination_detection.interfaces.WindowStyleManager
import java.awt.Window

class DesktopAwtStyleManager(private val window: Window) : WindowStyleManager {
    override suspend fun setWindowOpacity(opacity: Float, targetWindowId: String?) {
        if (targetWindowId != null) {
            println("DesktopAwtStyleManager: Adjusting opacity of external apps is not supported.")
            return
        }
        try {
            // This works reliably on Windows, macOS, and some Linux X11 setups
            window.opacity = opacity.coerceIn(0.0f, 1.0f)
        } catch (e: UnsupportedOperationException) {
            println("AWT transparency not supported on this OS/Compositor.")
        }
    }
}