package com.usuallycoding.portfolio.server.service

import com.usuallycoding.portfolio.server.MutableClock
import com.usuallycoding.portfolio.server.domain.DomainError
import com.usuallycoding.portfolio.server.domain.ImageLimits
import com.usuallycoding.portfolio.server.domain.Transactor
import com.usuallycoding.portfolio.server.domain.thumbnailName
import kotlinx.coroutines.test.runTest
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageServicesTest {
    private val clock = MutableClock()
    private val store = FakeTransactor()
    private val files = FakeImageFiles()
    private val limits = ImageLimits(maxUploadBytes = 1000, maxImageEdge = 8000, maxImagePixels = 40_000_000)
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 1, 7)

    private fun service(transactor: Transactor = store, processor: FakeImageProcessor = FakeImageProcessor()) =
        ImageService(transactor, files, processor, clock, limits)

    // --- storing ---

    @Test
    fun anUploadKeepsTheOriginalBytesAndAddsAThumbnailAndARecord() = runTest {
        val image = service().store(jpeg)
        assertEquals(800, image.width)
        assertEquals(600, image.height)
        assertContentEquals(jpeg, files.read(image.fileName))
        assertContentEquals("thumb".toByteArray(), files.read(thumbnailName(image.fileName)))
        assertEquals(image.id, store.state.images.keys.single())
        assertEquals(clock.now().epochSecond, store.state.images.getValue(image.id).uploadedAt)
    }

    @Test
    fun tooLargeAndNotAnImageAreRejectedBeforeAnythingIsStored() = runTest {
        assertFailsWith<DomainError.PayloadTooLarge> { service().store(ByteArray(1001)) }
        assertFailsWith<DomainError.UnsupportedImage> { service().store("GIF89a".toByteArray()) }
        assertFailsWith<DomainError.UnsupportedImage> { service().store(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0, 0)) }
        assertTrue(files.names.isEmpty())
        assertTrue(store.state.images.isEmpty())
    }

    @Test
    fun aFailureWhileRecordingTheImageRemovesItsFiles() = runTest {
        assertFailsWith<IllegalStateException> { service(FailingTransactor()).store(jpeg) }
        assertTrue(files.names.isEmpty(), "files left behind: ${files.names}")
    }

    @Test
    fun aFailureWhileWritingTheThumbnailRemovesTheOriginal() = runTest {
        files.failWritesAfter = 1
        assertFailsWith<IllegalStateException> { service().store(jpeg) }
        assertTrue(files.names.isEmpty())
        assertTrue(store.state.images.isEmpty())
    }

    // --- serving ---

    private suspend fun stored(): String = service().store(jpeg).fileName.also { name ->
        store.publicImageIds = setOf(store.state.images.values.single { it.fileName == name }.id)
    }

    @Test
    fun visitorsGetPublicImagesAndThumbnails() = runTest {
        val name = stored()
        val full = service().open(name, thumbnail = false, isOwner = false)
        assertContentEquals(jpeg, full.bytes)
        assertEquals("image/jpeg", full.contentType)
        assertContentEquals("thumb".toByteArray(), service().open(name, thumbnail = true, isOwner = false).bytes)
    }

    @Test
    fun onlyTheOwnerGetsImagesNotInPublicContent() = runTest {
        val name = stored()
        store.publicImageIds = emptySet()
        assertFailsWith<DomainError.NotFound> { service().open(name, thumbnail = false, isOwner = false) }
        assertNotNull(service().open(name, thumbnail = false, isOwner = true))
    }

    @Test
    fun badOrUnknownNamesAndMissingFilesAreNotFound() = runTest {
        val name = stored()
        assertFailsWith<DomainError.NotFound> { service().open("../secret.jpg", false, isOwner = true) }
        assertFailsWith<DomainError.NotFound> { service().open("00000000-0000-0000-0000-000000000000.jpg", false, isOwner = true) }
        files.delete(name)
        assertFailsWith<DomainError.NotFound> { service().open(name, false, isOwner = true) }
    }

    // --- cleanup ---

    private val grace = Duration.ofMinutes(60)
    private val cleanup = ImageCleanupService(store, files, clock, grace)
    private val now get() = clock.now().epochSecond
    private val old get() = now - grace.seconds - 10
    private val young get() = now - 10

    private fun addStored(id: String, uploadedAt: Long) {
        store.addImage(id, 100, 100, uploadedAt)
        files.put("$id.jpg", modified = uploadedAt)
        files.put("${id}_thumb.jpg", modified = uploadedAt)
    }

    private val a = "00000000-0000-0000-0000-00000000000a"
    private val b = "00000000-0000-0000-0000-00000000000b"
    private val c = "00000000-0000-0000-0000-00000000000c"

    @Test
    fun oldUnreferencedImagesAreRemovedWithTheirFiles() = runTest {
        addStored(a, old)
        val result = cleanup.run()
        assertEquals(1, result.orphanImages)
        assertTrue(store.state.images.isEmpty())
        assertTrue(files.names.isEmpty())
    }

    @Test
    fun youngAndReferencedImagesAreKept() = runTest {
        addStored(a, young)
        addStored(b, old)
        addStored(c, old)
        FakeLinksUse.attach(store, iconId = b)
        val result = cleanup.run()
        assertEquals(1, result.orphanImages)
        assertEquals(setOf(a, b), store.state.images.keys)
        assertFalse("$c.jpg" in files.names)
        assertTrue("$b.jpg" in files.names && "$a.jpg" in files.names)
    }

    @Test
    fun strayFilesWithNoRowAreRemovedOnceOldEnough() = runTest {
        files.put("$a.jpg", modified = old)
        files.put("${a}_thumb.jpg", modified = old)
        files.put("$b.jpg", modified = young)
        files.put("notes.txt", modified = old)
        addStored(c, young)
        val result = cleanup.run()
        assertEquals(2, result.strayFiles)
        assertEquals(setOf("$b.jpg", "notes.txt", "$c.jpg", "${c}_thumb.jpg"), files.names)
        assertNull(store.state.images[a])
    }
}

/** Puts one link in the fake data so an image counts as in use. */
private object FakeLinksUse {
    fun attach(store: FakeTransactor, iconId: String) {
        store.state = store.state.copy(
            links = store.state.links + (1L to com.usuallycoding.portfolio.server.domain.LinkFields("L", "https://a.b", true, iconId)),
        )
    }
}
