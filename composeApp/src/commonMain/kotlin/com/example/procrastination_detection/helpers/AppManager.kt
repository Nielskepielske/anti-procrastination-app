package com.example.procrastination_detection.helpers

// In commonMain/src/commonMain/kotlin/AppManager.kt
expect fun getActiveGuiApps(): List<String>

expect fun getActiveApp(): String?