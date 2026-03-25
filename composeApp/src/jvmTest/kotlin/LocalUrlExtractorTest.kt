import com.example.procrastination_detection.helpers.LocalUrlExtractor
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LocalUrlExtractorTest {

    @Test
    fun testUrlExtractionFromImage() = runTest {
        // 1. Load the test image from the jvmTest/resources folder
        // The ClassLoader looks directly in the resources folder
        val imageResource = this::class.java.classLoader.getResource("test_browser_screenshot.png")
        assertNotNull(imageResource, "Could not find the test image in resources!")

        // 2. Convert the image file to a ByteArray
        val imageBytes = imageResource.readBytes()

        // 3. Initialize your extractor
        val extractor = LocalUrlExtractor()

        // 4. Run the OCR and Regex (this might take a second or two on the first run)
        val extractedUrl = extractor.extractUrlFromImage(imageBytes)

        // 5. Print the result so you can see what Tesseract actually found
        println("Found URL: $extractedUrl")

        // 6. Assert that it found the correct URL
        // (Change this string to whatever URL is actually in your screenshot)
        assertEquals("https://en.wikipedia.org", extractedUrl)
    }
}