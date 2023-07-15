package me.lomovtsev.flappy.bird

import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.g2d.TextureSlice
import com.lehaine.littlekt.math.Rect

class TexturedEnvironmentObject(
    val texture: TextureSlice,
    totalToWait: Int,
    hasCollsion: Boolean
): EnvironmentObject(texture.width, totalToWait, hasCollsion) {

    fun render(batch: Batch) = batch.draw(texture, x, y)

    override fun isColliding(rect: Rect): Boolean {
        return if (hasCollision) rect.intersects(
            x, y,
            x + texture.width.toFloat(),
            y + texture.height.toFloat()
        ) else false
    }
}