package io.github.jwyoon1220.stellastep.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import io.github.jwyoon1220.stellastep.StellarStepGame

/**
 * Calibration screen.
 * - Shows current global offset in ms
 * - Plays a metronome beat at 120 BPM (every 500 ms)
 * - User taps SPACE to measure timing difference
 * - LEFT/RIGHT arrows adjust offset by 1ms; SHIFT+LEFT/RIGHT by 10ms
 * - S key saves; ESC returns to main menu
 *
 * Asset used: assets/sound/hihat.mp3 (fallback: simple beep via javax.sound.sampled)
 */
class CalibrationScreen(private val game: StellarStepGame) : Screen {
    private var metronomeTimer = 0f
    private val beatIntervalSec = 0.5f  // 120 BPM
    private var beatCount = 0

    // Recent tap deltas for display (difference between tap time and beat time)
    private val recentDeltas = ArrayDeque<Long>()
    private val maxDeltas = 8
    private var lastBeatTimeMs = 0L
    private var startTimeMs = 0L

    private var hihatSound: Sound? = null
    private var beepSound: Sound? = null

    // We generate a simple beep internally as fallback
    private var usingFallback = false

    override fun show() {
        startTimeMs = System.currentTimeMillis()
        metronomeTimer = 0f
        // Try loading hihat
        val hihatFile = Gdx.files.internal("assets/sound/hihat.mp3")
        if (hihatFile.exists()) {
            try { hihatSound = Gdx.audio.newSound(hihatFile) } catch (_: Exception) { }
        }
        if (hihatSound == null) {
            usingFallback = true
            // Generate beep sound
            beepSound = generateBeepSound()
        }
    }

    /** Generate a simple 440 Hz beep as a libGDX Sound using PCM data */
    private fun generateBeepSound(): Sound? {
        return try {
            val sampleRate = 44100
            val durationMs = 80
            val buffer = javax.sound.sampled.AudioSystem.getAudioInputStream(
                generateBeepWav(440, sampleRate, durationMs)
            )
            val tmp = java.io.File(System.getProperty("java.io.tmpdir"), "stellarstep_beep.wav")
            buffer.use { javax.sound.sampled.AudioSystem.write(it, javax.sound.sampled.AudioFileFormat.Type.WAVE, tmp) }
            Gdx.audio.newSound(Gdx.files.absolute(tmp.absolutePath))
        } catch (e: Exception) {
            println("Could not generate beep: ${e.message}")
            null
        }
    }

    private fun generateBeepWav(freqHz: Int, sampleRate: Int, durationMs: Int): java.io.InputStream {
        val numSamples = sampleRate * durationMs / 1000
        val buffer = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = if (i < numSamples / 4) i.toDouble() / (numSamples / 4) else
                           if (i > numSamples * 3 / 4) (numSamples - i).toDouble() / (numSamples / 4) else 1.0
            val sample = (Math.sin(2 * Math.PI * freqHz * t) * 28000 * envelope).toInt().toShort()
            buffer[i * 2]     = (sample.toInt() and 0xFF).toByte()
            buffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        // Build WAV in memory
        val header = buildWavHeader(buffer.size, sampleRate)
        return java.io.ByteArrayInputStream(header + buffer)
    }

