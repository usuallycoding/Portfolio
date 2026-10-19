package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.ImageDto
import java.sql.Connection
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

object Images {
    /** SQL predicate (over the `images` table) true when nothing references the image. */
    const val UNREFERENCED = """
        NOT EXISTS (SELECT 1 FROM profile WHERE profile_image_id = images.id OR cover_image_id = images.id)
        AND NOT EXISTS (SELECT 1 FROM social_links WHERE icon_image_id = images.id)
        AND NOT EXISTS (SELECT 1 FROM projects WHERE thumbnail_image_id = images.id)
        AND NOT EXISTS (SELECT 1 FROM project_images WHERE image_id = images.id)"""

    /** SQL predicate true when an anonymous visitor can reach the image through public content. */
    const val PUBLICLY_REFERENCED = """
        EXISTS (SELECT 1 FROM profile WHERE profile_image_id = images.id OR cover_image_id = images.id)
        OR EXISTS (SELECT 1 FROM social_links WHERE icon_image_id = images.id AND is_public = 1)
        OR EXISTS (SELECT 1 FROM projects WHERE thumbnail_image_id = images.id AND is_public = 1)
        OR EXISTS (SELECT 1 FROM project_images pi JOIN projects p ON p.id = pi.project_id
                   WHERE pi.image_id = images.id AND p.is_public = 1)"""

    private const val COLUMNS = "id, file_name, format, width, height, uploaded_at"

    private fun java.sql.ResultSet.toRecord() = ImageRecord(
        id = getString("id"),
        fileName = getString("file_name"),
        format = ImageFormat.fromExtension(getString("format")) ?: error("Unknown stored format"),
        width = getInt("width"),
        height = getInt("height"),
        uploadedAt = getLong("uploaded_at"),
    )

    fun find(conn: Connection, id: String): ImageRecord? =
        conn.prepareStatement("SELECT $COLUMNS FROM images WHERE id = ?").use {
            it.setString(1, id)
            it.executeQuery().use { rs -> if (rs.next()) rs.toRecord() else null }
        }

    fun findByFileName(conn: Connection, fileName: String): ImageRecord? =
        conn.prepareStatement("SELECT $COLUMNS FROM images WHERE file_name = ?").use {
            it.setString(1, fileName)
            it.executeQuery().use { rs -> if (rs.next()) rs.toRecord() else null }
        }

    fun isPubliclyReferenced(conn: Connection, id: String): Boolean =
        conn.prepareStatement("SELECT 1 FROM images WHERE id = ? AND ($PUBLICLY_REFERENCED)").use {
            it.setString(1, id)
            it.executeQuery().use { rs -> rs.next() }
        }

    fun insert(conn: Connection, record: ImageRecord) {
        conn.prepareStatement("INSERT INTO images($COLUMNS) VALUES (?,?,?,?,?,?)").use {
            it.setString(1, record.id)
            it.setString(2, record.fileName)
            it.setString(3, record.format.extension)
            it.setInt(4, record.width)
            it.setInt(5, record.height)
            it.setLong(6, record.uploadedAt)
            it.executeUpdate()
        }
    }

    /**
     * Resolves an image id to attach to a field. Returns null for a null id (clearing the field).
     * Throws a 400 for an unknown image or, when [ratio] is given, an image outside the tolerance.
     */
    fun requireForAttach(conn: Connection, id: String?, ratio: AspectRatio?, tolerance: Double, field: String): ImageRecord? {
        if (id == null) return null
        val record = find(conn, id) ?: badRequest("Unknown image for $field")
        if (ratio != null && !ratio.matches(record.width, record.height, tolerance)) {
            badRequest("$field must have a ${ratio.label} aspect ratio (got ${record.width}x${record.height})")
        }
        return record
    }
}
