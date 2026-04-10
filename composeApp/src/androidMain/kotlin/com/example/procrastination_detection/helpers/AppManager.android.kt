package com.example.procrastination_detection.helpers

import android.app.Application
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

// --- GET ALL APPS (Launchable Apps) ---
actual fun getActiveGuiApps(): List<String> {
    val context = AndroidAppBridge.applicationContext
    val pm = context.packageManager

    // We only want apps that have a launcher icon (GUI apps), not background services
    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

    return apps.map { resolveInfo ->
        resolveInfo.loadLabel(pm).toString()
    }.distinct().sorted()
}

import com.example.procrastination_detection.models.WindowInfo

// --- GET CURRENT ACTIVE APP ---
actual fun getActiveApp(): WindowInfo? {
    val context = AndroidAppBridge.applicationContext
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // Look at the events from the last 10 seconds to find the most recent app transition
    val endTime = System.currentTimeMillis()
    val startTime = endTime - (1000 * 10)

    val events = usageStatsManager.queryEvents(startTime, endTime)
    val event = UsageEvents.Event()
    var currentAppPackage: String? = null

    // Iterate through recent events to find the last app that was "Resumed" (brought to foreground)
    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
            currentAppPackage = event.packageName
        }
    }

    // Convert the package name (e.g., "com.google.android.youtube") to the real name ("YouTube")
    if (currentAppPackage != null) {
        val pm = context.packageManager
        return try {
            val appInfo = pm.getApplicationInfo(currentAppPackage, 0)
            val title = pm.getApplicationLabel(appInfo).toString()
            WindowInfo(currentAppPackage, title, currentAppPackage)
        } catch (e: PackageManager.NameNotFoundException) {
            WindowInfo(currentAppPackage, currentAppPackage, currentAppPackage) // Fallback to package name if label fails
        }
    }

    return null
}

actual fun getMyAppProcessName(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Application.getProcessName() ?: "Unknown"
    } else {
        // Fallback for older Android versions (though you likely know your package name)
        "com.yourcompany.focusapp"
    }
}