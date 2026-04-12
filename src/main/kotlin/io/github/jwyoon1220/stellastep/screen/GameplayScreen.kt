package io.github.jwyoon1220.stellastep.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.jwyoon1220.stellastep.StellarStepGame
import io.github.jwyoon1220.stellastep.model.Chart
import io.github.jwyoon1220.stellastep.model.Judgment
import io.github.jwyoon1220.stellastep.model.Note
import io.github.jwyoon1220.stellastep.model.NoteType
import java.io.File

/**
 * Timing windows in milliseconds (half-window on each side).
 * PERFECT: ±40ms, GREAT: ±70ms, GOOD: ±100ms, BAD: ±150ms, MISS: >150ms
 */
private const val PERFECT_MS = 40L
private const val GREAT_MS   = 70L
private const val GOOD_MS    = 100L
private const val BAD_MS     = 150L

/**
 * Lane layout (all in libGDX screen coordinates, y=0 at bottom):
 *   Screen: 1280×720
 *   4 lanes, each 80px wide → total 320px centered at x=480..800
 *   LANE_NOTE_WIDTH = 76px (4px gap between lanes)
 *   JUDGMENT_LINE_Y = 120px from bottom
 *   LANE_TOP_Y = 680px (notes spawn here)
 */
private const val LANE_COUNT      = 4
private const val LANE_WIDTH      = 80f
private const val LANE_NOTE_W     = 76f   // note.png expected width; see ASSET_SPEC.md
private const val NOTE_HEIGHT     = 20f   // note.png expected height; see ASSET_SPEC.md
private const val JUDGMENT_LINE_Y = 120f
private const val LANE_TOP_Y      = 680f
private const val LANE_START_X    = (1280 - LANE_COUNT * LANE_WIDTH) / 2  // = 480

