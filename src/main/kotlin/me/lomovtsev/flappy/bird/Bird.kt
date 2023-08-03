package me.lomovtsev.flappy.bird

import com.lehaine.littlekt.graphics.g2d.Animation
import com.lehaine.littlekt.graphics.g2d.AnimationPlayer
import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.g2d.TextureSlice
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.util.milliseconds
import kotlin.math.abs
import kotlin.time.Duration

class Bird(
    private val flapAnimation: Animation<TextureSlice>,
    var width: Float,
    var height: Float
) {
    var x = 0f
    var y = 0f
    val speed = 0.06f
    val gravity = 0.018f
    var speedMultiplier =  1f
    var gravityMultiplier = 1f
    val flapHeight = -0.4f // in the mac os -0.7 is fine, in windows -0.5

    private val velocity = MutableVec2f(0f)
    private var sprite = flapAnimation.firstFrame

    val animationPlayer = AnimationPlayer<TextureSlice>().apply {
        onFrameChange = {
            sprite = currentAnimation?.get(it) ?: sprite
        }
        playLooped(flapAnimation)
    }

    val collider = Rect(x - width * 0.5f - height * 0.5f, y, width, height)

    fun update(dt: Duration) {
        velocity.x = speed * speedMultiplier
        velocity.y += gravity * gravityMultiplier

        x += velocity.x * dt.milliseconds
        y += velocity.y * dt.milliseconds
        animationPlayer.update(dt)

        velocity.y *= 0.91f
        if (abs(velocity.y) <= 0.0005f) {
            velocity.y = 0f
        }
        collider.set(x - width * 0.5f, y - height * 0.5f, width, height)
    }

    fun render(batch: Batch) = batch.draw(sprite, x, y, sprite.width * 0.5f, sprite.height * 0.5f)

    fun flap() {
        velocity.y = flapHeight
    }

    fun reset() {
        gravityMultiplier = 1f
        speedMultiplier = 1f
        animationPlayer.playLooped(flapAnimation)
    }

    fun die() {
        velocity.y = 0f
        velocity.y = 0f
        gravityMultiplier = 0f
        speedMultiplier = 0f
        animationPlayer.stop()
    }
}