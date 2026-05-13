package com.example.procrastination_detection.helpers

// jvmMain
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.util.LoadLibs
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

actual class LocalUrlExtractor {
    /**
     * Extracts a URL from the given screenshot using a 4-step fallback chain:
     *
     *  1. BoundingBox OCR  +  windowTitle domain match  (fastest & most accurate)
     *  2. BoundingBox OCR  +  first found URL            (fast, no title corroboration)
     *  3. Full-image OCR   +  windowTitle domain match  (heavy, recovers hidden address bars)
     *  4. Full-image OCR   +  first found URL            (last resort)
     *
     * The windowTitle is used to cross-reference results. E.g. if the browser window title
     * contains "YouTube" and OCR found "youtube.com", that match gets promoted.
     */
    actual suspend fun extractUrlFromImage(imageData: ByteArray, windowTitle: String?): String? {
        val fullImage = ImageIO.read(ByteArrayInputStream(imageData)) ?: return null

        val tesseract = buildTesseract()

        // --- Prepare both image slices ---
        val croppedImage = cropTopBand(fullImage, CROP_RATIO)

        // --- Run OCR on both slices ---
        val croppedText = runOcr(tesseract, croppedImage) ?: ""
        println("=== OCR [Bounding Box] ===\n$croppedText\n=========================")

        // Filter out fake TLDs like ".bus" before candidates are considered
        val croppedUrls = extractAllUrlsWithRegex(croppedText).filter { hasKnownTld(it) }

        // Step 1: BoundingBox + title match
        val step1 = if (windowTitle != null) titleMatch(croppedUrls, windowTitle) else null
        if (step1 != null) {
            println("BrowserAnalyser: ✅ [Step 1] BoundingBox + Title Match → $step1")
            return step1
        }

        // Step 2: BoundingBox only
        val step2 = croppedUrls.firstOrNull()
        if (step2 != null) {
            println("BrowserAnalyser: ✅ [Step 2] BoundingBox Only → $step2")
            return step2
        }

        // --- Full image OCR (the heavy path) ---
        val fullText = runOcr(tesseract, fullImage) ?: ""
        println("=== OCR [Full Image] ===\n$fullText\n========================")

        val fullUrls = extractAllUrlsWithRegex(fullText).filter { hasKnownTld(it) }

        // Step 3: Full image + title match
        val step3 = if (windowTitle != null) titleMatch(fullUrls, windowTitle) else null
        if (step3 != null) {
            println("BrowserAnalyser: ✅ [Step 3] Full Image + Title Match → $step3")
            return step3
        }

        // Step 4: Full image only
        val step4 = fullUrls.firstOrNull()
        if (step4 != null) {
            println("BrowserAnalyser: ✅ [Step 4] Full Image Only → $step4")
        } else {
            println("BrowserAnalyser: 👁️ No URL found in any fallback step.")
        }
        return step4
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /** Crops the top [ratio] fraction of the image (e.g. 0.15 = top 15%). */
    private fun cropTopBand(image: BufferedImage, ratio: Float): BufferedImage {
        val croppedHeight = (image.height * ratio).toInt().coerceAtLeast(1)
        return image.getSubimage(0, 0, image.width, croppedHeight)
    }

    /** Runs Tesseract OCR on a BufferedImage. Returns null on hard failure. */
    private fun runOcr(tesseract: Tesseract, image: BufferedImage): String? {
        return try {
            tesseract.doOCR(image)
        } catch (e: Exception) {
            println("BrowserAnalyser: ❌ OCR error – ${e.message}")
            null
        }
    }

    /**
     * Tries to find a URL within [candidates] whose domain appears inside [windowTitle].
     *
     * Checks EVERY segment of the domain, not just the first, so:
     *   - "kotlinlang.org"    matches a title containing "kotlin" or "kotlinlang"
     *   - "github.com"        matches a title containing "github"
     *   - "stackoverflow.com" matches a title containing "stackoverflow"
     *
     * Segments shorter than 4 characters are skipped to avoid trivial false positives
     * like "org", "com", or "www" matching nearly any page title.
     */
    private fun titleMatch(candidates: List<String>, windowTitle: String): String? {
        val titleLower = windowTitle.lowercase()
        return candidates.firstOrNull { url ->
            val host = url
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .substringBefore("/")

            // Check every domain segment — e.g. ["kotlinlang", "org"] for "kotlinlang.org"
            host.split(".").any { segment ->
                val segLower = segment.lowercase()
                segLower.length >= 4 && (
                    // Direct segment match: "github" in title
                    titleLower.contains(segLower) ||
                    // 5-char prefix match: "youtu" from "youtu.be" matches "youtube" in title
                    (segLower.length >= 5 && titleLower.contains(segLower.take(5)))
                )
            }
        }
    }

    /**
     * Rejects URLs that use non-existent TLDs (e.g. "niels.bus", "foo.bar").
     * This prevents OCR noise — tab titles with email-like suffixes, abbreviations,
     * or random short words — from being treated as real web addresses.
     */
    private fun hasKnownTld(url: String): Boolean {
        val tld = url
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .substringAfterLast(".")
            .lowercase()
        return tld in KNOWN_TLDS
    }

    private fun buildTesseract(): Tesseract {
        val tessDataFolder = LoadLibs.extractTessResources("tessdata")
        return Tesseract().apply {
            setDatapath(tessDataFolder.absolutePath)
            setLanguage("eng")
        }
    }

    companion object {
        /** Fraction of the screen height used for the address-bar crop. */
        private const val CROP_RATIO = 0.15f

        /**
         * Allowlist of real TLDs. Not exhaustive, just common enough to filter obvious
         * OCR noise. Extend this set as new legitimate domains are encountered.
         */
        private val KNOWN_TLDS = setOf(
            // Generic
            "com", "org", "net", "edu", "gov", "mil", "int",
            // Tech / dev favourites
            "io", "dev", "app", "ai", "tech", "so",
            // Country codes commonly used by developers
            "be", "nl", "uk", "de", "fr", "ca", "au", "us", "eu",
            "jp", "cn", "ru", "br", "in", "co", "me", "tv", "fm", "am",
        )
    }
}