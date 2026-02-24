package com.example.procrastination_detection

// In desktopMain/src/desktopMain/kotlin/AppManager.desktop.kt
import java.io.BufferedReader
import java.io.InputStreamReader

actual fun getActiveGuiApps(): List<String> {
    val os = System.getProperty("os.name").lowercase()

    return when {
        os.contains("win") -> getWindowsApps()
        os.contains("mac") -> getMacApps()
        os.contains("nix") || os.contains("nux") || os.contains("aix") || os.contains("fedora") -> getLinuxApps()
        else -> emptyList()
    }
}

// --- WINDOWS IMPLEMENTATION ---
private fun getWindowsApps(): List<String> {
    val apps = mutableListOf<String>()
    try {
        // We use PowerShell to get processes that have a MainWindowTitle (GUI apps)
        val command = "Get-Process | Where-Object { \$_.MainWindowTitle } | Select-Object -ExpandProperty MainWindowTitle"
        val process = ProcessBuilder("powershell", "-command", command).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val title = line?.trim()
            if (!title.isNullOrEmpty()) {
                apps.add(title)
            }
        }
        process.waitFor()
    } catch (e: Exception) {
        println("Error fetching Windows apps: ${e.message}")
    }
    return apps.distinct() // Remove duplicates if multiple windows have the same name
}

// --- macOS IMPLEMENTATION ---
private fun getMacApps(): List<String> {
    val apps = mutableListOf<String>()
    try {
        // We use AppleScript to ask System Events for apps that aren't running just in the background
        val script = "tell application \"System Events\" to get name of (processes where background only is false)"
        val process = ProcessBuilder("osascript", "-e", script).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        // AppleScript returns a single comma-separated string: "Finder, Safari, IntelliJ IDEA"
        val output = reader.readLine()
        if (output != null) {
            apps.addAll(output.split(",").map { it.trim() })
        }
        process.waitFor()
    } catch (e: Exception) {
        println("Error fetching macOS apps: ${e.message}")
    }
    return apps
}

// --- LINUX IMPLEMENTATION (From earlier) ---
private fun getLinuxApps(): List<String> {
    val sessionType = System.getenv("XDG_SESSION_TYPE")?.lowercase() ?: ""

    return if (sessionType == "wayland") {
        val desktop = System.getenv("XDG_CURRENT_DESKTOP")?.lowercase() ?: ""
        when {
            desktop.contains("gnome") -> getGnomeWaylandApps()
            desktop.contains("kde") -> getKdeWaylandApps()
            // wlroots covers Sway, Hyprland, etc.
            desktop.contains("sway") -> getWlrootsWaylandApps()
            desktop.contains("hyprland") -> getHyprlandApps()
            else -> getX11Apps() // Fallback
        }
    } else {
        getX11Apps() // This is your previous wmctrl implementation
    }
}

private fun getWlrootsWaylandApps(): List<String> {
    val apps = mutableListOf<String>()
    try {
        // Output format: "app_id: Window Title"
        val process = ProcessBuilder("wlrctl", "toplevel", "list").start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val title = line!!.substringAfter(":").trim()
            if (title.isNotEmpty()) apps.add(title)
        }
        process.waitFor()
    } catch (e: Exception) {
        println("Is wlrctl installed? ${e.message}")
    }
    return apps
}

private fun getHyprlandApps(): List<String> {
    val apps = mutableListOf<String>()
    try {
        // hyprctl clients returns a list of all open windows and their properties
        val process = ProcessBuilder("hyprctl", "clients").start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val trimmed = line!!.trim()

            // We look for the line specifying the application class
            // Example output line: "class: firefox"
            if (trimmed.startsWith("class:")) {
                val className = trimmed.removePrefix("class:").trim()

                // Ignore empty classes or internal Hyprland layers if any pop up
                if (className.isNotEmpty()) {
                    apps.add(className)
                }
            }

            // Alternatively, if you want the exact Window Title instead of the App Name,
            // you can search for "title:" instead of "class:"
        }
        process.waitFor()
    } catch (e: Exception) {
        println("Error fetching Hyprland apps. Is hyprctl available? ${e.message}")
    }

    // An app like Chrome might have multiple windows open, so we remove duplicates
    return apps.distinct()
}

private fun getGnomeWaylandApps(): List<String> {
    val apps = mutableListOf<String>()
    try {
        // This requires the "Window Calls" GNOME extension to be installed by the user
        val command = listOf(
            "gdbus", "call", "--session",
            "--dest", "org.gnome.Shell",
            "--object-path", "/org/gnome/Shell/Extensions/Windows",
            "--method", "org.gnome.Shell.Extensions.Windows.List"
        )
        val process = ProcessBuilder(command).start()
        // ... parse the JSON array returned by this D-Bus call ...
    } catch (e: Exception) {
        println("GNOME Wayland requires an extension to list windows.")
    }
    return apps
}

private fun getX11Apps(): List<String> {
    val apps = mutableListOf<String>()
    try {
        // wmctrl -l lists all windows managed by the X11 window manager
        val process = ProcessBuilder("wmctrl", "-l").start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            // Output example: 0x02800003  0 user-pc  Mozilla Firefox
            val parts = line!!.split("\\s+".toRegex(), limit = 4)
            if (parts.size >= 4) {
                val windowTitle = parts[3]
                // Filter out standard desktop environment elements
                if (windowTitle != "Desktop" && windowTitle != "panel") {
                    apps.add(windowTitle)
                }
            }
        }
        process.waitFor()
    } catch (e: Exception) {
        println("Error fetching X11 apps. Is wmctrl installed? ${e.message}")
    }
    return apps
}

private fun getKdeWaylandApps(): List<String> {
    val apps = mutableListOf<String>()
    try {
        // Step 1: Search for all window IDs.
        // "." is a regex that matches any window name.
        val searchProcess = ProcessBuilder("kdotool", "search", "--name", ".").start()
        val searchReader = BufferedReader(InputStreamReader(searchProcess.inputStream))

        // Read all the window IDs returned by kdotool
        val windowIds = searchReader.readLines()
        searchProcess.waitFor()

        // Step 2: Iterate through each ID to get the actual window title
        for (id in windowIds) {
            if (id.isNotBlank()) {
                val nameProcess = ProcessBuilder("kdotool", "getwindowname", id.trim()).start()
                val nameReader = BufferedReader(InputStreamReader(nameProcess.inputStream))

                val name = nameReader.readLine()?.trim()
                if (!name.isNullOrEmpty() && name != "Desktop") {
                    apps.add(name)
                }
                nameProcess.waitFor()
            }
        }
    } catch (e: Exception) {
        println("Error fetching KDE Wayland apps. Is kdotool installed? ${e.message}")
    }

    // kdotool might return duplicate IDs or names depending on how Plasma groups windows
    return apps.distinct()
}