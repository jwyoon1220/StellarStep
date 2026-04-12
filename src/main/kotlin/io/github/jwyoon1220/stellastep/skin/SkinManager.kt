package io.github.jwyoon1220.stellastep.skin

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Color

/**
 * Loads skin textures from assets/skins/<skinName>/.
 * Falls back to a solid-color 1×1 texture if any file is missing.
 *
 * Asset keys and expected files (see ASSET_SPEC.md):
 *   bg          → bg.png           (1280×720)
 *   lane        → lane.png         (80×600)
 *   note        → note.png         (76×20)
 *   hold_body   → hold_body.png    (76×1)
 *   hold_end    → hold_end.png     (76×20)
 *   key_on      → key_on.png       (80×120)
 *   key_off     → key_off.png      (80×120)
 *   hit_effect  → hit_effect.png   (80×80)
 *   judgment_line → judgment_line.png (320×4)
 */
class SkinManager(private val skinName: String = "default") {
    private val textures = mutableMapOf<String, Texture>()

    // Fallback colors for each asset key
    private val fallbackColors = mapOf(
        "bg"             to Color(0.05f, 0.05f, 0.1f, 1f),
        "lane"           to Color(0.15f, 0.15f, 0.25f, 1f),
        "note"           to Color(0.2f, 0.8f, 1.0f, 1f),
        "hold_body"      to Color(0.1f, 0.6f, 0.9f, 0.8f),
        "hold_end"       to Color(0.0f, 0.9f, 0.9f, 1f),
        "key_on"         to Color(1f, 1f, 0.3f, 1f),
        "key_off"        to Color(0.3f, 0.3f, 0.4f, 1f),
        "hit_effect"     to Color(1f, 1f, 1f, 0.9f),
        "judgment_line"  to Color(1f, 1f, 1f, 1f)
    )

    fun load() {
        val keys = fallbackColors.keys
        for (key in keys) {
            val path = "assets/skins/$skinName/$key.png"
            textures[key] = tryLoadTexture(path, fallbackColors[key] ?: Color.WHITE)
        }
    }

    private fun tryLoadTexture(path: String, fallback: Color): Texture {
        return try {
            val file = Gdx.files.internal(path)
            if (file.exists()) Texture(file)
            else solidColorTexture(fallback)
        } catch (_: Exception) {
            solidColorTexture(fallback)
        }
    }

    private fun solidColorTexture(color: Color): Texture {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()
        val tex = Texture(pixmap)
        pixmap.dispose()
        return tex
    }

    operator fun get(key: String): Texture = textures[key] ?: solidColorTexture(Color.MAGENTA)

    fun dispose() {
        textures.values.forEach { it.dispose() }
        textures.clear()
    }
}
