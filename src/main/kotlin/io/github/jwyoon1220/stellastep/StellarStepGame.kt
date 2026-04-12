package io.github.jwyoon1220.stellastep

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.jwyoon1220.stellastep.screen.MainMenuScreen
import io.github.jwyoon1220.stellastep.settings.SettingsManager
import io.github.jwyoon1220.stellastep.skin.SkinManager
import io.github.jwyoon1220.stellastep.video.VideoPlayer

class StellarStepGame : Game() {
    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont
    lateinit var shapeRenderer: ShapeRenderer
    lateinit var settings: SettingsManager
    lateinit var skin: SkinManager
    lateinit var videoPlayer: VideoPlayer

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont() // built-in libGDX font
        font.data.setScale(2f)
        shapeRenderer = ShapeRenderer()
        settings = SettingsManager()
        settings.load()
        skin = SkinManager()
        skin.load()
        videoPlayer = VideoPlayer()
        setScreen(MainMenuScreen(this))
    }

    override fun dispose() {
        videoPlayer.dispose()
        batch.dispose()
        font.dispose()
        shapeRenderer.dispose()
        skin.dispose()
        screen?.dispose()
    }
}
