package io.github.jwyoon1220.stellastep.io

import com.google.gson.GsonBuilder
import io.github.jwyoon1220.stellastep.model.Chart
import io.github.jwyoon1220.stellastep.model.NoteData
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Normalizes recorded raw notes and writes a chart.json file.
 *
 * Normalization steps:
 *  1. Sort notes by time.
 *  2. Quantize each note's time and duration to the nearest beat subdivision
 *     (tries 1/4, 1/8, 1/16, 1/32 note; picks whichever grid minimises error,
 *      only snaps if the error is ≤ half a 1/32-note interval).
 *  3. Remove exact duplicates (same time + lane).
 *  4. Clamp lane to 0..3, clamp time ≥ 0.
 */
object ChartWriter {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * @param rawNotes  list of (timeMs, lane, durationMs) triples, unsorted, raw from recording
     * @param chart     metadata (title, artist, bpm, audioFile, offsetMs, videoFile)
     * @param outputDir directory where chart.json will be written
     * @return the written File
     */
    fun write(
        rawNotes: List<Triple<Long, Int, Long>>,   // (timeMs, lane, durationMs)
        chart: Chart,
        outputDir: File
    ): File {
        val bpm = chart.bpm.coerceAtLeast(1)
        val beatMs = 60_000.0 / bpm

        // Subdivisions to try: whole, half, quarter, eighth, sixteenth, thirty-second, sixty-fourth
        val subdivisions = listOf(1.0, 0.5, 0.25, 0.125, 0.0625, 0.03125, 0.015625)
        val snapTolerance = beatMs * 0.03125 / 2.0  // half a 1/64 note

        fun quantize(rawMs: Long): Long {
            if (rawMs < 0) return 0L
            var bestSnapped = rawMs
            var bestError   = Double.MAX_VALUE
            for (sub in subdivisions) {
                val gridMs  = beatMs * sub
                val snapped = (rawMs / gridMs).roundToLong() * gridMs
                val error   = abs(rawMs - snapped)
                if (error < bestError) {
                    bestError   = error
                    bestSnapped = snapped.roundToLong()
                }
            }
            // Only snap if within tolerance
            return if (abs(rawMs - bestSnapped) <= snapTolerance) bestSnapped else rawMs
        }

        // 1. Build NoteData list with quantized times
        val notes = rawNotes.map { (time, lane, dur) ->
            NoteData(
                time     = quantize(time),
                lane     = lane.coerceIn(0, 3),
                type     = if (dur > 0L) "hold" else "tap",
                duration = if (dur > 0L) quantize(dur).coerceAtLeast(1L) else 0L
            )
        }

        // 2. Sort by time, then lane
        val sorted = notes.sortedWith(compareBy({ it.time }, { it.lane }))

        // 3. Remove exact duplicates (same time + lane)
        val deduped = sorted.distinctBy { Pair(it.time, it.lane) }

        // 4. Build final Chart and write JSON
        val finalChart = chart.copy(notes = deduped)
        outputDir.mkdirs()
        val outFile = File(outputDir, "chart.json")
        outFile.writeText(gson.toJson(finalChart), Charsets.UTF_8)
        println("ChartWriter: saved ${deduped.size} notes → ${outFile.absolutePath}")
        return outFile
    }
}
