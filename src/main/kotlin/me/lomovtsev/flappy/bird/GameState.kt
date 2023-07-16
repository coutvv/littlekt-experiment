package me.lomovtsev.flappy.bird

import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer

class GameState {
    private var currentState = FbGameState.INIT

    fun startGame() {
        if (currentState != FbGameState.INIT) {
            throw IllegalStateException("incorrect state $currentState for starting")
        }
        currentState = FbGameState.STARTED
    }

    fun gameOver() {
        if (currentState != FbGameState.STARTED) {
            throw IllegalStateException("incorrect state $currentState for game over")
        }
        currentState = FbGameState.GAME_OVER
    }

    fun pauseToggle() {
        if (currentState != FbGameState.STARTED || currentState != FbGameState.PAUSED) {
            throw IllegalStateException("incorrect state $currentState for pause toggling")
        }
        if (currentState == FbGameState.STARTED) {
            currentState = FbGameState.PAUSED
        } else {
            currentState = FbGameState.STARTED
        }
    }

    fun reset() {
        currentState = FbGameState.INIT
    }

    fun control(input: Input, audioPart: AudioPart, bird: Bird) {
        if (currentState == FbGameState.STARTED) {
            if (input.isJustTouched(Pointer.POINTER1) || input.isKeyJustPressed(Key.SPACE)) {
                audioPart.flapSound()
                bird.flap()
            }
        } else if (currentState == FbGameState.GAME_OVER) {
            if (input.isKeyJustPressed(Key.SPACE)) {
                reset() // TODO: another logic
            }
        } else if (currentState == FbGameState.INIT) {
            if (input.isKeyJustPressed(Key.SPACE)) {
                startGame()
            }
        }
    }

    fun isStarted(): Boolean {
        return currentState == FbGameState.STARTED
    }
    fun get(): FbGameState {
        return currentState
    }

    fun init() {
        currentState = FbGameState.INIT
    }
}