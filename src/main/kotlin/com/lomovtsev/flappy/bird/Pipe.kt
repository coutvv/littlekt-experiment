package com.lomovtsev.flappy.bird

import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.g2d.TextureSlice
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.random

class Pipe(
    val pipeHead: TextureSlice,
    val pipeBody: TextureSlice,
    val offsetX: Int,
    val availableHeight: Int,
    val groundOffset: Int
) : EnvironmentObject(pipeBody.width, 0, true) {

    private var pipeTopHeight = 0f
    private var pipeBottomHeight = 0f

    private val topPipeBodyRect = Rect()
    private val topPipeHeadRect = Rect()

    private val bottomPipeBodyRect = Rect()
    private val bottomPipeHeadRect = Rect()

    private val scoreRect = Rect()

    private val pipeSeparationHeight = 75
    private var collected = false

    init {
        generate()
    }

    override fun update(viewBounds: Rect) {
        if (x + width + (width * 2) < viewBounds.x) {
            x += offsetX
            onViewBoundsReset()
        }
        topPipeBodyRect.set(x, y, pipeBody.width.toFloat(), pipeTopHeight)
        topPipeHeadRect.set(x, y + pipeTopHeight, pipeHead.width.toFloat(), pipeHead.height.toFloat())

        bottomPipeBodyRect.set(
            x, y - groundOffset + availableHeight,
            pipeBody.width.toFloat(),
            pipeBottomHeight
        )
        bottomPipeHeadRect.set(
            x, y + availableHeight - groundOffset - pipeBottomHeight - pipeHead.height.toFloat(),
            pipeHead.width.toFloat(), pipeHead.height.toFloat()
        )

        scoreRect.set(x + 5f, y + pipeTopHeight + pipeHead.height.toFloat(), 5f, pipeSeparationHeight.toFloat())
    }

    fun render(batch: Batch) {
        // draw top pipe
        batch.draw(pipeBody, x, y, height = pipeTopHeight)
        batch.draw(pipeHead, x, y + pipeTopHeight, flipY = true)

        // draw bottom pipe
        batch.draw(
            slice = pipeBody,
            x = x,
            y = y - groundOffset + availableHeight,
            originY = pipeBottomHeight,
            height = pipeBottomHeight
        )
        batch.draw(
            slice = pipeHead,
            x = x,
            y = y + availableHeight - groundOffset - pipeBottomHeight,
            originY = pipeHead.height.toFloat()
        )

    }

    fun intersectingScore(rect: Rect) = !collected && scoreRect.intersects(rect)

    fun collect() {
        collected = true
    }

    fun generate() {
        val minPipeHeight = 5
        val availablePipeHeight = availableHeight - groundOffset - pipeHead.height * 2 - pipeSeparationHeight
        pipeTopHeight = (minPipeHeight..availablePipeHeight).random()
        pipeBottomHeight = availablePipeHeight - pipeTopHeight
        collected = false
    }

    override fun onViewBoundsReset() {
        generate()
    }

    override fun isColliding(rect: Rect): Boolean {
        return hasCollision && intersecting(rect)
    }

    private fun intersecting(rect: Rect) =
        topPipeBodyRect.intersects(rect)
                || topPipeHeadRect.intersects(rect)
                || bottomPipeBodyRect.intersects(rect)
                || bottomPipeHeadRect.intersects(rect)
}