    private fun buildWavHeader(dataSize: Int, sampleRate: Int): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val h = ByteArray(44)
        fun writeInt(arr: ByteArray, off: Int, v: Int) {
            arr[off]   = (v and 0xFF).toByte()
            arr[off+1] = ((v shr 8) and 0xFF).toByte()
            arr[off+2] = ((v shr 16) and 0xFF).toByte()
            arr[off+3] = ((v shr 24) and 0xFF).toByte()
        }
        fun writeShort(arr: ByteArray, off: Int, v: Short) {
            arr[off]   = (v.toInt() and 0xFF).toByte()
            arr[off+1] = ((v.toInt() shr 8) and 0xFF).toByte()
        }
        "RIFF".toByteArray().copyInto(h, 0)
        writeInt(h, 4, 36 + dataSize)
        "WAVE".toByteArray().copyInto(h, 8)
        "fmt ".toByteArray().copyInto(h, 12)
        writeInt(h, 16, 16)          // subchunk1 size
        writeShort(h, 20, 1)         // PCM
        writeShort(h, 22, channels.toShort())
        writeInt(h, 24, sampleRate)
        writeInt(h, 28, byteRate)
        writeShort(h, 32, blockAlign)
        writeShort(h, 34, bitsPerSample.toShort())
        "data".toByteArray().copyInto(h, 36)
        writeInt(h, 40, dataSize)
        return h
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(MainMenuScreen(game))
            return
        }

        // Metronome tick
        metronomeTimer += delta
        if (metronomeTimer >= beatIntervalSec) {
            metronomeTimer -= beatIntervalSec
            lastBeatTimeMs = System.currentTimeMillis()
            beatCount++
            hihatSound?.play(1f) ?: beepSound?.play(1f)
        }

        // Offset adjustment
        val shiftHeld = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
        val step = if (shiftHeld) 10L else 1L
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            game.settings.globalOffsetMs += step
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            game.settings.globalOffsetMs -= step
        }

        // Tap key → measure offset
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            val now = System.currentTimeMillis()
            // Calculate where we are in the beat cycle
            val timeSinceBeat = now - lastBeatTimeMs
            val halfBeat = (beatIntervalSec * 500).toLong()
            // Normalize: if timeSinceBeat > halfBeat, the next beat is closer
            val tapDelta = if (timeSinceBeat > halfBeat) timeSinceBeat - halfBeat * 2 else timeSinceBeat
            recentDeltas.addFirst(tapDelta)
            if (recentDeltas.size > maxDeltas) recentDeltas.removeLast()
        }

        // Auto-apply average delta on A key
        if (Gdx.input.isKeyJustPressed(Input.Keys.A) && recentDeltas.isNotEmpty()) {
            val avg = recentDeltas.average().toLong()
            game.settings.globalOffsetMs -= avg
            recentDeltas.clear()
        }

        // Draw UI
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        game.batch.begin()
        game.font.data.setScale(3f)
        game.font.color = Color(0.4f, 0.8f, 1f, 1f)
        game.font.draw(game.batch, "CALIBRATION", 80f, h - 40f)

        game.font.data.setScale(2.5f)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "Global Offset: ${game.settings.globalOffsetMs} ms", 80f, h - 110f)

        // Beat indicator
        val beatFraction = metronomeTimer / beatIntervalSec
        val beatAlpha = 1f - beatFraction
        game.font.data.setScale(2f)
        game.font.color = Color(1f, 1f, 0.3f, beatAlpha.coerceIn(0f, 1f))
        game.font.draw(game.batch, "● BEAT", 80f, h - 180f)

        game.font.data.setScale(2f)
        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "Recent tap deltas (ms):", 80f, h - 260f)
        game.font.data.setScale(1.8f)
        if (recentDeltas.isEmpty()) {
            game.font.color = Color.GRAY
            game.font.draw(game.batch, "(tap SPACE to the beat)", 80f, h - 310f)
        } else {
            for ((i, d) in recentDeltas.withIndex()) {
                game.font.color = when {
                    Math.abs(d) <= 10 -> Color.GREEN
                    Math.abs(d) <= 30 -> Color.YELLOW
                    else              -> Color.RED
                }
                game.font.draw(game.batch, "${if (d > 0) "+" else ""}${d}ms", 80f + i * 130f, h - 310f)
            }
            val avg = recentDeltas.average().toLong()
            game.font.data.setScale(2f)
            game.font.color = Color.WHITE
            game.font.draw(game.batch, "Average: ${if (avg > 0) "+" else ""}${avg}ms", 80f, h - 380f)
        }

        game.font.data.setScale(1.8f)
        game.font.color = Color(0.5f, 0.5f, 0.7f, 1f)
        val hints = listOf(
            "LEFT / RIGHT    : offset -1 / +1 ms",
            "SHIFT+LEFT/RIGHT: offset -10 / +10 ms",
            "SPACE           : tap to beat (measure offset)",
            "A               : auto-apply average tap delta",
            "ESC             : back to menu (auto-saved)"
        )
        for ((i, hint) in hints.withIndex()) {
            game.font.draw(game.batch, hint, 80f, 280f - i * 45f)
        }
        game.batch.end()
    }

    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {
        hihatSound?.dispose()
        beepSound?.dispose()
    }
    override fun dispose() {}
}
