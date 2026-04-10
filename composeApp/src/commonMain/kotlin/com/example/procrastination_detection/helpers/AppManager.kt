package com.example.procrastination_detection.helpers

import com.example.procrastination_detection.models.WindowInfo

// In commonMain/src/commonMain/kotlin/AppManager.kt
expect fun getActiveGuiApps(): List<String>

expect fun getActiveApp(): WindowInfo?

// This is the get the name of THIS application
expect fun getMyAppProcessName(): String