package io.github.jwyoon1220.stellastep


import javax.swing.SwingUtilities


object Main {
    const val SCREEN_WIDTH = 1280
    const val SCREEN_HEIGHT = 720
}

fun main() {

    SwingUtilities.invokeLater {
        StellarStep()
    }

}
