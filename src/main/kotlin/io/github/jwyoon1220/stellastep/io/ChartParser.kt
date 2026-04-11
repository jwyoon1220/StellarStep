package io.github.jwyoon1220.stellastep.io

import com.google.gson.Gson
import io.github.jwyoon1220.stellastep.model.Chart
import io.github.jwyoon1220.stellastep.model.Note
import io.github.jwyoon1220.stellastep.model.NoteType
import java.io.File

object ChartParser {
    private val gson = Gson()

    /**
     * Loads a chart JSON file and returns a list of [Note] objects ready for gameplay.
     * @param file the chart JSON file
     * @return Pair of (Chart metadata, list of Note objects sorted by time)
     */
    fun load(file: File): Pair<Chart, List<Note>> {
        val chart = gson.fromJson(file.readText(Charsets.UTF_8), Chart::class.java)
        val notes = chart.notes.map { nd ->
            Note(
                timeMs = nd.time,
                lane = nd.lane.coerceIn(0, 3),
                type = if (nd.type == "hold") NoteType.HOLD else NoteType.TAP,
                durationMs = nd.duration
            )
        }.sortedBy { it.timeMs }
        return Pair(chart, notes)
    }
}
