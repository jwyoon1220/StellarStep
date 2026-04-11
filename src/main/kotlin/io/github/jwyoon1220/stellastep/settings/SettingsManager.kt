package io.github.jwyoon1220.stellastep.settings

import com.google.gson.Gson
import io.github.jwyoon1220.stellastep.model.GameSettings
import java.io.File

class SettingsManager {
    private val gson = Gson()
    private val settingsFile: File = File(
        System.getProperty("user.home"), ".stellarstep${File.separator}settings.json"
    )

    var settings = GameSettings()
        private set

    fun load() {
        if (settingsFile.exists()) {
            try {
                settings = gson.fromJson(settingsFile.readText(Charsets.UTF_8), GameSettings::class.java)
                    ?: GameSettings()
            } catch (_: Exception) {
                settings = GameSettings()
            }
        }
    }

    fun save() {
        settingsFile.parentFile?.mkdirs()
        settingsFile.writeText(gson.toJson(settings), Charsets.UTF_8)
    }

    // Convenience delegates
    var globalOffsetMs: Long
        get() = settings.globalOffsetMs
        set(v) { settings.globalOffsetMs = v; save() }

    var scrollSpeed: Float
        get() = settings.scrollSpeed
        set(v) { settings.scrollSpeed = v; save() }

    var keyBindings: IntArray
        get() = settings.keyBindings
        set(v) { settings.keyBindings = v; save() }
}
