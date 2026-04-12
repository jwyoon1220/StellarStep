package io.github.jwyoon1220.stellastep.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.jwyoon1220.stellastep.StellarStepGame

class MainMenuScreen(private val game: StellarStepGame) : Screen {
    private val menuItems = listOf("PLAY", "CALIBRATION", "CHART EDITOR", "QUIT")
    private var selectedIndex = 0
    private var keyTimer = 0f

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Handle input (with repeat delay)
        keyTimer -= delta
        if (keyTimer <= 0f) {
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                selectedIndex = (selectedIndex - 1 + menuItems.size) % menuItems.size
                keyTimer = 0.2f
            } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                selectedIndex = (selectedIndex + 1) % menuItems.size
                keyTimer = 0.2f
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            when (selectedIndex) {
                0 -> game.setScreen(SongSelectScreen(game))
                1 -> game.setScreen(CalibrationScreen(game))
                2 -> game.setScreen(ChartEditorSetupScreen(game))
                3 -> Gdx.app.exit()
            }
        }

        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        game.batch.begin()
        // Title
        game.font.color = Color(0.4f, 0.8f, 1f, 1f)
        game.font.data.setScale(4f)
        game.font.draw(game.batch, "StellarStep", w / 2 - 180f, h * 0.75f)

        // Menu items
        game.font.data.setScale(2.5f)
        for ((i, item) in menuItems.withIndex()) {
            game.font.color = if (i == selectedIndex) Color.YELLOW else Color.WHITE
            game.font.draw(game.batch, (if (i == selectedIndex) "> " else "  ") + item, w / 2 - 140f, h * 0.55f - i * 60f)
        }
        // Hint
        game.font.data.setScale(1.5f)
        game.font.color = Color(0.5f, 0.5f, 0.5f, 1f)
        game.font.draw(game.batch, "UP/DOWN: Navigate   ENTER: Select", 20f, 40f)
        game.batch.end()
    }

    override fun show() {}
    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {}
}
