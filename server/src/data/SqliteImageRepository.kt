package com.usuallycoding.portfolio.server.data

import com.usuallycoding.portfolio.server.domain.ImageFormat
import com.usuallycoding.portfolio.server.domain.ImageRecord
import com.usuallycoding.portfolio.server.domain.ImageRepository
import java.sql.Connection
import java.sql.ResultSet

class SqliteImageRepository(private val conn: Connection) : ImageRepository {
    override fun find(id: String): ImageRecord? =
        conn.prepareStatement("SELECT $COLUMNS FROM images WHERE id = ?").use {
            it.setString(1, id)
            it.executeQuery().use { rs -> if (rs.next()) rs.toRecord() else null }
        }

    override fun findByFileName(fileName: String): ImageRecord? =
        conn.prepareStatement("SELECT $COLUMNS FROM images WHERE file_name = ?").use {
            it.setString(1, fileName)
            it.executeQuery().use { rs -> if (rs.next()) rs.toRecord() else null }
        }

    override fun insert(record: ImageRecord) {
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

    override fun isPubliclyReferenced(id: String): Boolean =
        conn.prepareStatement("SELECT 1 FROM images WHERE id = ? AND (${ImageSql.PUBLICLY_REFERENCED})").use {
            it.setString(1, id)
            it.executeQuery().use { rs -> rs.next() }
        }

    override fun unreferencedBefore(cutoff: Long): List<ImageRecord> =
        conn.prepareStatement("SELECT $COLUMNS FROM images WHERE uploaded_at < ? AND ${ImageSql.UNREFERENCED}").use {
            it.setLong(1, cutoff)
            it.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toRecord()) } }
        }

    override fun deleteIfUnreferenced(id: String, cutoff: Long): Boolean =
        conn.prepareStatement("DELETE FROM images WHERE id = ? AND uploaded_at < ? AND ${ImageSql.UNREFERENCED}").use {
            it.setString(1, id)
            it.setLong(2, cutoff)
            it.executeUpdate() == 1
        }

    private fun ResultSet.toRecord() = ImageRecord(
        id = getString("id"),
        fileName = getString("file_name"),
        format = ImageFormat.fromExtension(getString("format")) ?: error("Unknown stored format"),
        width = getInt("width"),
        height = getInt("height"),
        uploadedAt = getLong("uploaded_at"),
    )

    private companion object {
        const val COLUMNS = "id, file_name, format, width, height, uploaded_at"
    }
}
