package com.example.procrastination_detection.helpers

import android.app.NotificationManager
import com.mmk.kmpnotifier.notification.NotifierManager

actual fun sendDistractionAlert(title: String, message: String) {
    NotifierManager.getLocalNotifier().notify(title, message)
}