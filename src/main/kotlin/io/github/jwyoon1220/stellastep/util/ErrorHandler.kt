package io.github.jwyoon1220.stellastep.util

/**
 * Simple error handler (no JNA/Win32 dependency – we use libGDX now).
 */
object ErrorHandler {
    fun handleError(
        e: Throwable,
        exit: Boolean = false,
        exitCode: Int = 1,
        title: String = "StellarStep Fatal Error"
    ) {
        System.err.println("[$title] ${e.message}")
        e.printStackTrace()
        if (exit) kotlin.system.exitProcess(exitCode)
    }

    fun handleWarning(message: String, title: String = "Warning") {
        System.err.println("[$title] $message")
    }
}