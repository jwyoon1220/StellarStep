package io.github.jwyoon1220.stellastep

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import io.github.jwyoon1220.stellastep.util.SampleAssetGenerator

fun main() {
    // Generate placeholder assets and sample data if they don't exist yet
    SampleAssetGenerator.generateIfNeeded()

    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("StellarStep")
        setWindowedMode(1280, 720)
        setResizable(false)
        useVsync(true)
        setForegroundFPS(60)
        setWindowIcon("assets/imgs/icon.png") // uses fallback if missing
    }
    Lwjgl3Application(StellarStepGame(), config)
}
