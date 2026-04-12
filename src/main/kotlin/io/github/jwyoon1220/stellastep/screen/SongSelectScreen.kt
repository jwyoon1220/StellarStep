package io.github.jwyoon1220.stellastep.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import io.github.jwyoon1220.stellastep.StellarStepGame
import io.github.jwyoon1220.stellastep.io.ChartParser
import java.io.File

/** Entry in the song select list */
data class SongEntry(val displayName: String, val chartFile: File)

class SongSelectScreen(private val game: StellarStepGame) : Screen {
    private val songs: List<SongEntry> = discoverSongs()
    private var selectedIndex = 0
    private var keyTimer = 0f

    private fun discoverSongs(): List<SongEntry> {
        val base = File("assets/songs")
        if (!base.exists()) return emptyList()
        return base.listFiles()?.filter { it.isDirectory }
            ?.flatMap { dir ->
                dir.listFiles { f -> f.name.endsWith(".json") }
                    ?.map { SongEntry("${dir.name} / ${it.nameWithoutExtension}", it) }
                    ?: emptyList()
            }?.sortedBy { it.displayName } ?: emptyList()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        keyTimer -= delta
        if (songs.isNotEmpty() && keyTimer <= 0f) {
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                selectedIndex = (selectedIndex - 1 + songs.size) % songs.size
                keyTimer = 0.2f
            } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                selectedIndex = (selectedIndex + 1) % songs.size
                keyTimer = 0.2f
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(MainMenuScreen(game))
            return
        }
        if (songs.isNotEmpty() && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
            val entry = songs[selectedIndex]
            try {
                val (chart, notes) = ChartParser.load(entry.chartFile)
                val audioFile = File(entry.chartFile.parentFile, chart.audioFile)
                game.setScreen(GameplayScreen(game, chart, notes, audioFile, entry.chartFile.parentFile))
            } catch (e: Exception) {
                println("Failed to load chart: ${e.message}")
            }
        }

        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        game.batch.begin()
        game.font.data.setScale(3f)
        game.font.color = Color(0.4f, 0.8f, 1f, 1f)
        game.font.draw(game.batch, "SONG SELECT", 40f, h - 40f)

        game.font.data.setScale(2f)
        if (songs.isEmpty()) {
            game.font.color = Color.GRAY
            game.font.draw(game.batch, "No songs found in assets/songs/", 40f, h / 2)
        } else {
            val visibleStart = maxOf(0, selectedIndex - 5)
            val visibleEnd = minOf(songs.size, visibleStart + 12)
            for (i in visibleStart until visibleEnd) {
                game.font.color = if (i == selectedIndex) Color.YELLOW else Color.WHITE
                val prefix = if (i == selectedIndex) "> " else "  "
                game.font.draw(game.batch, prefix + songs[i].displayName, 40f, h - 120f - (i - visibleStart) * 50f)
            }
        }
        game.font.data.setScale(1.5f)
        game.font.color = Color(0.5f, 0.5f, 0.5f, 1f)
        game.font.draw(game.batch, "UP/DOWN: Navigate   ENTER: Play   ESC: Back", 20f, 40f)
        game.batch.end()
    }

    override fun show() {}
    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {}
}
