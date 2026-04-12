package io.github.jwyoon1220.stellastep.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import io.github.jwyoon1220.stellastep.StellarStepGame

/**
 * Chart Editor – Setup Screen.
 *
 * Fields (set via system text-input dialogs on ENTER/double-click):
 *   Title, Artist, BPM, AudioFile, VideoFile (optional)
 *
 * Controls:
 *   UP/DOWN  – move cursor
 *   ENTER    – edit selected field (system dialog)
 *   S / F5   – start recording
 *   ESC      – back to main menu
 */
class ChartEditorSetupScreen(private val game: StellarStepGame) : Screen {

    private var title     = "New Song"
    private var artist    = "Unknown Artist"
    private var bpm       = "120"
    private var audioFile = ""   // relative path, e.g. "assets/songs/mysong/audio.wav"
    private var videoFile = ""   // optional, e.g. "assets/video/myvideo.mp4"

    private val fieldLabels  = listOf("Title", "Artist", "BPM", "Audio File", "Video File (opt.)")
    private val fieldGetters = listOf<() -> String>({ title }, { artist }, { bpm }, { audioFile }, { videoFile })
    private val fieldSetters = listOf<(String) -> Unit>(
        { title = it },
        { artist = it },
        { bpm = it.toIntOrNull()?.coerceIn(1, 960)?.toString() ?: bpm },
        { audioFile = it },
        { videoFile = it }
    )

    private var selectedField = 0
    private var keyTimer = 0f
    private var dialogOpen = false

    override fun show() {}

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(MainMenuScreen(game))
            return
        }

        keyTimer -= delta
        if (keyTimer <= 0f) {
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                selectedField = (selectedField - 1 + fieldLabels.size) % fieldLabels.size
                keyTimer = 0.2f
            } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                selectedField = (selectedField + 1) % fieldLabels.size
                keyTimer = 0.2f
            }
        }

        if (!dialogOpen && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            openInputDialog(selectedField)
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            startRecording()
        }

        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        game.batch.begin()
        game.font.data.setScale(3f)
        game.font.color = Color(0.4f, 0.8f, 1f, 1f)
        game.font.draw(game.batch, "CHART EDITOR", 60f, h - 40f)

        game.font.data.setScale(1.8f)
        game.font.color = Color(0.7f, 0.7f, 0.7f, 1f)
        game.font.draw(game.batch, "Set metadata then press S to start recording", 60f, h - 90f)

        for ((i, label) in fieldLabels.withIndex()) {
            val value   = fieldGetters[i]()
            val isSelected = i == selectedField
            game.font.data.setScale(2f)
            game.font.color = if (isSelected) Color.YELLOW else Color.WHITE
            val prefix = if (isSelected) "> " else "  "
            val displayValue = if (value.isEmpty()) "(empty - press ENTER to edit)" else value
            game.font.draw(game.batch, "$prefix$label: $displayValue", 60f, h - 180f - i * 70f)
        }

        game.font.data.setScale(1.6f)
        game.font.color = Color(0.5f, 0.5f, 0.7f, 1f)
        val hints = listOf(
            "UP/DOWN: select field   ENTER: edit field",
            "S / F5 : start recording   ESC: back"
        )
        for ((i, hint) in hints.withIndex()) {
            game.font.draw(game.batch, hint, 60f, 100f - i * 40f)
        }
        game.batch.end()
    }

    private fun openInputDialog(fieldIndex: Int) {
        dialogOpen = true
        val current = fieldGetters[fieldIndex]()
        Gdx.input.getTextInput(object : Input.TextInputListener {
            override fun input(text: String) {
                fieldSetters[fieldIndex](text.trim())
                dialogOpen = false
            }
            override fun canceled() { dialogOpen = false }
        }, "Edit: ${fieldLabels[fieldIndex]}", current, "")
    }

    private fun startRecording() {
        val bpmInt = bpm.toIntOrNull()?.coerceIn(1, 960) ?: 120
        if (audioFile.isBlank()) {
            // Show dialog to enter audio file
            openInputDialog(3)
            return
        }
        game.setScreen(ChartRecordingScreen(game, title, artist, bpmInt, audioFile, videoFile))
    }

    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {}
}
