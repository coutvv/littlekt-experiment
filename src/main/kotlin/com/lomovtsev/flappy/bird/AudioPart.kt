package com.lomovtsev.flappy.bird

import com.lehaine.littlekt.async.AsyncThreadDispatcher
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.async.newSingleThreadAsyncContext
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.file.vfs.readAudioClip
import kotlinx.coroutines.launch

data class AudioPart(
    private val flapSfx: AudioClip,
    private val scoreSfx: AudioClip,
    private val audioCtx: AsyncThreadDispatcher
) {
    companion object {
        suspend fun create(resourcesVfs: VfsFile): AudioPart {
            val audioCtx: AsyncThreadDispatcher = newSingleThreadAsyncContext()
            val flapSfx = resourcesVfs["sfx/flap.wav"].readAudioClip()
            val scoreSfx = resourcesVfs["sfx/coinPickup0.wav"].readAudioClip()
            return AudioPart(flapSfx, scoreSfx, audioCtx)
        }
    }
    fun flapSound() {
        KtScope.launch(audioCtx) {
            flapSfx.play()
        }
    }

    fun coinPickupSound() {
        KtScope.launch(audioCtx) {
            scoreSfx.play(0.5f)
        }
    }
}