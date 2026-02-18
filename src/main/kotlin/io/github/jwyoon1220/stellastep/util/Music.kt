package io.github.jwyoon1220.stellastep.util

import javazoom.jl.player.Player
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream


class Music(name: String?, isLoop: Boolean = false) : Thread() {
    private var player: Player? = null
    private var isLoop = false
    private var file: File? = null
    private var fis: FileInputStream? = null
    private var bis: BufferedInputStream? = null

    init {
        try {
            this.isLoop = isLoop
            file = File("assets/sound/$name")
            fis = FileInputStream(file)
            bis = BufferedInputStream(fis)
            player = Player(bis)
        } catch (e: Exception) {
            println(e.message)
        }
    }

    val time: Int
        get() {
            if (player == null) return 0
            return player!!.getPosition()
        }

    fun close() {
        isLoop = false
        player!!.close()
        this.interrupt()
    }

    override fun run() {
        try {
            do {
                player!!.play()
                fis = FileInputStream(file)
                bis = BufferedInputStream(fis)
                player = Player(bis)
            } while (isLoop)
        } catch (e: Exception) {
            ErrorHandler.handleError(e, false, 0, "소리 재생 오류")
        }
    }
}
