package com.example.procrastination_detection.platform.android

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.procrastination_detection.domain.pipeline.SessionDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class AndroidSessionDownloader(private val context: Context) : SessionDownloader {
    override suspend fun downloadSession(internalFilePath: String, suggestedFileName: String) {
        withContext(Dispatchers.IO) {
            val sourceFile = File(internalFilePath)
            if (!sourceFile.exists()) {
                println("AndroidSessionDownloader: Source file does not exist at $internalFilePath")
                return@withContext
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, suggestedFileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }

                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            FileInputStream(sourceFile).use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        showToast("Saved to Downloads: $suggestedFileName")
                    } else {
                        println("AndroidSessionDownloader: Failed to create MediaStore entry")
                        showToast("Failed to save file")
                    }
                } else {
                    // For older devices, write directly to the public Downloads directory
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val destFile = File(downloadsDir, suggestedFileName)
                    
                    FileOutputStream(destFile).use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    showToast("Saved to Downloads: $suggestedFileName")
                }
            } catch (e: Exception) {
                println("AndroidSessionDownloader: Failed to save file: ${e.message}")
                showToast("Error saving file: ${e.message}")
            }
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
