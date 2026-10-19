package com.usuallycoding.portfolio.server.domain

/** What an upload may be, checked before anything is stored. */
class ImageLimits(val maxUploadBytes: Long, val maxImageEdge: Int, val maxImagePixels: Long)

/** An accepted image: its size and a smaller copy for lists. */
class ProcessedImage(val width: Int, val height: Int, val thumbnail: ByteArray)

/** Reads the picture inside an upload. Throws [DomainError.UnsupportedImage] or [DomainError.PayloadTooLarge]. */
interface ImageProcessor {
    fun process(bytes: ByteArray, format: ImageFormat, limits: ImageLimits): ProcessedImage
}

class StoredFile(val name: String, val modifiedEpochSecond: Long)

/** Where image files live. Names are plain file names, never paths. */
interface ImageFiles {
    fun write(name: String, bytes: ByteArray)

    /** Null if there is no such file. */
    fun read(name: String): ByteArray?

    /** Removes the file if it exists; true if there was one. */
    fun delete(name: String): Boolean
    fun list(): List<StoredFile>
}
