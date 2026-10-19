package com.usuallycoding.portfolio.server.service

import com.usuallycoding.portfolio.server.domain.Clock
import com.usuallycoding.portfolio.server.domain.ImageFiles
import com.usuallycoding.portfolio.server.domain.Transactor
import com.usuallycoding.portfolio.server.domain.thumbnailName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

data class CleanupResult(val orphanImages: Int, val strayFiles: Int)

/**
 * Removes images nothing references (replaced, deleted or abandoned uploads) once they are older than the
 * grace period, together with their thumbnails. Also removes stray files that have no database row,
 * which can be left behind by a crash between writing a file and recording it.
 */
class ImageCleanupService(
    private val transactor: Transactor,
    private val files: ImageFiles,
    private val clock: Clock,
    private val orphanGrace: Duration,
) {
    suspend fun run(): CleanupResult = withContext(Dispatchers.IO) {
        val cutoff = clock.now().minus(orphanGrace).epochSecond
        CleanupResult(removeOrphans(cutoff), removeStrayFiles(cutoff))
    }

    private suspend fun removeOrphans(cutoff: Long): Int {
        val candidates = transactor.transaction { images.unreferencedBefore(cutoff) }
        var removed = 0
        for (candidate in candidates) {
            // The delete re-checks the conditions, in case the image was attached since it was listed.
            val deleted = transactor.transaction { images.deleteIfUnreferenced(candidate.id, cutoff) }
            if (deleted) {
                // The row is gone first: a failure here leaves a stray file for the next sweep, never a dangling row.
                files.delete(candidate.fileName)
                files.delete(thumbnailName(candidate.fileName))
                removed++
            }
        }
        return removed
    }

    private suspend fun removeStrayFiles(cutoff: Long): Int {
        var removed = 0
        for (file in files.list()) {
            val id = imageIdOf(file.name) ?: continue // not one of ours: leave it alone
            if (file.modifiedEpochSecond >= cutoff) continue
            val known = transactor.transaction { images.find(id) != null }
            if (!known && files.delete(file.name)) removed++
        }
        return removed
    }

    private fun imageIdOf(name: String): String? = STORED_FILE.matchEntire(name)?.groupValues?.get(1)

    private companion object {
        val STORED_FILE = Regex("^([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})(?:_thumb)?\\.(?:jpg|png)$")
    }
}
