package io.github.jwyoon1220.stellastep.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import io.github.jwyoon1220.stellastep.StellarStepGame
import java.io.File

/**
 * Shown after chart recording completes. Displays save status and path.
 */
class ChartEditorSaveScreen(
    private val game:       StellarStepGame,
    private val savedFile:  File?,
    private val noteCount:  Int
) : Screen {

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(MainMenuScreen(game))
            return
        }

        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        game.batch.begin()
        game.font.data.setScale(3.5f)
        if (savedFile != null) {
            game.font.color = Color(0.3f, 1f, 0.3f, 1f)
            game.font.draw(game.batch, "Chart Saved!", w / 2 - 160f, h - 60f)
            game.font.data.setScale(2f)
            game.font.color = Color.WHITE
            game.font.draw(game.batch, "Notes (raw): $noteCount", 80f, h - 160f)
            game.font.draw(game.batch, "After normalize:", 80f, h - 220f)
            game.font.data.setScale(1.5f)
            game.font.color = Color(0.7f, 1f, 0.7f, 1f)
            game.font.draw(game.batch, savedFile.absolutePath, 80f, h - 280f)
        } else {
            game.font.color = Color(1f, 0.3f, 0.3f, 1f)
            game.font.draw(game.batch, "Save Failed", w / 2 - 140f, h - 60f)
            game.font.data.setScale(2f)
            game.font.color = Color.GRAY
            game.font.draw(game.batch, "Check console for details.", 80f, h - 160f)
        }

        game.font.data.setScale(2f)
        game.font.color = Color(0.5f, 0.5f, 0.5f, 1f)
        game.font.draw(game.batch, "ENTER / ESC: Back to Menu", w / 2 - 230f, 60f)
        game.batch.end()
    }

    override fun show() {}
    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {}
}
