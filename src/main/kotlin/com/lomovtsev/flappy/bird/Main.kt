package com.lomovtsev.flappy.bird

import com.lehaine.littlekt.createLittleKtApp

fun main() {
    createLittleKtApp{
        width = 540
        height = 1024
        vSync = true
        title = "Flappy Bird Clone"
    }.start {
        FlappyBirdGame(it)
    }
}