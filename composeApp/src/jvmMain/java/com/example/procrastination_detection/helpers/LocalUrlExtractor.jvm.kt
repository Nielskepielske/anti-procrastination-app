package com.example.procrastination_detection.helpers

// jvmMain
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.util.LoadLibs
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

actual class LocalUrlExtractor {
    actual suspend fun extractUrlFromImage(imageData: ByteArray): String? {
        val inputStream = ByteArrayInputStream(imageData)
        val bufferedImage = ImageIO.read(inputStream) ?: return null

        val tesseract = Tesseract()

        // This magic line grabs the folder from your resources,
        // copies it to a temp folder on the OS, and returns the physical File object!
        val tessDataFolder = LoadLibs.extractTessResources("tessdata")

        tesseract.setDatapath(tessDataFolder.absolutePath)
        tesseract.setLanguage("eng")

        val extractedText = try {
            tesseract.doOCR(bufferedImage)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        // ADD THIS LINE to see the raw output:
        println("=== RAW OCR OUTPUT ===\n$extractedText\n======================")

        return extractUrlWithRegex(extractedText)
    }
}