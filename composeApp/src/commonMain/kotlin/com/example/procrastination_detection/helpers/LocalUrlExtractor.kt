package com.example.procrastination_detection.helpers

expect class LocalUrlExtractor(){
    suspend fun extractUrlFromImage(imageData: ByteArray): String?
}

fun extractUrlWithRegex(text: String): String? {
    // The Regex now supports SUBDOMAINS!
    // (?:[a-zA-Z0-9][a-zA-Z0-9-]*\.)+ means "one or more words ending in a dot"
    val urlPattern = """(?:https?://)?(?:www\.)?(?:[a-zA-Z0-9][a-zA-Z0-9-]*\.)+[a-zA-Z]{2,6}\b(?:[-a-zA-Z0-9()@:%_+.~#?&/=]*)""".toRegex()

    val matches = urlPattern.findAll(text)

    for (match in matches) {
        var foundUrl = match.value

        if (foundUrl.length < 5) continue

        // 1. Add https:// if it's missing
        if (!foundUrl.startsWith("http")) {
            foundUrl = "https://$foundUrl"
        }

        // 2. Extract ONLY the base domain for procrastination tracking
        // This looks for the first slash after the "https://" (which is at index 8)
        val pathStartIndex = foundUrl.indexOf('/', startIndex = 8)
        if (pathStartIndex != -1) {
            foundUrl = foundUrl.substring(0, pathStartIndex)
        }

        return foundUrl // Boom. Clean base URL with subdomains intact.
    }

    return null
}