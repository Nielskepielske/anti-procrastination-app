package com.example.procrastination_detection.helpers

import java.io.File

/**
 * Helper to ensure we can always talk to Hyprland, even if environment variables are stale.
 */
object HyprlandEnvHelper {
    private var cachedSignature: String? = null

    /**
     * Tries to find a valid Hyprland instance signature.
     */
    fun getSignature(): String? {
        // 1. Try environment variable first
        val envSig = System.getenv("HYPRLAND_INSTANCE_SIGNATURE")
        if (!envSig.isNullOrEmpty() && isValidSignature(envSig)) {
            return envSig
        }

        // 2. Try cached signature from previous discovery
        cachedSignature?.let {
            if (isValidSignature(it)) return it
        }

        // 3. Discover signature from the filesystem if environment is stale
        val discovered = discoverSignature()
        if (discovered != null && isValidSignature(discovered)) {
            cachedSignature = discovered
            return discovered
        }

        cachedSignature = null
        return null
    }

    /**
     * Verifies if a signature is currently valid by attempting a light hyprctl command.
     */
    private fun isValidSignature(signature: String): Boolean {
        return try {
            val pb = ProcessBuilder("hyprctl", "activewindow")
            pb.environment()["HYPRLAND_INSTANCE_SIGNATURE"] = signature
            val process = pb.start()
            val exitCode = process.waitFor()
            // hyprctl returns 4 if it cannot connect to the socket
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Scans /run/user/UID/hypr/ for the most recent active session directory.
     */
    private fun discoverSignature(): String? {
        val uid = try {
            // Attempt to get current user ID
            val process = Runtime.getRuntime().exec("id -u")
            process.inputStream.bufferedReader().readLine()?.trim() ?: "1000"
        } catch (e: Exception) {
            "1000"
        }
        
        // Potential Hyprland socket locations
        val potentialDirs = listOf("/run/user/$uid/hypr", "/tmp/hypr")
        
        for (basePath in potentialDirs) {
            val hyprDir = File(basePath)
            if (!hyprDir.exists() || !hyprDir.isDirectory) continue

            // Find the most recently modified directory that contains a .socket.sock
            val found = hyprDir.listFiles()
                ?.filter { it.isDirectory }
                ?.sortedByDescending { it.lastModified() }
                ?.firstOrNull { dir ->
                    File(dir, ".socket.sock").exists()
                }?.name
            
            if (found != null) return found
        }
        
        return null
    }

    /**
     * Appends the correct Hyprland environment to a ProcessBuilder.
     */
    fun applyTo(pb: ProcessBuilder) {
        val signature = getSignature()
        if (signature != null) {
            pb.environment()["HYPRLAND_INSTANCE_SIGNATURE"] = signature
        }
        
        // Also ensure XDG_RUNTIME_DIR is present as hyprctl might need it
        if (pb.environment()["XDG_RUNTIME_DIR"] == null) {
            System.getenv("XDG_RUNTIME_DIR")?.let {
                pb.environment()["XDG_RUNTIME_DIR"] = it
            }
        }
    }
}
