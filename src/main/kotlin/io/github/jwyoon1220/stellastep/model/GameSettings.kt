package io.github.jwyoon1220.stellastep.model

import com.badlogic.gdx.Input

/**
 * Persisted game settings.
 * Stored as JSON in user home / .stellarstep / settings.json
 */
data class GameSettings(
    /** global offset in ms; positive = push notes later (audio is early) */
    var globalOffsetMs: Long = 0L,
    /** scroll speed in pixels per second */
    var scrollSpeed: Float = 500f,
    /** libGDX key codes for lanes 0-3 (default: D F J K) */
    var keyBindings: IntArray = intArrayOf(Input.Keys.D, Input.Keys.F, Input.Keys.J, Input.Keys.K)
)
