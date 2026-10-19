package com.usuallycoding.portfolio.server.data

import com.usuallycoding.portfolio.server.domain.DomainError
import com.usuallycoding.portfolio.server.domain.ImageFormat
import com.usuallycoding.portfolio.server.domain.ImageLimits
import com.usuallycoding.portfolio.server.domain.ImageProcessor
import com.usuallycoding.portfolio.server.domain.ProcessedImage
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Decodes with the JDK's image readers and scales thumbnails to at most [thumbnailEdge] on the longest edge. */
class AwtImageProcessor(private val thumbnailEdge: Int) : ImageProcessor {
    override fun process(bytes: ByteArray, format: ImageFormat, limits: ImageLimits): ProcessedImage {
        val decoded = decode(bytes, format, limits)
        return ProcessedImage(decoded.width, decoded.height, encode(makeThumbnail(decoded, format), format))
    }

    /** Checks dimensions from the header first, so a huge image is rejected before it is decoded. */
    private fun decode(bytes: ByteArray, format: ImageFormat, limits: ImageLimits): BufferedImage {
        val unsupported = DomainError.UnsupportedImage("The file is not a readable JPEG or PNG image")
        try {
            ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
                val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: throw unsupported
                try {
                    if (!reader.formatName.equals(format.readerName, ignoreCase = true)) throw unsupported
                    reader.input = input
                    val width = reader.getWidth(0)
                    val height = reader.getHeight(0)
                    if (width <= 0 || height <= 0) throw unsupported
                    if (width > limits.maxImageEdge || height > limits.maxImageEdge ||
                        width.toLong() * height > limits.maxImagePixels
                    ) {
                        throw DomainError.PayloadTooLarge("Image dimensions are too large (${width}x$height)")
                    }
                    return reader.read(0)
                } finally {
                    reader.dispose()
                }
            }
        } catch (e: DomainError) {
            throw e
        } catch (e: IOException) {
            throw unsupported
        } catch (e: RuntimeException) {
            throw unsupported
        }
    }

    /** Scales so the longest edge is at most the configured size; never upscales. Keeps transparency for PNG. */
    private fun makeThumbnail(source: BufferedImage, format: ImageFormat): BufferedImage {
        val scale = min(1.0, thumbnailEdge.toDouble() / max(source.width, source.height))
        val width = max(1, (source.width * scale).roundToInt())
        val height = max(1, (source.height * scale).roundToInt())
        val type = if (format == ImageFormat.JPEG) BufferedImage.TYPE_INT_RGB else BufferedImage.TYPE_INT_ARGB
        val target = BufferedImage(width, height, type)
        val scaled = source.getScaledInstance(width, height, java.awt.Image.SCALE_AREA_AVERAGING)
        val g = target.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g.drawImage(scaled, 0, 0, null)
        } finally {
            g.dispose()
        }
        return target
    }

    private fun encode(image: BufferedImage, format: ImageFormat): ByteArray {
        val writer = ImageIO.getImageWritersByFormatName(format.readerName).next()
        try {
            val out = ByteArrayOutputStream()
            ImageIO.createImageOutputStream(out).use { ios ->
                writer.output = ios
                val param = writer.defaultWriteParam
                if (format == ImageFormat.JPEG) {
                    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                    param.compressionQuality = 0.85f
                }
                writer.write(null, IIOImage(image, null, null), param)
            }
            return out.toByteArray()
        } finally {
            writer.dispose()
        }
    }
}
