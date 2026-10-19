package com.usuallycoding.portfolio.server.service

import com.usuallycoding.portfolio.server.domain.DomainError
import com.usuallycoding.portfolio.server.domain.ImageFiles
import com.usuallycoding.portfolio.server.domain.ImageFormat
import com.usuallycoding.portfolio.server.domain.ImageLimits
import com.usuallycoding.portfolio.server.domain.ImageProcessor
import com.usuallycoding.portfolio.server.domain.ProcessedImage
import com.usuallycoding.portfolio.server.domain.StoredFile
import com.usuallycoding.portfolio.server.domain.Repositories
import com.usuallycoding.portfolio.server.domain.Transactor

class FakeImageFiles : ImageFiles {
    private class Entry(val bytes: ByteArray, val modified: Long)

    private val entries = linkedMapOf<String, Entry>()
    var failWritesAfter: Int? = null
    private var writes = 0

    val names: Set<String> get() = entries.keys

    fun put(name: String, bytes: ByteArray = byteArrayOf(1), modified: Long = 0) {
        entries[name] = Entry(bytes, modified)
    }

    override fun write(name: String, bytes: ByteArray) {
        if (failWritesAfter?.let { writes >= it } == true) error("disk full")
        writes++
        entries[name] = Entry(bytes, 0)
    }

    override fun read(name: String) = entries[name]?.bytes
    override fun delete(name: String) = entries.remove(name) != null
    override fun list() = entries.map { StoredFile(it.key, it.value.modified) }
}

/** Reports a fixed size and a recognisable thumbnail; bytes starting with 0 are "not an image". */
class FakeImageProcessor(private val width: Int = 800, private val height: Int = 600) : ImageProcessor {
    override fun process(bytes: ByteArray, format: ImageFormat, limits: ImageLimits): ProcessedImage {
        if (bytes.size > 3 && bytes[3] == 0.toByte() && bytes[4] == 0.toByte()) throw DomainError.UnsupportedImage("not readable")
        return ProcessedImage(width, height, "thumb".toByteArray())
    }
}

/** A database that is down: every unit of work fails. */
class FailingTransactor : Transactor {
    override suspend fun <T> transaction(block: Repositories.() -> T): T = error("database is down")
}
