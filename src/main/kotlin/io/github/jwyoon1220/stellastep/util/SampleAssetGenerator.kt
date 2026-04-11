package io.github.jwyoon1220.stellastep.util

import java.io.File
import java.awt.image.BufferedImage
import java.awt.Color
import javax.imageio.ImageIO

/**
 * Generates placeholder skin assets and a sample chart on first run.
 * All PNG files are 1×1 solid color placeholders – replace with real art as per ASSET_SPEC.md.
 */
object SampleAssetGenerator {

    fun generateIfNeeded() {
        generatePlaceholderSkin()
        generateSampleChart()
        generateSampleAudio()
    }

    // ── Placeholder skin PNGs ─────────────────────────────────────────────
    // asset key → RGB color for the placeholder (1×1 pixel)
    // Replace these files with proper artwork; see ASSET_SPEC.md for specs.
    private val skinAssets = mapOf(
        "bg"            to Color(13, 13, 26),     // very dark navy – bg.png (1280×720)
        "lane"          to Color(38, 38, 64),      // dark blue – lane.png (80×600)
        "note"          to Color(51, 204, 255),    // cyan – note.png (76×20)
        "hold_body"     to Color(26, 153, 230),    // light blue – hold_body.png (76×1 tiled)
        "hold_end"      to Color(0, 230, 230),     // teal – hold_end.png (76×20)
        "key_on"        to Color(255, 255, 77),    // yellow – key_on.png (80×120)
        "key_off"       to Color(77, 77, 102),     // slate – key_off.png (80×120)
        "hit_effect"    to Color(255, 255, 255),   // white – hit_effect.png (80×80)
        "judgment_line" to Color(255, 255, 255)    // white – judgment_line.png (320×4)
    )

    private fun generatePlaceholderSkin() {
        val dir = File("assets/skins/default")
        dir.mkdirs()
        for ((key, color) in skinAssets) {
            val file = File(dir, "$key.png")
            if (!file.exists()) {
                writeSolidColorPng(file, 1, 1, color)
            }
        }
        // Also generate icon placeholder
        val iconFile = File("assets/imgs/icon.png")
        if (!iconFile.exists()) {
            File("assets/imgs").mkdirs()
            writeSolidColorPng(iconFile, 16, 16, Color(51, 204, 255))
        }
    }

    private fun writeSolidColorPng(file: File, w: Int, h: Int, color: Color) {
        try {
            val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            val g = img.createGraphics()
            g.color = color
            g.fillRect(0, 0, w, h)
            g.dispose()
            ImageIO.write(img, "PNG", file)
        } catch (e: Exception) {
            println("Warning: could not write placeholder ${file.name}: ${e.message}")
        }
    }

    // ── Sample chart ──────────────────────────────────────────────────────
    private fun generateSampleChart() {
        val dir = File("assets/songs/sample")
        dir.mkdirs()
        val chartFile = File(dir, "chart.json")
        if (!chartFile.exists()) {
            chartFile.writeText(SAMPLE_CHART_JSON, Charsets.UTF_8)
        }
        // Write a README in the song folder
        val readmeFile = File(dir, "README.md")
        if (!readmeFile.exists()) {
            readmeFile.writeText("""
# Sample Song

Place an audio file here named `audio.ogg` (or `audio.wav`).
The chart file (`chart.json`) references `audioFile: "audio.wav"`.

If no audio file is found the game will attempt `audio.wav`.
If neither is found, gameplay still runs (timing uses system clock).

**Audio format:** OGG Vorbis or WAV, 44100 Hz stereo recommended.
""".trimIndent(), Charsets.UTF_8)
        }
    }

    // ── Sample audio stub (silent WAV, ~30 seconds) ────────────────────────
    private fun generateSampleAudio() {
        val dir = File("assets/songs/sample")
        val wavFile = File(dir, "audio.wav")
        if (!wavFile.exists()) {
            try {
                writeSilentWav(wavFile, sampleRate = 44100, durationSec = 30)
            } catch (e: Exception) {
                println("Warning: could not generate sample audio: ${e.message}")
            }
        }
    }

    private fun writeSilentWav(file: File, sampleRate: Int, durationSec: Int) {
        val numSamples = sampleRate * durationSec * 2  // stereo
        val dataSize = numSamples * 2  // 16-bit
        val header = buildWavHeader(dataSize, sampleRate, channels = 2)
        file.outputStream().use { out ->
            out.write(header)
            // Write silent 16-bit PCM data in chunks
            val chunk = ByteArray(4096)
            var remaining = dataSize
            while (remaining > 0) {
                val toWrite = minOf(chunk.size, remaining)
                out.write(chunk, 0, toWrite)
                remaining -= toWrite
            }
        }
    }

    private fun buildWavHeader(dataSize: Int, sampleRate: Int, channels: Int): ByteArray {
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val h = ByteArray(44)
        fun wi(off: Int, v: Int) { h[off]=(v and 0xFF).toByte(); h[off+1]=((v shr 8) and 0xFF).toByte(); h[off+2]=((v shr 16) and 0xFF).toByte(); h[off+3]=((v shr 24) and 0xFF).toByte() }
        fun ws(off: Int, v: Short) { h[off]=(v.toInt() and 0xFF).toByte(); h[off+1]=((v.toInt() shr 8) and 0xFF).toByte() }
        "RIFF".toByteArray().copyInto(h, 0); wi(4, 36 + dataSize); "WAVE".toByteArray().copyInto(h, 8)
        "fmt ".toByteArray().copyInto(h, 12); wi(16, 16); ws(20, 1); ws(22, channels.toShort())
        wi(24, sampleRate); wi(28, byteRate); ws(32, blockAlign); ws(34, bitsPerSample.toShort())
        "data".toByteArray().copyInto(h, 36); wi(40, dataSize)
        return h
    }

