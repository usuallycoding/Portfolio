package com.usuallycoding.portfolio.server

import io.ktor.server.application.Application
import io.ktor.server.application.log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.name

data class CleanupResult(val orphanImages: Int, val strayFiles: Int)

/**
 * Removes images nothing references (replaced, deleted or abandoned uploads) once they are older than the
 * grace period, together with their thumbnails. Also removes stray files that have no database row,
 * which can be left behind by a crash between writing a file and recording it.
 */
class ImageCleanup(
    private val config: ServerConfig,
    private val database: Database,
    private val images: ImageStore,
    private val clock: Clock,
) {
    suspend fun run(): CleanupResult = withContext(Dispatchers.IO) {
        val cutoff = clock.now().minus(config.orphanGrace).epochSecond
        CleanupResult(removeOrphans(cutoff), removeStrayFiles(cutoff))
    }

    private fun removeOrphans(cutoff: Long): Int {
        val candidates = database.transactionBlocking { conn ->
            conn.prepareStatement("SELECT id, file_name FROM images WHERE uploaded_at < ? AND ${Images.UNREFERENCED}").use {
                it.setLong(1, cutoff)
                it.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.getString(1) to rs.getString(2)) } }
            }
        }
        var removed = 0
        for ((id, fileName) in candidates) {
            // Re-check the conditions in the DELETE itself, in case the image was attached since the SELECT.
            val deleted = database.transactionBlocking { conn ->
                conn.prepareStatement("DELETE FROM images WHERE id = ? AND uploaded_at < ? AND ${Images.UNREFERENCED}").use {
                    it.setString(1, id)
                    it.setLong(2, cutoff)
                    it.executeUpdate()
                }
            }
            if (deleted == 1) {
                // The row is gone first: a failure here leaves a stray file for the next sweep, never a dangling row.
                Files.deleteIfExists(images.fileFor(fileName, thumbnail = false))
                Files.deleteIfExists(images.fileFor(fileName, thumbnail = true))
                removed++
            }
        }
        return removed
    }

    private fun removeStrayFiles(cutoff: Long): Int {
        var removed = 0
        Files.newDirectoryStream(config.imagesDir).use { files ->
            for (file in files) {
                val id = imageIdOf(file) ?: continue // not one of ours: leave it alone
                if (Files.getLastModifiedTime(file).toInstant().epochSecond >= cutoff) continue
                val known = database.transactionBlocking { conn -> Images.find(conn, id) != null }
                if (!known && Files.deleteIfExists(file)) removed++
            }
        }
        return removed
    }

    private fun imageIdOf(file: Path): String? = STORED_FILE.matchEntire(file.name)?.groupValues?.get(1)

    private companion object {
        val STORED_FILE = Regex("^([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})(?:_thumb)?\\.(?:jpg|png)$")
    }
}

/**
 * Calls [action] after [initialDelay] and then every [interval], until cancelled.
 * A failing run is reported to [onError] and does not stop the schedule.
 */
suspend fun runPeriodically(
    initialDelay: Duration,
    interval: Duration,
    onError: (Exception) -> Unit,
    action: suspend () -> Unit,
) {
    delay(initialDelay.toMillis())
    while (currentCoroutineContext().isActive) {
        try {
            action()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onError(e)
        }
        delay(interval.toMillis())
    }
}

/** Runs the cleanup once a day for as long as the application is running. */
fun Application.startImageCleanup(context: ServerContext) {
    launch {
        runPeriodically(
            initialDelay = context.config.cleanupInitialDelay,
            interval = context.config.cleanupInterval,
            onError = { log.error("Image cleanup failed", it) },
        ) {
            val result = context.cleanup.run()
            log.info("Image cleanup removed ${result.orphanImages} orphan image(s) and ${result.strayFiles} stray file(s)")
        }
    }
}
