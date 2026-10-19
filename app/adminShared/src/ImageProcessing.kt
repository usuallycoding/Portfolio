package com.usuallycoding.portfolio.admin

import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The aspect ratios the server requires for specific images. */
enum class CropAspect(val width: Int, val height: Int, val label: String) {
    Square(1, 1, "1:1"),
    Cover(3, 1, "3:1"),
    Thumbnail(16, 9, "16:9"),
}

/** A rectangle in source image pixels. */
data class CropRect(val left: Float, val top: Float, val width: Float, val height: Float)

/** A decoded image, already rotated to its display orientation (EXIF is applied by the decoder). */
class SourceImage internal constructor(
    internal val image: Image,
    val width: Int,
    val height: Int,
    val isPng: Boolean,
)

/** An image ready to upload: JPEG or PNG, no larger than the maximum edge. */
class PreparedImage(
    val bytes: ByteArray,
    val contentType: String,
    val extension: String,
    val width: Int,
    val height: Int,
)

object ImageProcessing {
    /** Longest edge of an uploaded image. Larger sources are scaled down. */
    const val MAX_EDGE = 2000
    private const val JPEG_QUALITY = 85

    private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

    /** Decodes any image Skia understands (JPEG, PNG, WebP, ...). Returns null if the bytes are not an image. */
    fun decode(bytes: ByteArray): SourceImage? = try {
        val isPng = bytes.size >= 4 && PNG_MAGIC.indices.all { bytes[it] == PNG_MAGIC[it] }
        // Skia applies the EXIF orientation while decoding, so phone photos come out upright.
        val image = Image.makeFromEncoded(bytes)
        SourceImage(image, image.width, image.height, isPng).takeIf { it.width > 0 && it.height > 0 }
    } catch (e: Exception) {
        null
    }

    /**
     * Crops [source] to [crop] (or keeps all of it), scales it down to at most [maxEdge], and encodes it.
     * With an [aspect] the output has exactly that ratio (to the nearest pixel); without one it keeps the
     * crop's shape. PNG sources stay PNG so transparency survives; everything else becomes JPEG.
     */
    fun prepare(
        source: SourceImage,
        crop: CropRect? = null,
        aspect: CropAspect? = null,
        maxEdge: Int = MAX_EDGE,
    ): PreparedImage {
        val region = crop ?: CropRect(0f, 0f, source.width.toFloat(), source.height.toFloat())
        val scale = min(1f, maxEdge.toFloat() / max(region.width, region.height))
        val outWidth: Int
        val outHeight: Int
        if (aspect != null) {
            outWidth = max(1, min((region.width * scale).roundToInt(), maxEdge))
            outHeight = max(1, (outWidth.toDouble() * aspect.height / aspect.width).roundToInt())
        } else {
            outWidth = max(1, (region.width * scale).roundToInt())
            outHeight = max(1, (region.height * scale).roundToInt())
        }

        val surface = Surface.makeRasterN32Premul(outWidth, outHeight)
        val canvas = surface.canvas
        if (!source.isPng) canvas.clear(Color.WHITE)
        canvas.drawImageRect(
            source.image,
            Rect.makeXYWH(region.left, region.top, region.width, region.height),
            Rect.makeWH(outWidth.toFloat(), outHeight.toFloat()),
            FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
            null,
            true,
        )
        val snapshot = surface.makeImageSnapshot()
        val format = if (source.isPng) EncodedImageFormat.PNG else EncodedImageFormat.JPEG
        val data = snapshot.encodeToData(format, JPEG_QUALITY) ?: error("Could not encode the image")
        return PreparedImage(
            bytes = data.bytes,
            contentType = if (source.isPng) "image/png" else "image/jpeg",
            extension = if (source.isPng) "png" else "jpg",
            width = outWidth,
            height = outHeight,
        )
    }
}
