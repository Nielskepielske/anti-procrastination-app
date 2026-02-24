package com.example.procrastination_detection

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.procrastination_detection.interfaces.AppScanner

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "procrastination_detection",
    ) {
        val scanner = DesktopAppScanner();
        AppListScreen(scanner);
    }
}