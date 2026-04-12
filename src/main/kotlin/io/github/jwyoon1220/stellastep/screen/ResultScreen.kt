package io.github.jwyoon1220.stellastep.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import io.github.jwyoon1220.stellastep.StellarStepGame
import io.github.jwyoon1220.stellastep.model.Chart
import io.github.jwyoon1220.stellastep.model.Judgment

class ResultScreen(
    private val game: StellarStepGame,
    private val chart: Chart,
    private val judgmentCounts: Map<Judgment, Int>,
    private val maxCombo: Int,
    private val score: Long
) : Screen {

    private val totalNotes = judgmentCounts.values.sum()
    private val accuracy: Float = if (totalNotes == 0) 0f else
        ((judgmentCounts[Judgment.PERFECT]!! * 100 +
          judgmentCounts[Judgment.GREAT]!! * 70 +
          judgmentCounts[Judgment.GOOD]!! * 40 +
          judgmentCounts[Judgment.BAD]!! * 10) /
         (totalNotes * 100f)) * 100f

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
        game.font.color = Color(0.4f, 0.8f, 1f, 1f)
        game.font.draw(game.batch, "RESULT", w / 2 - 100f, h - 50f)

        game.font.data.setScale(2f)
        game.font.color = Color.LIGHT_GRAY
        game.font.draw(game.batch, "${chart.title}  -  ${chart.artist}", 80f, h - 110f)

        val rows = listOf(
            "SCORE" to "$score",
            "ACCURACY" to "%.2f%%".format(accuracy),
            "MAX COMBO" to "$maxCombo",
            "" to "",
            "PERFECT" to "${judgmentCounts[Judgment.PERFECT]}",
            "GREAT"   to "${judgmentCounts[Judgment.GREAT]}",
            "GOOD"    to "${judgmentCounts[Judgment.GOOD]}",
            "BAD"     to "${judgmentCounts[Judgment.BAD]}",
            "MISS"    to "${judgmentCounts[Judgment.MISS]}"
        )
        val colors = mapOf(
            "PERFECT" to Color(1f, 1f, 0.3f, 1f),
            "GREAT"   to Color(0.3f, 1f, 0.3f, 1f),
            "GOOD"    to Color(0.3f, 0.7f, 1f, 1f),
            "BAD"     to Color(1f, 0.5f, 0.2f, 1f),
            "MISS"    to Color(0.7f, 0.2f, 0.2f, 1f)
        )
        game.font.data.setScale(2.2f)
        for ((i, row) in rows.withIndex()) {
            val (label, value) = row
            if (label.isEmpty()) continue
            game.font.color = colors[label] ?: Color.WHITE
            game.font.draw(game.batch, label, 160f, h - 200f - i * 55f)
            game.font.color = Color.WHITE
            game.font.draw(game.batch, value, 500f, h - 200f - i * 55f)
        }

        game.font.data.setScale(1.8f)
        game.font.color = Color(0.5f, 0.5f, 0.5f, 1f)
        game.font.draw(game.batch, "ENTER / ESC: Back to Menu", w / 2 - 220f, 50f)
        game.batch.end()
    }

    override fun show() {}
    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {}
}