class GameplayScreen(
    private val game: StellarStepGame,
    private val chart: Chart,
    private val notes: List<Note>,
    private val audioFile: File,
    private val chartDir: File
) : Screen {

    // Audio
    private var music: Music? = null
    private var audioStartTimeNs: Long = 0L
    private var audioStarted = false
    private var countdownTimer = -2000L // 2 second countdown before audio

    /** Returns current song time in ms (may run even before audio starts for countdown) */
    private val currentTimeMs: Long
        get() {
            if (!audioStarted) return (System.nanoTime() - audioStartTimeNs) / 1_000_000L + countdownTimer
            return ((music?.position ?: 0f) * 1000f).toLong() + chart.offsetMs + game.settings.globalOffsetMs
        }

    // Judgment display
    private var lastJudgment = Judgment.NONE
    private var judgeDisplayTimer = 0f

    // Hit effects per lane (timer in seconds)
    private val hitEffectTimers = FloatArray(LANE_COUNT) { 0f }

    // Key state (previous frame)
    private val prevKeyDown = BooleanArray(LANE_COUNT) { false }

    // Score state
    private val judgmentCounts = mutableMapOf(
        Judgment.PERFECT to 0, Judgment.GREAT to 0, Judgment.GOOD to 0,
        Judgment.BAD to 0, Judgment.MISS to 0
    )
    private var combo = 0
    private var maxCombo = 0
    private var score = 0L

    // Scroll speed (px/s) – from settings
    private val scrollSpeed get() = game.settings.scrollSpeed

    // Track whether all notes are finished + audio ended
    private var gameFinished = false
    private var finishTimer = 0f

    override fun show() {
        audioStartTimeNs = System.nanoTime()
        // Load audio if file exists
        if (audioFile.exists()) {
            try {
                music = Gdx.audio.newMusic(Gdx.files.absolute(audioFile.absolutePath))
            } catch (e: Exception) {
                println("Could not load audio: ${e.message}")
            }
        }
        // Start video if chart specifies one
        if (chart.videoFile.isNotBlank() && game.videoPlayer.available) {
            game.videoPlayer.open(chart.videoFile)
            game.videoPlayer.play()
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // ESC to quit
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            music?.stop()
            game.setScreen(MainMenuScreen(game))
            return
        }

        val time = currentTimeMs

        // Start audio after countdown
        if (!audioStarted && time >= 0) {
            audioStarted = true
            music?.play()
        }

        updateJudgments(time, delta)
        updateMissedNotes(time)
        updateFinish(delta)

        if (gameFinished) {
            finishTimer -= delta
            if (finishTimer <= 0f) {
                music?.stop()
                game.setScreen(ResultScreen(game, chart, judgmentCounts, maxCombo, score))
                return
            }
        }

        render(time, delta)
    }

    private fun updateJudgments(time: Long, delta: Float) {
        judgeDisplayTimer -= delta
        for (i in 0 until LANE_COUNT) {
            hitEffectTimers[i] = maxOf(0f, hitEffectTimers[i] - delta)
        }

        val keys = game.settings.keyBindings
        for (lane in 0 until LANE_COUNT) {
            val pressed = Gdx.input.isKeyPressed(keys[lane])
            val justPressed = pressed && !prevKeyDown[lane]
            prevKeyDown[lane] = pressed

            if (!justPressed) continue

            // Find the nearest unjudged note in this lane
            val note = notes.filter { it.lane == lane && it.judgment == Judgment.NONE && it.isVisible }
                .minByOrNull { Math.abs(it.timeMs - time) } ?: continue

            val diff = time - note.timeMs  // positive = late, negative = early
            val absDiff = Math.abs(diff)

            val j = when {
                absDiff <= PERFECT_MS -> Judgment.PERFECT
                absDiff <= GREAT_MS   -> Judgment.GREAT
                absDiff <= GOOD_MS    -> Judgment.GOOD
                absDiff <= BAD_MS     -> Judgment.BAD
                else                  -> Judgment.NONE // too early, don't judge yet
            }

            if (j != Judgment.NONE) {
                note.judgment = j
                judgmentCounts[j] = (judgmentCounts[j] ?: 0) + 1
                lastJudgment = j
                judgeDisplayTimer = 0.6f
                hitEffectTimers[lane] = 0.15f
                if (j != Judgment.BAD) { combo++; if (combo > maxCombo) maxCombo = combo }
                else combo = 0
                score += scoreForJudgment(j) + combo * 10L
            }
        }
    }

    private fun updateMissedNotes(time: Long) {
        for (note in notes) {
            if (note.judgment != Judgment.NONE) continue
            if (time - note.timeMs > BAD_MS) {
                note.judgment = Judgment.MISS
                judgmentCounts[Judgment.MISS] = (judgmentCounts[Judgment.MISS] ?: 0) + 1
                combo = 0
                lastJudgment = Judgment.MISS
                judgeDisplayTimer = 0.4f
            }
        }
    }

    private fun updateFinish(delta: Float) {
        if (gameFinished) return
        val allJudged = notes.all { it.judgment != Judgment.NONE }
        val audioEnded = music?.let { !it.isPlaying && audioStarted } ?: audioStarted
        if (allJudged || (audioEnded && audioStarted)) {
            gameFinished = true
            finishTimer = 1.5f
        }
    }

    private fun render(time: Long, delta: Float) {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        // Draw background (video if available, else static skin)
        game.batch.begin()
        game.batch.setColor(1f, 1f, 1f, 1f)
        if (game.videoPlayer.available && game.videoPlayer.texture != null) {
            game.videoPlayer.update()
            game.batch.draw(game.videoPlayer.texture!!, 0f, 0f, w, h)
        } else {
            game.batch.draw(game.skin["bg"], 0f, 0f, w, h)
        }
        game.batch.end()

        // Draw lanes
        game.batch.begin()
        for (lane in 0 until LANE_COUNT) {
            val laneX = LANE_START_X + lane * LANE_WIDTH
            // lane background
            game.batch.setColor(1f, 1f, 1f, 1f)
            game.batch.draw(game.skin["lane"], laneX + 2f, JUDGMENT_LINE_Y, LANE_WIDTH - 4f, LANE_TOP_Y - JUDGMENT_LINE_Y)

            // key indicator (bottom of lane)
            val keyPressed = Gdx.input.isKeyPressed(game.settings.keyBindings[lane])
            val keyTex = if (keyPressed) game.skin["key_on"] else game.skin["key_off"]
            game.batch.draw(keyTex, laneX + 2f, 0f, LANE_WIDTH - 4f, JUDGMENT_LINE_Y)

            // hit effect
            if (hitEffectTimers[lane] > 0f) {
                val alpha = hitEffectTimers[lane] / 0.15f
                game.batch.setColor(1f, 1f, 1f, alpha)
                game.batch.draw(game.skin["hit_effect"], laneX, JUDGMENT_LINE_Y - 40f, LANE_WIDTH, 80f)
                game.batch.setColor(1f, 1f, 1f, 1f)
            }
        }

        // Draw judgment line
        game.batch.setColor(1f, 1f, 1f, 1f)
        game.batch.draw(game.skin["judgment_line"], LANE_START_X, JUDGMENT_LINE_Y - 2f, LANE_COUNT * LANE_WIDTH, 4f)

        // Draw notes
        for (note in notes) {
            if (note.judgment != Judgment.NONE) continue  // already judged
            val noteY = JUDGMENT_LINE_Y + (note.timeMs - time) * scrollSpeed / 1000f
            if (noteY > h + 50f || noteY < -NOTE_HEIGHT * 2) continue  // off screen

            val noteX = LANE_START_X + note.lane * LANE_WIDTH + 2f

            when (note.type) {
                NoteType.TAP -> {
                    game.batch.setColor(1f, 1f, 1f, 1f)
                    game.batch.draw(game.skin["note"], noteX, noteY, LANE_NOTE_W, NOTE_HEIGHT)
                }
                NoteType.HOLD -> {
                    // hold body
                    val bodyH = note.durationMs * scrollSpeed / 1000f
                    game.batch.setColor(1f, 1f, 1f, 0.8f)
                    game.batch.draw(game.skin["hold_body"], noteX, noteY, LANE_NOTE_W, bodyH)
                    // hold end (head)
                    game.batch.setColor(1f, 1f, 1f, 1f)
                    game.batch.draw(game.skin["hold_end"], noteX, noteY + bodyH, LANE_NOTE_W, NOTE_HEIGHT)
                    game.batch.draw(game.skin["note"], noteX, noteY, LANE_NOTE_W, NOTE_HEIGHT)
                }
            }
        }

        game.batch.end()

        // HUD: score, combo, judgment display
        game.batch.begin()
        game.font.data.setScale(2f)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "SCORE: $score", 20f, h - 20f)
        game.font.draw(game.batch, "COMBO: $combo", 20f, h - 60f)

        // Judgment text
        if (judgeDisplayTimer > 0f) {
            val alpha = (judgeDisplayTimer / 0.6f).coerceIn(0f, 1f)
            game.font.data.setScale(3f)
            game.font.color = when (lastJudgment) {
                Judgment.PERFECT -> Color(1f, 1f, 0.3f, alpha)
                Judgment.GREAT   -> Color(0.3f, 1f, 0.3f, alpha)
                Judgment.GOOD    -> Color(0.3f, 0.7f, 1f, alpha)
                Judgment.BAD     -> Color(1f, 0.5f, 0.2f, alpha)
                Judgment.MISS    -> Color(0.7f, 0.2f, 0.2f, alpha)
                else             -> Color.WHITE
            }
            game.font.draw(game.batch, lastJudgment.name, w / 2 - 80f, h / 2 + 60f)
        }

        // Song info
        game.font.data.setScale(1.5f)
        game.font.color = Color(0.7f, 0.7f, 0.7f, 1f)
        game.font.draw(game.batch, "${chart.title}  -  ${chart.artist}", w / 2 - 200f, h - 20f)

        // ESC hint
        game.font.data.setScale(1.2f)
        game.font.color = Color(0.4f, 0.4f, 0.4f, 1f)
        game.font.draw(game.batch, "ESC: Quit", w - 120f, 30f)

        game.batch.end()
    }

    private fun scoreForJudgment(j: Judgment): Long = when (j) {
        Judgment.PERFECT -> 300L
        Judgment.GREAT   -> 200L
        Judgment.GOOD    -> 100L
        Judgment.BAD     -> 50L
        else             -> 0L
    }

    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() { music?.pause(); game.videoPlayer.stop() }
    override fun dispose() { music?.dispose() }
}
