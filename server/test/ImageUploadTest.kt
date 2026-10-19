package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.server.data.*

import com.usuallycoding.portfolio.ApiException
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.listDirectoryEntries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ImageUploadTest {
    private fun TestEnv.imageFiles() = config.imagesDir.listDirectoryEntries()

    private fun TestEnv.imageRows(): Int = database.transactionBlocking { conn ->
        conn.createStatement().use { st -> st.executeQuery("SELECT COUNT(*) FROM images").use { it.next(); it.getInt(1) } }
    }

    private suspend fun TestEnv.loginOwner() {
        api.login(TEST_PASSWORD)
    }

    @Test
    fun ownerUploadsJpegAndGetsIdAndUniqueName() = withServer {
        loginOwner()
        val image = api.uploadImage(TestImages.jpeg(800, 450), "holiday.jpg", "image/jpeg")
        assertTrue(image.fileName.endsWith(".jpg"))
        assertEquals(image.id + ".jpg", image.fileName)
        assertEquals(800, image.width)
        assertEquals(450, image.height)
        assertEquals(1, imageRows())
    }

    @Test
    fun originalIsStoredByteForByte() = withServer {
        loginOwner()
        val bytes = TestImages.png(300, 200)
        val image = api.uploadImage(bytes, "a.png", "image/png")
        assertTrue(bytes.contentEquals(Files.readAllBytes(imageFile(image.fileName, thumbnail = false))))
    }

    @Test
    fun sameOriginalNameTwiceGetsDifferentFiles() = withServer {
        loginOwner()
        val a = api.uploadImage(TestImages.jpeg(100, 100), "same.jpg", "image/jpeg")
        val b = api.uploadImage(TestImages.jpeg(100, 100), "same.jpg", "image/jpeg")
        assertNotEquals(a.fileName, b.fileName)
        assertTrue(imageFiles().none { it.fileName.toString().contains("same") })
        assertEquals(4, imageFiles().size) // two originals + two thumbnails
    }

    @Test
    fun anonymousUploadIs401AndStoresNothing() = withServer {
        val response = raw.submitFormWithBinaryData(
            "/admin/images",
            formData { append("file", TestImages.jpeg(10, 10), Headers.build { append(HttpHeaders.ContentDisposition, "filename=\"a.jpg\"") }) },
        )
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(0, imageRows())
        assertEquals(0, imageFiles().size)
    }

    @Test
    fun nonImageRenamedToJpgIsRejected() = withServer {
        loginOwner()
        val e = assertFailsWith<ApiException> {
            api.uploadImage("this is just text".toByteArray(), "fake.jpg", "image/jpeg")
        }
        assertEquals(415, e.status)
        assertEquals(0, imageRows())
        assertEquals(0, imageFiles().size)
    }

    @Test
    fun declaredTypeIsIgnoredContentDecides() = withServer {
        loginOwner()
        val image = api.uploadImage(TestImages.png(40, 40), "x.jpg", "image/jpeg")
        assertTrue(image.fileName.endsWith(".png"))
    }

    @Test
    fun truncatedImageIsRejected() = withServer {
        loginOwner()
        val bytes = TestImages.jpeg(300, 300).let { it.copyOf(it.size / 4) }
        val e = assertFailsWith<ApiException> { api.uploadImage(bytes, "t.jpg", "image/jpeg") }
        assertEquals(415, e.status)
        assertEquals(0, imageFiles().size)
    }

    @Test
    fun fileAboveByteLimitIsRejected() = withServer(config = tempConfig().copy(maxUploadBytes = 2_000)) {
        loginOwner()
        val big = TestImages.png(600, 600).also { assertTrue(it.size > 2_000) }
        assertEquals(413, assertFailsWith<ApiException> { api.uploadImage(big, "big.png", "image/png") }.status)
        assertEquals(0, imageRows())
        assertEquals(0, imageFiles().size)
    }

    @Test
    fun imageAbovePixelLimitIsRejectedWithoutBeingStored() = withServer(config = tempConfig().copy(maxImageEdge = 500)) {
        loginOwner()
        assertEquals(413, assertFailsWith<ApiException> { api.uploadImage(TestImages.png(600, 100), "wide.png", "image/png") }.status)
        assertEquals(0, imageRows())
        assertEquals(0, imageFiles().size)
    }

    @Test
    fun missingFilePartIs400() = withServer {
        loginOwner()
        val response = raw.submitFormWithBinaryData(
            "/admin/images",
            formData { append("other", "value") },
        ) { headers.append(HttpHeaders.Authorization, "Bearer ${api.token}") }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun storingAndClockUseUploadTime() = runTest {
        // uploaded_at drives orphan cleanup, so it must follow the injected clock.
        var recorded = 0L
        withServer {
            api.login(TEST_PASSWORD)
            val image = api.uploadImage(TestImages.jpeg(50, 50), "a.jpg", "image/jpeg")
            recorded = database.transactionBlocking { conn -> SqliteImageRepository(conn).find(image.id)!!.uploadedAt }
            assertEquals(clock.instant.epochSecond, recorded)
        }
    }
}
