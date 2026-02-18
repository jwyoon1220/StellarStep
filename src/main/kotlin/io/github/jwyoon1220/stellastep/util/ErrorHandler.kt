package io.github.jwyoon1220.stellastep.util

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.win32.W32APIOptions
import kotlin.system.exitProcess

private interface User32 : Library {

    companion object {
        val INSTANCE: User32 = Native.load(
            "user32",
            User32::class.java,
            W32APIOptions.DEFAULT_OPTIONS
        )
    }

    fun MessageBoxW(
        hWnd: Int,
        lpText: String,
        lpCaption: String,
        uType: Int
    ): Int
}

object ErrorHandler {

    private const val MB_OK = 0x00000000
    private const val MB_ICONERROR = 0x00000010
    private const val MB_ICONWARNING = 0x00000030

    fun handleError(
        e: Throwable,
        exit: Boolean = false,
        exitCode: Int = 1,
        title: String = "StellaStep 치명적 오류"
    ) {
        println(e.message)
        e.printStackTrace()

        User32.INSTANCE.MessageBoxW(
            0,
            e.message ?: "An unknown error occurred.",
            title,
            MB_OK or MB_ICONERROR
        )

        if (exit) {
            exitProcess(exitCode)
        }
    }

    fun handleWarning(
        message: String,
        title: String = "경고"
    ) {
        println("Warning: $message")

        User32.INSTANCE.MessageBoxW(
            0,
            message,
            title,
            MB_OK or MB_ICONWARNING
        )
    }
}