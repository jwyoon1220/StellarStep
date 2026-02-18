package io.github.jwyoon1220.stellastep.util

import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

object KeyListener: KeyAdapter() {
    val keyMap: HashMap<Int, Boolean> = HashMap<Int, Boolean>()

    override fun keyPressed(e: KeyEvent) {
        keyMap[e.id] = true
    }
    override fun keyReleased(e: KeyEvent) {
        keyMap[e.id] = false;
    }

    fun isKeyPressed(code: Int): Boolean {
        if (keyMap.containsKey(code)) {
            return keyMap[code]!!
        } else {
            keyMap[code] = false
            return false
        }
    }

    /*
    * @param codes: 체크할 키 코드들
    * @return 하나라도 눌려있으면 true, 하나라도 눌려
    */
    fun isKeyPressed(vararg codes: Int): Boolean {
        for (code in codes) {
            if (keyMap.getOrPut(code) { false }) {
                return true
            }
        }
        return false
    }
    /*
    * @param codes: 체크할 키 코드들
    * @return 모든 키가 눌려있으면 true, 하나라도 눌려
    */
    fun isKeysPressed(vararg codes: Int): Boolean {
        for (code in codes) {
            if (!keyMap.getOrPut(code) { false }) {
                return false
            }
        }
        return true
    }
}