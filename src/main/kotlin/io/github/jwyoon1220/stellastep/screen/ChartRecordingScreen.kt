package io.github.jwyoon1220.stellastep.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import io.github.jwyoon1220.stellastep.StellarStepGame
import io.github.jwyoon1220.stellastep.io.ChartWriter
import io.github.jwyoon1220.stellastep.model.Chart
import java.io.File

/**
 * Chart Recording Screen – 채보도구 핵심 화면.
 *
 * Flow:
 *   COUNTDOWN (3 seconds) → RECORDING (audio plays) → FINISHED → auto-save
 *
 * During RECORDING:
 *   D/F/J/K  → record note in lanes 0-3
 *   Key hold → produces a hold note (duration = release time – press time)
 *   R        → restart from beginning
 *   ESC      → cancel and return to setup screen
 *
 * After audio ends:
 *   Automatically normalizes and saves chart.json to assets/songs/<sanitizedTitle>/
 *   Then navigates to ChartEditorSaveScreen.
 */
/** Minimum key-hold duration in ms to be recorded as a hold note; shorter presses become taps. */
private const val MIN_HOLD_MS = 80L

class ChartRecordingScreen(
    private val game:       StellarStepGame,
    private val songTitle:  String,
    private val artist:     String,
    private val bpm:        Int,
    private val audioPath:  String,
    private val videoPath:  String
) : Screen {

    private enum class State { COUNTDOWN, RECORDING, FINISHED }
    private var state = State.COUNTDOWN

    // Countdown
    private var countdownSec = 3f

    // Audio
    private var music: Music? = null
    private var audioStarted = false

    // Time tracking
    private var audioStartTimeNs = 0L

    /** Current time in ms: negative during countdown, then audio position. */
    private val currentTimeMs: Long get() = when {
        state == State.COUNTDOWN -> -(countdownSec * 1000f).toLong()
        !audioStarted            -> 0L
        else                     -> ((music?.position ?: 0f) * 1000f).toLong() + game.settings.globalOffsetMs
    }

    // Key state
    private val LANE_COUNT = 4
    private val prevKeyDown = BooleanArray(LANE_COUNT)
    // For hold detection: when was each lane key pressed down (ms)
    private val keyDownTime = LongArray(LANE_COUNT) { -1L }

    // Raw recorded notes: (pressTimeMs, lane, durationMs)
    private val rawNotes = mutableListOf<Triple<Long, Int, Long>>()

    // Key flash timers for visual feedback
    private val keyFlashTimers = FloatArray(LANE_COUNT)

    // Lane layout constants (matching GameplayScreen)
    private val LANE_WIDTH   = 80f
    private val LANE_START_X = (1280 - LANE_COUNT * LANE_WIDTH) / 2f  // 480
    private val JUDGE_Y      = 120f

    // Finished state
    private var savedFile: File? = null
    private var finishTimer = 0f

    override fun show() {
        audioStartTimeNs = System.nanoTime()
        val audioFile = File(audioPath)
        if (audioFile.exists()) {
            try {
                music = Gdx.audio.newMusic(Gdx.files.absolute(audioFile.absolutePath))
            } catch (e: Exception) {
                println("ChartRecordingScreen: could not load audio: ${e.message}")
            }
        }
        // Video (optional)
        if (videoPath.isNotBlank() && game.videoPlayer.available) {
            game.videoPlayer.open(videoPath)
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            cleanup()
            game.setScreen(ChartEditorSetupScreen(game))
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restart()
            return
        }

        when (state) {
            State.COUNTDOWN -> updateCountdown(delta)
            State.RECORDING -> updateRecording(delta)
            State.FINISHED  -> {
                finishTimer -= delta
                if (finishTimer <= 0f) {
                    navigateToSave()
                }
            }
        }

        draw(delta)
    }

    private fun updateCountdown(delta: Float) {
        countdownSec -= delta
        if (countdownSec <= 0f) {
            state = State.RECORDING
            audioStarted = true
            music?.play()
            if (videoPath.isNotBlank() && game.videoPlayer.available) game.videoPlayer.play()
        }
    }

    private fun updateRecording(delta: Float) {
        // Check if audio finished
        val musicEnded = music?.let { !it.isPlaying && audioStarted } ?: false
        if (musicEnded) {
            // Release any still-held keys
            val time = currentTimeMs
            for (lane in 0 until LANE_COUNT) {
                if (keyDownTime[lane] >= 0) {
                    val dur = time - keyDownTime[lane]
                    rawNotes += Triple(keyDownTime[lane], lane, dur.coerceAtLeast(0L))
                    keyDownTime[lane] = -1L
                }
            }
            finishAndSave()
            return
        }

        val time = currentTimeMs
        val keys = game.settings.keyBindings

        for (lane in 0 until LANE_COUNT) {
            keyFlashTimers[lane] = maxOf(0f, keyFlashTimers[lane] - delta)
            val pressed = Gdx.input.isKeyPressed(keys[lane])

            if (pressed && !prevKeyDown[lane]) {
                // Key just pressed
                keyDownTime[lane] = time
                keyFlashTimers[lane] = 0.15f
            } else if (!pressed && prevKeyDown[lane]) {
                // Key just released
                val pressTime = keyDownTime[lane]
                if (pressTime >= 0) {
                    val dur = time - pressTime
                    rawNotes += Triple(pressTime, lane, if (dur > MIN_HOLD_MS) dur else 0L)
                    keyDownTime[lane] = -1L
                }
            }
            prevKeyDown[lane] = pressed
        }
    }

    private fun finishAndSave() {
        if (state == State.FINISHED) return
        state = State.FINISHED
        finishTimer = 1.5f
        game.videoPlayer.stop()

        val sanitized = songTitle.replace(Regex("[^a-zA-Z0-9가-힣._\\- ]"), "_").trim().replace(' ', '_').lowercase()
        val outDir    = File("assets/songs/$sanitized")
        val chartMeta = Chart(
            title     = songTitle,
            artist    = artist,
            bpm       = bpm,
            audioFile = File(audioPath).name,
            offsetMs  = game.settings.globalOffsetMs,
            videoFile = videoPath
        )
        try {
            savedFile = ChartWriter.write(rawNotes, chartMeta, outDir)
            // Copy audio file into the song dir if not already there
            val audioSrc = File(audioPath)
            val audioDst = File(outDir, audioSrc.name)
            if (audioSrc.exists() && !audioDst.exists()) audioSrc.copyTo(audioDst)
        } catch (e: Exception) {
            println("ChartRecordingScreen: save failed: ${e.message}")
        }
    }

    private fun navigateToSave() {
        cleanup()
        game.setScreen(ChartEditorSaveScreen(game, savedFile, rawNotes.size))
    }

    private fun restart() {
        rawNotes.clear()
        prevKeyDown.fill(false)
        keyDownTime.fill(-1L)
        countdownSec = 3f
        state = State.COUNTDOWN
        audioStarted = false
        music?.stop()
        music?.dispose()
        music = null
        game.videoPlayer.stop()
        show()
    }

    private fun cleanup() {
        music?.stop()
        music?.dispose()
        music = null
        game.videoPlayer.stop()
    }

    private fun draw(delta: Float) {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        // Background / video
        game.batch.begin()
        if (game.videoPlayer.available && game.videoPlayer.texture != null) {
            game.videoPlayer.update()
            game.batch.draw(game.videoPlayer.texture!!, 0f, 0f, w, h)
        } else {
            game.batch.draw(game.skin["bg"], 0f, 0f, w, h)
        }

        // Draw 4 lanes
        for (lane in 0 until LANE_COUNT) {
            val lx = LANE_START_X + lane * LANE_WIDTH
            val keyPressed = Gdx.input.isKeyPressed(game.settings.keyBindings[lane])
            val keyTex = if (keyPressed) game.skin["key_on"] else game.skin["key_off"]
            game.batch.draw(game.skin["lane"], lx + 2f, JUDGE_Y, LANE_WIDTH - 4f, h - JUDGE_Y - 40f)
            game.batch.draw(keyTex, lx + 2f, 0f, LANE_WIDTH - 4f, JUDGE_Y)
            if (keyFlashTimers[lane] > 0f) {
                val alpha = keyFlashTimers[lane] / 0.15f
                game.batch.setColor(1f, 1f, 1f, alpha)
                game.batch.draw(game.skin["hit_effect"], lx, JUDGE_Y - 40f, LANE_WIDTH, 80f)
                game.batch.setColor(1f, 1f, 1f, 1f)
            }
        }
        // Judgment line
        game.batch.draw(game.skin["judgment_line"], LANE_START_X, JUDGE_Y - 2f, LANE_COUNT * LANE_WIDTH, 4f)
        game.batch.end()

        // HUD
        game.batch.begin()
        when (state) {
            State.COUNTDOWN -> {
                val countNum = countdownSec.toInt() + 1
                game.font.data.setScale(8f)
                game.font.color = Color(1f, 1f, 0.3f, 1f)
                game.font.draw(game.batch, "$countNum", w / 2 - 40f, h / 2 + 80f)
                game.font.data.setScale(2f)
                game.font.color = Color.WHITE
                game.font.draw(game.batch, "Get ready!", w / 2 - 80f, h / 2 - 20f)
            }
            State.RECORDING -> {
                val timeMs = currentTimeMs
                val secStr = "%.1f".format(timeMs / 1000.0)
                game.font.data.setScale(2f)
                game.font.color = Color(1f, 0.4f, 0.4f, 1f)
                game.font.draw(game.batch, "● REC  ${secStr}s", 20f, h - 20f)
                game.font.data.setScale(1.8f)
                game.font.color = Color.WHITE
                game.font.draw(game.batch, "Notes recorded: ${rawNotes.size}", 20f, h - 60f)
                game.font.color = Color(0.5f, 0.5f, 0.5f, 1f)
                game.font.draw(game.batch, "R: Restart   ESC: Cancel", w - 280f, 30f)
            }
            State.FINISHED -> {
                game.font.data.setScale(3f)
                game.font.color = Color(0.3f, 1f, 0.3f, 1f)
                game.font.draw(game.batch, "Saving...", w / 2 - 120f, h / 2 + 60f)
                game.font.data.setScale(2f)
                game.font.color = Color.WHITE
                game.font.draw(game.batch, "Recorded ${rawNotes.size} notes", w / 2 - 160f, h / 2 - 10f)
            }
        }

        // Song info at top
        game.font.data.setScale(1.5f)
        game.font.color = Color(0.7f, 0.7f, 0.7f, 1f)
        game.font.draw(game.batch, "$songTitle  -  $artist  (${bpm} BPM)", w / 2 - 260f, h - 20f)

        game.batch.end()
    }

    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() { music?.pause() }
    override fun dispose() { music?.dispose() }
}
