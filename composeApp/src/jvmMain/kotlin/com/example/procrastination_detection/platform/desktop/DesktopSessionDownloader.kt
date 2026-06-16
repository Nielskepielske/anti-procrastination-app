package com.example.procrastination_detection.platform.desktop

import com.example.procrastination_detection.domain.pipeline.SessionDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class DesktopSessionDownloader : SessionDownloader {
    override suspend fun downloadSession(internalFilePath: String, suggestedFileName: String) {
        withContext(Dispatchers.IO) {
            val sourceFile = File(internalFilePath)
            if (!sourceFile.exists()) {
                println("DesktopSessionDownloader: Source file does not exist at $internalFilePath")
                return@withContext
            }

            // Create a FileDialog
            val dialog = FileDialog(null as Frame?, "Save Session Data", FileDialog.SAVE)
            dialog.file = suggestedFileName
            dialog.isVisible = true // This blocks until the user selects a file or cancels

            val dir = dialog.directory
            val file = dialog.file

            if (dir != null && file != null) {
                val destFile = File(dir, file)
                try {
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    println("DesktopSessionDownloader: Successfully saved to ${destFile.absolutePath}")
                } catch (e: Exception) {
                    println("DesktopSessionDownloader: Failed to save file: ${e.message}")
                }
            } else {
                println("DesktopSessionDownloader: User canceled save dialog")
            }
        }
    }
}
