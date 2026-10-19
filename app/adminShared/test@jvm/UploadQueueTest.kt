package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.ImageDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UploadQueueTest {
    private class Fixture(scope: TestScope, concurrency: Int = 3) {
        var active = 0
        var maxActive = 0
        val uploaded = mutableListOf<String>()
        var failOn: (String, Int) -> Boolean = { _, _ -> false }
        val attempts = mutableMapOf<String, Int>()
        var durations: (String) -> Long = { 100 }
        val progressSeen = mutableListOf<Float>()

        val queue = UploadQueue(
            scope = scope,
            concurrency = concurrency,
            prepare = { _, bytes -> PreparedImage(bytes, "image/jpeg", "jpg", 10, 10) },
            upload = { name, _, onProgress ->
                val attempt = (attempts[name] ?: 0) + 1
                attempts[name] = attempt
                active++; maxActive = maxOf(maxActive, active)
                try {
                    onProgress(0.5f)
                    delay(durations(name))
                    if (failOn(name, attempt)) error("boom")
                } finally {
                    active--
                }
                uploaded += name
                ImageDto("id-$name", "$name.jpg", 10, 10)
            },
        )

        fun files(vararg names: String) = names.map { PendingFile(it) { it.toByteArray() } }
        fun statuses() = queue.items.value.associate { it.name to it.status }
    }

    @Test
    fun neverRunsMoreThanThreeUploadsAtOnce() = runTest {
        val f = Fixture(this)
        f.queue.enqueue(f.files(*Array(10) { "f$it" }))
        advanceUntilIdle()

        assertEquals(3, f.maxActive)
        assertEquals(10, f.uploaded.size)
        assertTrue(f.queue.items.value.all { it.status == UploadStatus.Done })
    }

    @Test
    fun theConcurrencyLimitIsConfigurable() = runTest {
        val f = Fixture(this, concurrency = 1)
        f.queue.enqueue(f.files("a", "b", "c"))
        advanceUntilIdle()
        assertEquals(1, f.maxActive)
    }

    @Test
    fun oneFailureLeavesTheOthersFineAndOnlyThatFileIsRetried() = runTest {
        val f = Fixture(this)
        f.failOn = { name, attempt -> name == "f4" && attempt == 1 }
        f.queue.enqueue(f.files(*Array(10) { "f$it" }))
        advanceUntilIdle()

        assertEquals(9, f.queue.items.value.count { it.status == UploadStatus.Done })
        assertIs<UploadStatus.Failed>(f.statuses().getValue("f4"))
        assertEquals(1, f.queue.failedCount)
        assertTrue(f.queue.items.value.none { !it.isSettled }, "the batch finishes even though one file failed")
        assertEquals(9, f.queue.uploadedImages().size, "the failed file has no server image")

        val failedKey = f.queue.items.value.first { it.name == "f4" }.key
        f.queue.retry(failedKey)
        advanceUntilIdle()

        assertEquals(UploadStatus.Done, f.statuses().getValue("f4"))
        assertEquals(0, f.queue.failedCount)
        assertEquals(2, f.attempts.getValue("f4"))
        assertTrue(f.attempts.filterKeys { it != "f4" }.values.all { it == 1 }, "other files were not re-uploaded")
        assertEquals(10, f.queue.uploadedImages().size)
    }

    @Test
    fun retryingAFileThatIsNotFailedDoesNothing() = runTest {
        val f = Fixture(this)
        f.queue.enqueue(f.files("a"))
        advanceUntilIdle()
        f.queue.retry(f.queue.items.value.single().key)
        advanceUntilIdle()
        assertEquals(1, f.attempts.getValue("a"))
    }

    @Test
    fun galleryOrderIsTheListOrderNotTheCompletionOrder() = runTest {
        val f = Fixture(this)
        f.durations = { name -> if (name == "slow") 1_000 else 10 }
        f.queue.enqueue(f.files("slow", "fast1", "fast2"))
        advanceUntilIdle()

        assertEquals(listOf("fast1", "fast2", "slow"), f.uploaded, "slow finished last")
        assertEquals(listOf("slow", "fast1", "fast2"), f.queue.uploadedImages().map { it.id.removePrefix("id-") })
    }

    @Test
    fun movingItemsReordersTheSavedGallery() = runTest {
        val f = Fixture(this)
        f.queue.enqueue(f.files("a", "b", "c"))
        advanceUntilIdle()

        f.queue.move(f.queue.items.value[2].key, -2) // c to the front
        assertEquals(listOf("c", "a", "b"), f.queue.items.value.map { it.name })
        f.queue.move(f.queue.items.value[0].key, -1) // already first
        assertEquals(listOf("c", "a", "b"), f.queue.items.value.map { it.name })
        assertEquals(listOf("id-c", "id-a", "id-b"), f.queue.uploadedImages().map { it.id })
    }

    @Test
    fun removingAQueuedItemStopsItBeingUploaded() = runTest {
        val f = Fixture(this, concurrency = 1)
        f.queue.enqueue(f.files("a", "b", "c"))
        runCurrent()
        f.queue.remove(f.queue.items.value.first { it.name == "b" }.key)
        advanceUntilIdle()

        assertEquals(listOf("a", "c"), f.uploaded)
        assertEquals(listOf("a", "c"), f.queue.items.value.map { it.name })
    }

    @Test
    fun removingAnItemMidUploadCancelsIt() = runTest {
        val f = Fixture(this)
        f.durations = { 1_000 }
        f.queue.enqueue(f.files("a"))
        advanceTimeBy(100)
        assertIs<UploadStatus.Uploading>(f.queue.items.value.single().status)
        f.queue.remove(f.queue.items.value.single().key)
        advanceUntilIdle()
        assertTrue(f.uploaded.isEmpty())
        assertEquals(0, f.active)
    }

    @Test
    fun existingImagesAreAlreadyDoneAndKeepTheirOrder() = runTest {
        val f = Fixture(this)
        val existing = listOf(ImageDto("1", "1.jpg", 5, 5), ImageDto("2", "2.jpg", 5, 5))
        f.queue.setExisting(existing)
        assertFalse(f.queue.isBusy)
        assertEquals(existing, f.queue.uploadedImages())

        f.queue.enqueue(f.files("new"))
        assertTrue(f.queue.isBusy)
        advanceUntilIdle()
        assertEquals(listOf("1", "2", "id-new"), f.queue.uploadedImages().map { it.id })
    }

    @Test
    fun busyUntilEveryFileSettles() = runTest {
        val f = Fixture(this)
        assertFalse(f.queue.isBusy)
        f.queue.enqueue(f.files("a", "b"))
        assertTrue(f.queue.isBusy)
        advanceTimeBy(50)
        assertTrue(f.queue.isBusy)
        advanceUntilIdle()
        assertFalse(f.queue.isBusy)
    }

    @Test
    fun progressIsReportedPerFile() = runTest {
        val f = Fixture(this)
        f.durations = { 1_000 }
        f.queue.enqueue(f.files("a"))
        advanceTimeBy(100)
        assertEquals(0.5f, (f.queue.items.value.single().status as UploadStatus.Uploading).progress)
    }

    @Test
    fun anUnreadableFileFailsWithoutHurtingOthers() = runTest {
        val f = Fixture(this)
        f.queue.enqueue(listOf(PendingFile("bad") { throw NotAnImageException("bad") }) + f.files("ok"))
        advanceUntilIdle()
        assertEquals("bad is not an image this app can read.", (f.statuses().getValue("bad") as UploadStatus.Failed).message)
        assertEquals(UploadStatus.Done, f.statuses().getValue("ok"))
    }
}
