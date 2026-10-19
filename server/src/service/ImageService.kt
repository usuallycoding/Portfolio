package com.usuallycoding.portfolio.server.service

import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.server.domain.Clock
import com.usuallycoding.portfolio.server.domain.DomainError
import com.usuallycoding.portfolio.server.domain.ImageFiles
import com.usuallycoding.portfolio.server.domain.ImageFormat
import com.usuallycoding.portfolio.server.domain.ImageLimits
import com.usuallycoding.portfolio.server.domain.ImageProcessor
import com.usuallycoding.portfolio.server.domain.ImageRecord
import com.usuallycoding.portfolio.server.domain.Transactor
import com.usuallycoding.portfolio.server.domain.notFound
import com.usuallycoding.portfolio.server.domain.thumbnailName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/** An image ready to send. */
class ServedImage(val bytes: ByteArray, val contentType: String)

/** Validates, stores and serves uploaded images. Originals are kept byte-for-byte. */
class ImageService(
    private val transactor: Transactor,
    private val files: ImageFiles,
    private val processor: ImageProcessor,
    private val clock: Clock,
    private val limits: ImageLimits,
) {
    val maxUploadBytes: Long get() = limits.maxUploadBytes

    suspend fun store(bytes: ByteArray): ImageDto = withContext(Dispatchers.IO) {
        if (bytes.size > limits.maxUploadBytes) {
            throw DomainError.PayloadTooLarge("Image is larger than ${limits.maxUploadBytes} bytes")
        }
        val format = ImageFormat.sniff(bytes)
            ?: throw DomainError.UnsupportedImage("Only JPEG and PNG images are accepted")
        val processed = processor.process(bytes, format, limits)

        val id = UUID.randomUUID().toString()
        val record = ImageRecord(
            id = id,
            fileName = "$id.${format.extension}",
            format = format,
            width = processed.width,
            height = processed.height,
            uploadedAt = clock.now().epochSecond,
        )
        val thumbnail = thumbnailName(record.fileName)
        try {
            files.write(record.fileName, bytes)
            files.write(thumbnail, processed.thumbnail)
            transactor.transaction { images.insert(record) }
        } catch (e: Throwable) {
            files.delete(record.fileName)
            files.delete(thumbnail)
            throw e
        }
        record.toDto()
    }

    /**
     * Anonymous visitors only get images reachable from public content; the [isOwner] can fetch any image,
     * including staged and private ones. Anything else is not found.
     */
    suspend fun open(fileName: String, thumbnail: Boolean, isOwner: Boolean): ServedImage {
        if (!STORED_NAME.matches(fileName)) notFound()
        val allowed = transactor.transaction {
            val record = images.findByFileName(fileName) ?: return@transaction false
            isOwner || images.isPubliclyReferenced(record.id)
        }
        if (!allowed) notFound()
        val bytes = withContext(Dispatchers.IO) { files.read(if (thumbnail) thumbnailName(fileName) else fileName) } ?: notFound()
        val format = ImageFormat.fromExtension(fileName.substringAfterLast('.'))!!
        return ServedImage(bytes, format.contentType)
    }

    private companion object {
        val STORED_NAME = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)$")
    }
}
