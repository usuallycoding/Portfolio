package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.ImageDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

sealed interface UploadStatus {
    data object Queued : UploadStatus
    data object Preparing : UploadStatus
    data class Uploading(val progress: Float) : UploadStatus
    data object Done : UploadStatus
    data class Failed(val message: String) : UploadStatus
}

/** One image in a project's gallery: either already stored on the server or on its way there. */
data class UploadItem(
    val key: Long,
    val name: String,
    val status: UploadStatus,
    /** Set once the image exists on the server. */
    val image: ImageDto? = null,
) {
    val isSettled: Boolean get() = status is UploadStatus.Done || status is UploadStatus.Failed
}

/** A file chosen by the owner. Its bytes are only read when its turn to upload comes. */
class PendingFile(val name: String, val read: suspend () -> ByteArray)

/**
 * The ordered gallery of a project. Files are prepared (cropped to the size cap) and uploaded one per
 * request, a few at a time; each has its own state, and a failed file can be retried without touching
 * the others. The list order is the order the owner sees and saves.
 */
class UploadQueue(
    private val scope: CoroutineScope,
    private val concurrency: Int = 3,
    private val prepare: suspend (name: String, bytes: ByteArray) -> PreparedImage,
    private val upload: suspend (name: String, image: PreparedImage, onProgress: (Float) -> Unit) -> ImageDto,
) {
    private val _items = MutableStateFlow<List<UploadItem>>(emptyList())
    val items: StateFlow<List<UploadItem>> = _items.asStateFlow()

    private val permits = Semaphore(concurrency)
    private val jobs = mutableMapOf<Long, Job>()
    private val sources = mutableMapOf<Long, PendingFile>()
    private var nextKey = 0L

    val isBusy: Boolean get() = _items.value.any { !it.isSettled }
    val failedCount: Int get() = _items.value.count { it.status is UploadStatus.Failed }

    /** Images already on the server, in order. */
    fun setExisting(images: List<ImageDto>) {
        _items.value = images.map { UploadItem(nextKey++, it.fileName, UploadStatus.Done, it) }
    }

    /** The server-side images of every finished item, in gallery order. Failed and unfinished ones are left out. */
    fun uploadedImages(): List<ImageDto> = _items.value.mapNotNull { it.image }

    fun enqueue(files: List<PendingFile>) {
        val added = files.map { UploadItem(nextKey++, it.name, UploadStatus.Queued) }
        added.zip(files).forEach { (item, file) -> sources[item.key] = file }
        _items.update { it + added }
        added.forEach { start(it.key) }
    }

    fun retry(key: Long) {
        val item = _items.value.firstOrNull { it.key == key } ?: return
        if (item.status !is UploadStatus.Failed || key !in sources) return
        setStatus(key, UploadStatus.Queued)
        start(key)
    }

    fun remove(key: Long) {
        jobs.remove(key)?.cancel()
        sources.remove(key)
        _items.update { list -> list.filterNot { it.key == key } }
    }

    fun move(key: Long, delta: Int) {
        _items.update { list ->
            val index = list.indexOfFirst { it.key == key }
            if (index < 0) list else list.moved(index, delta)
        }
    }

    private fun start(key: Long) {
        jobs[key] = scope.launch {
            permits.withPermit {
                try {
                    val file = sources.getValue(key)
                    setStatus(key, UploadStatus.Preparing)
                    val bytes = file.read()
                    val prepared = prepare(file.name, bytes)
                    setStatus(key, UploadStatus.Uploading(0f))
                    val image = upload(file.name, prepared) { progress -> setStatus(key, UploadStatus.Uploading(progress)) }
                    _items.update { list -> list.map { if (it.key == key) it.copy(status = UploadStatus.Done, image = image) else it } }
                    sources.remove(key)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    setStatus(key, UploadStatus.Failed(e.adminMessage()))
                }
            }
        }
    }

    private fun setStatus(key: Long, status: UploadStatus) {
        _items.update { list -> list.map { if (it.key == key) it.copy(status = status) else it } }
    }
}

/** Raised when a chosen file cannot be read as an image. */
class NotAnImageException(name: String) : Exception("$name is not an image this app can read.")
