package io.github.jwyoon1220.stellastep.model

/** Type of a note */
enum class NoteType { TAP, HOLD }

/** Judgment result for a single note */
enum class Judgment { PERFECT, GREAT, GOOD, BAD, MISS, NONE }

/**
 * A single note in the chart.
 * @param timeMs  time in milliseconds when this note should be hit (relative to audio start)
 * @param lane    lane index 0-3 (left to right)
 * @param type    TAP or HOLD
 * @param durationMs  for HOLD notes: duration in ms; 0 for TAP
 */
data class Note(
    val timeMs: Long,
    val lane: Int,
    val type: NoteType = NoteType.TAP,
    val durationMs: Long = 0L,
    // runtime state
    var judgment: Judgment = Judgment.NONE,
    var isHolding: Boolean = false,
    var isVisible: Boolean = true
)
