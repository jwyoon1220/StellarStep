package io.github.jwyoon1220.stellastep.model

/**
 * A loaded chart file.
 * JSON format: {"title":"...","artist":"...","bpm":120,"audioFile":"audio.ogg","offsetMs":0,"notes":[...]}
 */
data class Chart(
    val title: String = "",
    val artist: String = "",
    val bpm: Int = 120,
    /** audio file name relative to the chart's directory */
    val audioFile: String = "",
    /** chart-level offset in ms (positive = audio starts late relative to notes) */
    val offsetMs: Long = 0L,
    val notes: List<NoteData> = emptyList()
)

data class NoteData(
    /** hit time in ms from audio start */
    val time: Long = 0L,
    /** lane 0-3 */
    val lane: Int = 0,
    /** "tap" or "hold" */
    val type: String = "tap",
    /** duration in ms for hold notes */
    val duration: Long = 0L
)
