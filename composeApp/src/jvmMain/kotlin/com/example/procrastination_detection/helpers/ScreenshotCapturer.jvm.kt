package com.example.procrastination_detection.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

actual suspend fun takeScreenshot(): ByteArray? = withContext(Dispatchers.IO) {
    val osName = System.getProperty("os.name").lowercase()
    val isLinux = osName.contains("nix") || osName.contains("nux") || osName.contains("aix")

    // Hyprland sets specific environment variables we can check
    val xdgDesktop = System.getenv("XDG_CURRENT_DESKTOP")
    val hyprlandSignature = System.getenv("HYPRLAND_INSTANCE_SIGNATURE")
    val isHyprland = xdgDesktop?.equals("Hyprland", ignoreCase = true) == true || hyprlandSignature != null

    if (isLinux && isHyprland) {
        println("ScreenshotCapturer: 🐧 Hyprland detected. Using native Wayland tools...")
        // Try the fast Wayland approach. If it fails for some reason, fall back to Robot!
        takeHyprlandScreenshot() ?: takeRobotScreenshot()
    } else {
        println("ScreenshotCapturer: 💻 Standard OS detected ($osName). Using Java Robot...")
        takeRobotScreenshot()
    }
}

private fun takeHyprlandScreenshot(): ByteArray? {
    return try {
        // 1. Get the active window details
        val hyprctlProcess = ProcessBuilder("hyprctl", "activewindow").start()
        val windowInfo = hyprctlProcess.inputStream.bufferedReader().readText()
        hyprctlProcess.waitFor()

        // 2. Parse the X, Y, Width, and Height
        val atRegex = """at:\s*(-?\d+),\s*(-?\d+)""".toRegex()
        val sizeRegex = """size:\s*(\d+),\s*(\d+)""".toRegex()

        val atMatch = atRegex.find(windowInfo)
        val sizeMatch = sizeRegex.find(windowInfo)

        if (atMatch == null || sizeMatch == null) {
            println("ScreenshotCapturer: ⚠️ Could not determine active window geometry.")
            return null
        }

        val x = atMatch.groupValues[1]
        val y = atMatch.groupValues[2]
        val width = sizeMatch.groupValues[1]
        val height = sizeMatch.groupValues[2]

        val geometry = "$x,$y ${width}x$height"

        // 3. Command grim to take a screenshot and pipe to stdout
        val grimProcess = ProcessBuilder("grim", "-g", geometry, "-").start()
        val imageBytes = grimProcess.inputStream.readBytes()
        val exitCode = grimProcess.waitFor()

        if (exitCode != 0 || imageBytes.isEmpty()) {
            println("ScreenshotCapturer: ❌ grim failed with exit code $exitCode")
            return null
        }

        imageBytes
    } catch (e: Exception) {
        println("ScreenshotCapturer: ❌ Hyprland capture failed - ${e.message}")
        null
    }
}

private fun takeRobotScreenshot(): ByteArray? {
    return try {
        val robot = Robot()
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val screenRect = Rectangle(screenSize)
        val screenCapture = robot.createScreenCapture(screenRect)

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(screenCapture, "png", outputStream)
        outputStream.toByteArray()
    } catch (e: Exception) {
        println("ScreenshotCapturer: ❌ Robot capture failed - ${e.message}")
        null
    }
}