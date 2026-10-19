package com.usuallycoding.portfolio.server.data

import com.usuallycoding.portfolio.server.domain.ImageFiles
import com.usuallycoding.portfolio.server.domain.StoredFile
import java.nio.file.Files
import java.nio.file.Path

class FileImageStorage(private val directory: Path) : ImageFiles {
    init {
        Files.createDirectories(directory)
    }

    override fun write(name: String, bytes: ByteArray) {
        Files.write(directory.resolve(name), bytes)
    }

    override fun read(name: String): ByteArray? =
        directory.resolve(name).let { if (Files.exists(it)) Files.readAllBytes(it) else null }

    override fun delete(name: String): Boolean = Files.deleteIfExists(directory.resolve(name))

    override fun list(): List<StoredFile> =
        Files.newDirectoryStream(directory).use { files ->
            files.map { StoredFile(it.fileName.toString(), Files.getLastModifiedTime(it).toInstant().epochSecond) }
        }
}
