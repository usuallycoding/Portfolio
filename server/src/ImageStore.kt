package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.ImageDto
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Validates, stores and describes uploaded images. Originals are kept byte-for-byte. */
class ImageStore(
    private val config: ServerConfig,
    private val database: Database,
    private val clock: Clock,
) {
    init {
        Files.createDirectories(config.imagesDir)
    }

    fun fileFor(fileName: String, thumbnail: Boolean): Path =
        config.imagesDir.resolve(if (thumbnail) thumbnailName(fileName) else fileName)

    suspend fun store(bytes: ByteArray): ImageDto = withContext(Dispatchers.IO) {
        if (bytes.size > config.maxUploadBytes) {
            throw ApiError(HttpStatusCode.PayloadTooLarge, "Image is larger than ${config.maxUploadBytes} bytes")
        }
        val format = ImageFormat.sniff(bytes)
            ?: throw ApiError(HttpStatusCode.UnsupportedMediaType, "Only JPEG and PNG images are accepted")
        val decoded = decode(bytes, format)

        val id = UUID.randomUUID().toString()
        val record = ImageRecord(
            id = id,
            fileName = "$id.${format.extension}",
            format = format,
            width = decoded.width,
            height = decoded.height,
            uploadedAt = clock.now().epochSecond,
        )
        val original = fileFor(record.fileName, thumbnail = false)
        val thumbnail = fileFor(record.fileName, thumbnail = true)
        try {
            Files.write(original, bytes)
            writeImage(makeThumbnail(decoded, format), format, thumbnail)
            database.transactionBlocking { conn -> Images.insert(conn, record) }
        } catch (e: Throwable) {
            Files.deleteIfExists(original)
            Files.deleteIfExists(thumbnail)
            throw e
        }
        record.toDto()
    }

    /** Checks dimensions from the header first, so a huge image is rejected before it is decoded. */
    private fun decode(bytes: ByteArray, format: ImageFormat): BufferedImage {
        val unsupported = ApiError(HttpStatusCode.UnsupportedMediaType, "The file is not a readable JPEG or PNG image")
        try {
            ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
                val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: throw unsupported
                try {
                    if (!reader.formatName.equals(format.readerName, ignoreCase = true)) throw unsupported
                    reader.input = input
                    val width = reader.getWidth(0)
                    val height = reader.getHeight(0)
                    if (width <= 0 || height <= 0) throw unsupported
                    if (width > config.maxImageEdge || height > config.maxImageEdge ||
                        width.toLong() * height > config.maxImagePixels
                    ) {
                        throw ApiError(HttpStatusCode.PayloadTooLarge, "Image dimensions are too large (${width}x$height)")
                    }
                    return reader.read(0)
                } finally {
                    reader.dispose()
                }
            }
        } catch (e: ApiError) {
            throw e
        } catch (e: IOException) {
            throw unsupported
        } catch (e: RuntimeException) {
            throw unsupported
        }
    }

    /** Scales so the longest edge is at most the configured size; never upscales. Keeps transparency for PNG. */
    private fun makeThumbnail(source: BufferedImage, format: ImageFormat): BufferedImage {
        val scale = min(1.0, config.thumbnailEdge.toDouble() / max(source.width, source.height))
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

    private fun writeImage(image: BufferedImage, format: ImageFormat, path: Path) {
        val writer = ImageIO.getImageWritersByFormatName(format.readerName).next()
        try {
            Files.newOutputStream(path).use { out ->
                ImageIO.createImageOutputStream(out).use { ios ->
                    writer.output = ios
                    val param = writer.defaultWriteParam
                    if (format == ImageFormat.JPEG) {
                        param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                        param.compressionQuality = 0.85f
                    }
                    writer.write(null, IIOImage(image, null, null), param)
                }
            }
        } finally {
            writer.dispose()
        }
    }
}