    // Sample chart: 4-key pattern, notes at 120 BPM (~500ms intervals) for ~20 seconds
    // time values are in ms from audio start
    private val SAMPLE_CHART_JSON = """
{
  "title": "Sample Song",
  "artist": "StellarStep",
  "bpm": 120,
  "audioFile": "audio.wav",
  "offsetMs": 0,
  "notes": [
    {"time": 1000, "lane": 0, "type": "tap"},
    {"time": 1500, "lane": 1, "type": "tap"},
    {"time": 2000, "lane": 2, "type": "tap"},
    {"time": 2500, "lane": 3, "type": "tap"},
    {"time": 3000, "lane": 0, "type": "tap"},
    {"time": 3000, "lane": 2, "type": "tap"},
    {"time": 3500, "lane": 1, "type": "tap"},
    {"time": 3500, "lane": 3, "type": "tap"},
    {"time": 4000, "lane": 0, "type": "hold", "duration": 500},
    {"time": 4000, "lane": 3, "type": "tap"},
    {"time": 4500, "lane": 1, "type": "tap"},
    {"time": 4500, "lane": 2, "type": "tap"},
    {"time": 5000, "lane": 0, "type": "tap"},
    {"time": 5000, "lane": 3, "type": "tap"},
    {"time": 5500, "lane": 1, "type": "tap"},
    {"time": 5500, "lane": 2, "type": "tap"},
    {"time": 6000, "lane": 0, "type": "tap"},
    {"time": 6250, "lane": 1, "type": "tap"},
    {"time": 6500, "lane": 2, "type": "tap"},
    {"time": 6750, "lane": 3, "type": "tap"},
    {"time": 7000, "lane": 0, "type": "tap"},
    {"time": 7000, "lane": 2, "type": "tap"},
    {"time": 7500, "lane": 1, "type": "tap"},
    {"time": 7500, "lane": 3, "type": "tap"},
    {"time": 8000, "lane": 0, "type": "hold", "duration": 1000},
    {"time": 8000, "lane": 1, "type": "tap"},
    {"time": 8500, "lane": 2, "type": "tap"},
    {"time": 8500, "lane": 3, "type": "tap"},
    {"time": 9000, "lane": 1, "type": "tap"},
    {"time": 9000, "lane": 2, "type": "tap"},
    {"time": 9500, "lane": 0, "type": "tap"},
    {"time": 9500, "lane": 3, "type": "tap"},
    {"time": 10000, "lane": 0, "type": "tap"},
    {"time": 10000, "lane": 2, "type": "tap"},
    {"time": 10500, "lane": 1, "type": "tap"},
    {"time": 10500, "lane": 3, "type": "tap"},
    {"time": 11000, "lane": 0, "type": "tap"},
    {"time": 11250, "lane": 1, "type": "tap"},
    {"time": 11500, "lane": 2, "type": "tap"},
    {"time": 11750, "lane": 3, "type": "tap"},
    {"time": 12000, "lane": 0, "type": "hold", "duration": 2000},
    {"time": 12000, "lane": 3, "type": "hold", "duration": 2000},
    {"time": 12500, "lane": 1, "type": "tap"},
    {"time": 12500, "lane": 2, "type": "tap"},
    {"time": 13000, "lane": 1, "type": "tap"},
    {"time": 13000, "lane": 2, "type": "tap"},
    {"time": 13500, "lane": 1, "type": "tap"},
    {"time": 13500, "lane": 2, "type": "tap"},
    {"time": 14000, "lane": 0, "type": "tap"},
    {"time": 14000, "lane": 3, "type": "tap"},
    {"time": 14500, "lane": 1, "type": "tap"},
    {"time": 14500, "lane": 2, "type": "tap"},
    {"time": 15000, "lane": 0, "type": "tap"},
    {"time": 15500, "lane": 1, "type": "tap"},
    {"time": 16000, "lane": 2, "type": "tap"},
    {"time": 16500, "lane": 3, "type": "tap"},
    {"time": 17000, "lane": 0, "type": "tap"},
    {"time": 17000, "lane": 1, "type": "tap"},
    {"time": 17000, "lane": 2, "type": "tap"},
    {"time": 17000, "lane": 3, "type": "tap"},
    {"time": 18000, "lane": 0, "type": "tap"},
    {"time": 18250, "lane": 1, "type": "tap"},
    {"time": 18500, "lane": 2, "type": "tap"},
    {"time": 18750, "lane": 3, "type": "tap"},
    {"time": 19000, "lane": 0, "type": "tap"},
    {"time": 19000, "lane": 3, "type": "tap"},
    {"time": 19500, "lane": 1, "type": "tap"},
    {"time": 19500, "lane": 2, "type": "tap"},
    {"time": 20000, "lane": 0, "type": "hold", "duration": 500},
    {"time": 20000, "lane": 1, "type": "hold", "duration": 500},
    {"time": 20000, "lane": 2, "type": "hold", "duration": 500},
    {"time": 20000, "lane": 3, "type": "hold", "duration": 500}
  ]
}
""".trimIndent()
}
