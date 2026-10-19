package com.usuallycoding.portfolio.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlin.math.max
import kotlin.math.min

/**
 * Where the crop frame sits on an image. At zoom 1 the frame covers the largest area of the
 * required ratio that fits inside the image; zooming in shrinks that area around its centre,
 * and panning moves it. The area never leaves the image.
 */
class CropState(val imageWidth: Int, val imageHeight: Int, val aspect: CropAspect) {
    private val ratio = aspect.width.toFloat() / aspect.height

    /** Size of the crop area at zoom 1, in image pixels. */
    val baseWidth: Float = min(imageWidth.toFloat(), imageHeight * ratio)
    val baseHeight: Float = baseWidth / ratio

    var zoom by mutableFloatStateOf(1f)
        private set
    var centerX by mutableFloatStateOf(imageWidth / 2f)
        private set
    var centerY by mutableFloatStateOf(imageHeight / 2f)
        private set

    val cropWidth: Float get() = baseWidth / zoom
    val cropHeight: Float get() = baseHeight / zoom

    fun rect(): CropRect {
        val w = cropWidth
        val h = cropHeight
        return CropRect(
            left = (centerX - w / 2).coerceIn(0f, imageWidth - w),
            top = (centerY - h / 2).coerceIn(0f, imageHeight - h),
            width = w,
            height = h,
        )
    }

    fun zoomTo(value: Float) {
        zoom = value.coerceIn(MIN_ZOOM, MAX_ZOOM)
        clampCenter()
    }

    fun zoomBy(factor: Float) = zoomTo(zoom * factor)

    /** Moves the crop area by a distance in image pixels. */
    fun panBy(dx: Float, dy: Float) {
        centerX += dx
        centerY += dy
        clampCenter()
    }

    fun reset() {
        zoom = 1f
        centerX = imageWidth / 2f
        centerY = imageHeight / 2f
    }

    private fun clampCenter() {
        centerX = centerX.coerceIn(cropWidth / 2, max(cropWidth / 2, imageWidth - cropWidth / 2))
        centerY = centerY.coerceIn(cropHeight / 2, max(cropHeight / 2, imageHeight - cropHeight / 2))
    }

    companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 8f
    }
}
