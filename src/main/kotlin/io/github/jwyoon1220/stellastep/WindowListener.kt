package io.github.jwyoon1220.stellastep

import sun.misc.Unsafe
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlin.system.exitProcess

class WindowListener: WindowAdapter() {

    override fun windowClosing(e: WindowEvent) {
        VideoPlayer.release()
        exitProcess(0)
    }

}