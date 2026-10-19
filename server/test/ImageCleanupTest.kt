package com.usuallycoding.portfolio.server

import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.time.Duration
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageCleanupTest {
    private fun TestEnv.hasRow(id: String): Boolean =
        context.database.transactionBlocking { conn -> Images.find(conn, id) != null }

    private fun TestEnv.exists(fileName: String, thumbnail: Boolean = false) =
        context.images.fileFor(fileName, thumbnail).exists()

    private fun TestEnv.useAsProfilePicture(id: String) {
        context.database.transactionBlocking { conn ->
            conn.createStatement().use {
                it.execute("INSERT OR REPLACE INTO profile(id, name, description, profile_image_id) VALUES (1,'n','d','$id')")
            }
        }
    }

    @Test
    fun oldOrphanIsDeletedWithRowOriginalAndThumbnail() = withServer {
        api.login(TEST_PASSWORD)
        val image = api.uploadImage(TestImages.jpeg(50, 50), "a.jpg", "image/jpeg")
        clock.advance(Duration.ofMinutes(61))

        val result = context.cleanup.run()

        assertEquals(1, result.orphanImages)
        assertFalse(hasRow(image.id))
        assertFalse(exists(image.fileName))
        assertFalse(exists(image.fileName, thumbnail = true))
    }

    @Test
    fun freshUploadIsKept() = withServer {
        api.login(TEST_PASSWORD)
        val image = api.uploadImage(TestImages.jpeg(50, 50), "a.jpg", "image/jpeg")
        clock.advance(Duration.ofMinutes(10))

        assertEquals(0, context.cleanup.run().orphanImages)
        assertTrue(hasRow(image.id))
        assertTrue(exists(image.fileName))
    }

    @Test
    fun exactlySixtyMinutesIsStillKept() = withServer {
        api.login(TEST_PASSWORD)
        val image = api.uploadImage(TestImages.jpeg(50, 50), "a.jpg", "image/jpeg")
        clock.advance(Duration.ofMinutes(60))
        assertEquals(0, context.cleanup.run().orphanImages)
        assertTrue(hasRow(image.id))
    }

    @Test
    fun referencedImageIsKeptRegardlessOfAge() = withServer {
        api.login(TEST_PASSWORD)
        val image = api.uploadImage(TestImages.jpeg(50, 50), "a.jpg", "image/jpeg")
        useAsProfilePicture(image.id)
        clock.advance(Duration.ofDays(30))

        assertEquals(0, context.cleanup.run().orphanImages)
        assertTrue(hasRow(image.id))
        assertTrue(exists(image.fileName))
        assertTrue(exists(image.fileName, thumbnail = true))
    }

    @Test
    fun imageReferencedOnlyByAProjectGalleryIsKept() = withServer {
        api.login(TEST_PASSWORD)
        val image = api.uploadImage(TestImages.jpeg(50, 50), "a.jpg", "image/jpeg")
        context.database.transactionBlocking { conn ->
            conn.createStatement().use {
                it.execute("INSERT INTO projects(title,created_at,is_public,summary,position) VALUES ('p','2026-01-01',0,'',0)")
                it.execute("INSERT INTO project_images VALUES (1,'${image.id}',0)")
            }
        }
        clock.advance(Duration.ofDays(2))
        assertEquals(0, context.cleanup.run().orphanImages)
    }

    @Test
    fun replacedImageBecomesOrphanAndIsCollectedLater() = withServer {
        api.login(TEST_PASSWORD)
        val old = api.uploadImage(TestImages.jpeg(50, 50), "old.jpg", "image/jpeg")
        val new = api.uploadImage(TestImages.jpeg(50, 50), "new.jpg", "image/jpeg")
        useAsProfilePicture(old.id)
        clock.advance(Duration.ofHours(2))
        useAsProfilePicture(new.id) // replaces: the old one is no longer referenced

        assertEquals(1, context.cleanup.run().orphanImages)
        assertFalse(hasRow(old.id))
        assertTrue(hasRow(new.id))
    }

    @Test
    fun strayOldFilesWithoutARowAreRemovedButUnknownFilesAndFreshOnesAreNot() = withServer {
        val dir = context.config.imagesDir
        val stray = dir.resolve("11111111-1111-1111-1111-111111111111.jpg").also { it.writeText("x") }
        val strayThumb = dir.resolve("11111111-1111-1111-1111-111111111111_thumb.jpg").also { it.writeText("x") }
        val fresh = dir.resolve("22222222-2222-2222-2222-222222222222.png").also { it.writeText("x") }
        val foreign = dir.resolve("notes.txt").also { it.writeText("keep me") }
        val old = FileTime.from(clock.instant.minus(Duration.ofHours(3)))
        listOf(stray, strayThumb, foreign).forEach { Files.setLastModifiedTime(it, old) }
        Files.setLastModifiedTime(fresh, FileTime.from(clock.instant.minus(Duration.ofMinutes(5))))

        assertEquals(2, context.cleanup.run().strayFiles)
        assertFalse(stray.exists())
        assertFalse(strayThumb.exists())
        assertTrue(fresh.exists())
        assertTrue(foreign.exists())
    }

    @Test
    fun knownFilesAreNeverTreatedAsStray() = withServer {
        api.login(TEST_PASSWORD)
        val image = api.uploadImage(TestImages.jpeg(50, 50), "a.jpg", "image/jpeg")
        useAsProfilePicture(image.id)
        val old = FileTime.from(clock.instant.minus(Duration.ofDays(5)))
        listOf(false, true).forEach { Files.setLastModifiedTime(context.images.fileFor(image.fileName, it), old) }

        assertEquals(0, context.cleanup.run().strayFiles)
        assertTrue(exists(image.fileName))
    }

    @Test
    fun runningTwiceIsHarmless() = withServer {
        api.login(TEST_PASSWORD)
        api.uploadImage(TestImages.jpeg(50, 50), "a.jpg", "image/jpeg")
        clock.advance(Duration.ofHours(2))
        assertEquals(1, context.cleanup.run().orphanImages)
        assertEquals(CleanupResult(0, 0), context.cleanup.run())
    }
}
