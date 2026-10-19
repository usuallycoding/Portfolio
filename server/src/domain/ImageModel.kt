package com.usuallycoding.portfolio.server.domain

import com.usuallycoding.portfolio.ImageDto
import kotlin.math.abs

enum class ImageFormat(val extension: String, val contentType: String, val readerName: String) {
    JPEG("jpg", "image/jpeg", "jpeg"),
    PNG("png", "image/png", "png");

    companion object {
        fun fromExtension(extension: String): ImageFormat? = entries.firstOrNull { it.extension == extension }

        /** Detects the format from the file's leading bytes, never from the declared type or name. */
        fun sniff(bytes: ByteArray): ImageFormat? = when {
            bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> JPEG
            bytes.size >= 8 && PNG_MAGIC.indices.all { bytes[it] == PNG_MAGIC[it] } -> PNG
            else -> null
        }

        private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}

enum class AspectRatio(val width: Int, val height: Int) {
    SQUARE(1, 1),
    COVER(3, 1),
    THUMBNAIL(16, 9);

    val label: String get() = "$width:$height"

    fun matches(imageWidth: Int, imageHeight: Int, tolerance: Double): Boolean {
        val expected = width.toDouble() / height
        val actual = imageWidth.toDouble() / imageHeight
        return abs(actual - expected) / expected <= tolerance
    }
}

data class ImageRecord(
    val id: String,
    val fileName: String,
    val format: ImageFormat,
    val width: Int,
    val height: Int,
    val uploadedAt: Long,
) {
    fun toDto() = ImageDto(id, fileName, width, height)
}

fun thumbnailName(fileName: String): String = fileName.substringBeforeLast('.') + "_thumb." + fileName.substringAfterLast('.')
