package com.example.procrastination_detection.helpers

import com.example.procrastination_detection.interfaces.WindowStyleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
data class HyprClient(
    val address: String,
    val `class`: String,
    val title: String,
    val mapped: Boolean
)

private val json = Json { ignoreUnknownKeys = true }

suspend fun getMatchingAddresses(className: String): List<String> = withContext(Dispatchers.IO) {
    val pb = ProcessBuilder("hyprctl", "-j", "clients")
    HyprlandEnvHelper.applyTo(pb)
    val process = pb
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()

    json.decodeFromString<List<HyprClient>>(output)
        .filter { it.mapped && it.`class` == className }
        .map { it.address }
}
class LinuxHyprlandStyleManager : WindowStyleManager {

    override suspend fun setWindowOpacity(opacity: Float, targetWindowId: String?) {
        val safeOpacity = opacity.coerceIn(0.0f, 1.0f)
        val opacityStr = safeOpacity.toString()

        println("Trying to set opacity $safeOpacity to $targetWindowId")

        try {
            // setprop target: address:0x... or "active"
            val setPropTarget = when {
                targetWindowId == null -> "active"
                targetWindowId.startsWith("0x") -> "address:$targetWindowId"
                else -> "class:$targetWindowId"
            }

            // windowrule match: can only match by class/title, not by address
            // If we only have an address, skip the rule (setprop alone will handle it)
            val windowRuleMatch = when {
                targetWindowId == null -> null  // no persistent rule for "active"
                targetWindowId.startsWith("0x") -> null  // can't match address in windowrule
                else -> "match:class $targetWindowId"
            }

            // 1. Set persistent rule if we have a class to match on
//            if (windowRuleMatch != null) {
//                executeAndLog(listOf(
//                    "hyprctl", "keyword", "windowrule",
//                    "$windowRuleMatch, opacity $opacityStr override $opacityStr override"
//                ))
//            }

            // 2. Apply immediately to the target window
//            executeAndLog(listOf(
//                "hyprctl", "dispatch", "setprop",
//                setPropTarget, "opacity", opacityStr, "override", opacityStr, "override"
//            ))
            executeAndLog(listOf(
                "hyprctl", "dispatch", "setprop",
                setPropTarget, "opacity", opacityStr
            ))
            executeAndLog(listOf(
                "hyprctl", "dispatch", "setprop",
                setPropTarget, "opacity_inactive", opacityStr
            ))

            //forceRuleReeval(targetWindowId!!, opacityStr)


        } catch (e: Exception) {
            println("[HyprlandStyleManager] ❌ Unexpected failure: ${e.message}")
        }
    }

    suspend fun executeAndLog(command: List<String>): String = withContext(Dispatchers.IO) {
        try {
            val pb = ProcessBuilder(command)
            HyprlandEnvHelper.applyTo(pb)
            val process = pb
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()

            // Log everything, not just failures
            println("[HyprlandStyleManager] CMD: ${command.joinToString(" ")}")
            println("[HyprlandStyleManager] Exit: $exitCode | Output: $output")

            output
        } catch (e: Exception) {
            println("[HyprlandStyleManager] ❌ Failed to execute: ${e.message}")
            ""
        }
    }
    suspend fun waitForWindow(title: String, timeoutMs: Long = 5000): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var attempt = 1

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val pb = ProcessBuilder("hyprctl", "clients")
                HyprlandEnvHelper.applyTo(pb)
                val process = pb
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()

                if (output.contains("title: $title", ignoreCase = true) ||
                    output.contains("initialTitle: $title", ignoreCase = true)) {
                    println("[HyprlandStyleManager] ✅ Window '$title' found on attempt $attempt!")
                    return@withContext true
                }
            } catch (e: Exception) {
                println("[HyprlandStyleManager] ❌ Exception: ${e.message}")
            }
            attempt++
            delay(50)
        }

        println("[HyprlandStyleManager] ❌ Timeout after ${attempt - 1} attempts.")
        return@withContext false
    }
}