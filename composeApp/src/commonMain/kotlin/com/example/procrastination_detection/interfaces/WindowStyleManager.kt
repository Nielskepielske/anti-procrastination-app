package com.example.procrastination_detection.interfaces

interface WindowStyleManager {
    /**
     * Sets the overall opacity of the application window.
     * @param opacity A value between 0.0f (invisible) and 1.0f (solid).
     */
    suspend fun setWindowOpacity(opacity: Float, targetWindowId: String? = null)

    // You can add other things later, like:
    // fun setBlurEffect(enabled: Boolean)
    // fun setAlwaysOnTop(enabled: Boolean)
}