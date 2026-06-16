package com.example.procrastination_detection.platform.desktop

import com.example.procrastination_detection.domain.pipeline.FileExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

class DesktopFileExporter : FileExporter {
    override suspend fun saveCompressedCsv(sessionId: String, csvContent: String): String = withContext(Dispatchers.IO) {
        val appDir = File(System.getProperty("user.home"), ".procrastination_detection")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        val exportDir = File(appDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val file = File(exportDir, "$sessionId.csv.gz")
        FileOutputStream(file).use { fos ->
            GZIPOutputStream(fos).use { gzip ->
                gzip.write(csvContent.toByteArray())
            }
        }
        file.absolutePath
    }
}
