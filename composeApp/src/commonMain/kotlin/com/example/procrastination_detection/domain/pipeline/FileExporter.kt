package com.example.procrastination_detection.domain.pipeline

interface FileExporter {
    /**
     * Saves the given CSV content to a compressed file and returns the file path.
     */
    suspend fun saveCompressedCsv(sessionId: String, csvContent: String): String
}
