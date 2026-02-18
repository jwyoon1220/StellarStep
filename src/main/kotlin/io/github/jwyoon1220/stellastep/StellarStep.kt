package io.github.jwyoon1220.stellastep

import io.github.jwyoon1220.stellastep.io.Parser
import io.github.jwyoon1220.stellastep.util.ErrorHandler
import io.github.jwyoon1220.stellastep.util.KeyListener
import io.github.jwyoon1220.stellastep.util.KeyListener.isKeyPressed
import io.github.jwyoon1220.stellastep.util.KeyListener.isKeysPressed
import io.github.jwyoon1220.stellastep.util.KeyListener.keyMap
import io.github.jwyoon1220.stellastep.util.Music
import java.awt.Color
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.io.File
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.RenderingHints
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.Timer
import javax.swing.WindowConstants
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class StellarStep : JFrame("StellarStep") {

    var screenImage: Image? = null
    var screenGraphics: Graphics? = null

    private val REPAINT_INTERVAL_MS = 33 // 약 30 FPS
    private val REPAINT_WINDOW_MS = 66 // 이건 60 FPS

    var videoImage: BufferedImage? = null

    private var backgroundImage = ImageIcon("assets/imgs/intro.png").image

    private val screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode

    private val songPack = Parser.loadSongPack(File("assets/sheets.json"))

    private var isMainScreen = true

    private var isDirty = false
    private val videoTimer = Timer(REPAINT_INTERVAL_MS) {
        VideoPlayer.updateVideo(videoImage ?: return@Timer, (screenGraphics ?: return@Timer) as Graphics2D, screenSize)
        isDirty = true
    }
    private val repaintTimer = Timer(REPAINT_WINDOW_MS) {
        if (isDirty) {
            repaint()
            isDirty = false
        }
    }


    init {
        VideoPlayer.init(screenSize.width, screenSize.height, this)
        val duplicateErrors = songPack.validateDuplicates()
        if (duplicateErrors.isNotEmpty()) {
            val sb = StringBuilder()
            for (err in songPack.validateDuplicates()) {
                sb.appendLine(err)
            }
            ErrorHandler.handleWarning(sb.toString())
            exitProcess(0)
        }

        setSize(screenSize.width, screenSize.height)
        isUndecorated = true
        setLocationRelativeTo(null)
        background = Color.BLACK
        layout = null

        addWindowListener(WindowListener())

        addKeyListener(KeyListener)
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {

                if (isKeysPressed(KeyEvent.VK_ALT, KeyEvent.VK_F4)) {
                    VideoPlayer.release()
                    exitProcess(0)
                }

                if (isMainScreen) {
                    // 음악 선택 화면으로
                    Music("hihat.mp3").start()
                    isMainScreen = false
                }
            }
        })

        defaultCloseOperation = EXIT_ON_CLOSE
        isVisible = true

        videoTimer.start()
        repaintTimer.start()

        VideoPlayer.play(songPack.songs.random().video);
    }

    override fun paint(g: Graphics) {
        if (screenImage == null) screenImage = createImage(screenSize.width, screenSize.height)
        screenGraphics = screenImage?.graphics
        (screenGraphics as Graphics2D).setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )

        //paintComponents(screenGraphics)
        val imgW = backgroundImage.getWidth(null)
        val imgH = backgroundImage.getHeight(null)

        val destW = screenSize.width
        val destH = screenSize.height

        val scale = minOf(destW.toDouble() / imgW, destH.toDouble() / imgH)
        val drawW = (imgW * scale).toInt()
        val drawH = (imgH * scale).toInt()

        val offsetX = (destW - drawW) / 2
        val offsetY = (destH - drawH) / 2

        if (isMainScreen) {
            screenGraphics?.drawImage(backgroundImage, offsetX + 40, offsetY, drawW, drawH, null)
        }
        screenDraw(screenGraphics ?: return)
        g.drawImage(screenImage, 0, 0, null)
    }

    private fun screenDraw(g: Graphics) {
        val g2d = g as Graphics2D
    }
}