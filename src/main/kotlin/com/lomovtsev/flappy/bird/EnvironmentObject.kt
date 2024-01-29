package com.lomovtsev.flappy.bird

import com.lehaine.littlekt.math.Rect
import kotlin.math.roundToInt

open class EnvironmentObject(
    val width: Int,
    val totalToWait: Int,
    val hasCollision: Boolean
) {

    var x: Float = 0f
    var y: Float = 0f

    open fun update(viewBounds: Rect) {
        if (x + width + (width * totalToWait) < viewBounds.x) {
            x = viewBounds.x2.roundToInt().toFloat()
            onViewBoundsReset()

        }
    }

    open fun onViewBoundsReset() = Unit

    open fun isColliding(rect: Rect) = false
}