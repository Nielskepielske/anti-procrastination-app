package com.example.procrastination_detection.helpers

import com.mmk.kmpnotifier.notification.NotifierManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

actual fun sendDistractionAlert(title: String, message: String) {
    val osName = System.getProperty("os.name").lowercase(Locale.getDefault())

    if (osName.contains("nux") || osName.contains("nix")) {
        // LINUX / HYPRLAND: Bypass Java and use the native shell command
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // You can even pass your extracted icon path here with the -i flag!
                Runtime.getRuntime().exec(arrayOf("notify-send", title, message))
            } catch (e: Exception) {
                println("Failed to send native Linux notification: ${e.message}")
            }
        }
    } else {
        // WINDOWS / MAC: Safely use KMPNotifier
        CoroutineScope(Dispatchers.IO).launch {
            NotifierManager.getLocalNotifier().notify(title, message)
        }
    }
}
