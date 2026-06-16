package com.example.procrastination_detection.domain.pipeline

interface SessionDownloader {
    /**
     * Prompts the user or delegates to the OS to save/share the file.
     * @param internalFilePath The absolute path of the generated CSV file.
     * @param suggestedFileName The suggested name for the downloaded file.
     */
    suspend fun downloadSession(internalFilePath: String, suggestedFileName: String)
}
