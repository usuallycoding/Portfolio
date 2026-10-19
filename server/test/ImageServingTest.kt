package com.usuallycoding.portfolio.server

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImageServingTest {
    /** Makes [imageId] the profile picture, so anonymous visitors may see it. */
    private fun TestEnv.makePublic(imageId: String) {
        context.database.transactionBlocking { conn ->
            conn.createStatement().use {
                it.execute("INSERT OR REPLACE INTO profile(id, name, description, profile_image_id) VALUES (1, 'n', 'd', '$imageId')")
            }
        }
    }

    @Test
    fun originalIsServedUnchanged() = withServer {
        api.login(TEST_PASSWORD)
        val bytes = TestImages.jpeg(900, 600)
        val image = api.uploadImage(bytes, "a.jpg", "image/jpeg")
        makePublic(image.id)

        val response = raw.get("/images/${image.fileName}")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Image.JPEG, response.contentType()?.withoutParameters())
        assertTrue(bytes.contentEquals(response.bodyAsBytes()))
    }

    @Test
    fun thumbnailIsSmallerAndKeepsAspectRatio() = withServer {
        api.login(TEST_PASSWORD)
        val image = api.uploadImage(TestImages.jpeg(1600, 800), "a.jpg", "image/jpeg")
        makePublic(image.id)

        val thumb = TestImages.read(raw.get("/images/${image.fileName}?size=thumb").bodyAsBytes())
        assertEquals(400, thumb.width)
        assertEquals(200, thumb.height)
    }

    @Test
    fun smallImagesAreNotUpscaled() = withServer {
        api.login(TEST_PASSWORD)
        val image = api.uploadImage(TestImages.png(120, 60), "a.png", "image/png")
        makePublic(image.id)

        val thumb = TestImages.read(raw.get("/images/${image.fileName}?size=thumb").bodyAsBytes())
        assertEquals(120, thumb.width)
        assertEquals(60, thumb.height)
    }

    @Test
    fun pngThumbnailKeepsTransparencyAndFormat() = withServer {
        api.login(TEST_PASSWORD)
        val image = api.uploadImage(TestImages.png(800, 800, transparentLeftHalf = true), "icon.png", "image/png")
        makePublic(image.id)

        val response = raw.get("/images/${image.fileName}?size=thumb")
        assertEquals(ContentType.Image.PNG, response.contentType()?.withoutParameters())
        val thumb = TestImages.read(response.bodyAsBytes())
        assertTrue(thumb.colorModel.hasAlpha())
        assertEquals(0, thumb.getRGB(10, 200) ushr 24, "left side should stay transparent")
        assertEquals(255, thumb.getRGB(390, 200) ushr 24, "right side should stay opaque")
    }

    @Test
    fun jpegThumbnailIsJpeg() = withServer {
        api.login(TEST_PASSWORD)
        val image = api.uploadImage(TestImages.jpeg(1000, 1000), "a.jpg", "image/jpeg")
        makePublic(image.id)
        val response = raw.get("/images/${image.fileName}?size=thumb")
        assertEquals(ContentType.Image.JPEG, response.contentType()?.withoutParameters())
    }

    @Test
    fun unknownAndMalformedNamesAre404() = withServer {
        assertEquals(HttpStatusCode.NotFound, raw.get("/images/00000000-0000-0000-0000-000000000000.jpg").status)
        assertEquals(HttpStatusCode.NotFound, raw.get("/images/..%2F..%2Fetc%2Fpasswd").status)
        assertEquals(HttpStatusCode.NotFound, raw.get("/images/not-a-uuid.jpg").status)
    }

    @Test
    fun publicResponsesAreCachedForever() = withServer {
        api.login(TEST_PASSWORD)
        val image = api.uploadImage(TestImages.jpeg(100, 100), "a.jpg", "image/jpeg")
        makePublic(image.id)
        val cache = assertNotNull(raw.get("/images/${image.fileName}").headers[HttpHeaders.CacheControl])
        assertTrue("max-age=31536000" in cache && "public" in cache)
    }

    // --- visibility (4.3) ---

    @Test
    fun stagedImageIsHiddenFromAnonymousButVisibleToOwner() = withServer {
        api.login(TEST_PASSWORD)
        val image = api.uploadImage(TestImages.jpeg(100, 100), "a.jpg", "image/jpeg")
        assertEquals(HttpStatusCode.NotFound, raw.get("/images/${image.fileName}").status)
        assertEquals(HttpStatusCode.NotFound, raw.get("/images/${image.fileName}?size=thumb").status)

        val owner = raw.get("/images/${image.fileName}") { bearerAuth(api.token!!) }
        assertEquals(HttpStatusCode.OK, owner.status)
        assertTrue("private" in owner.headers[HttpHeaders.CacheControl].orEmpty())
    }

    @Test
    fun invalidTokenIsTreatedAsAnonymous() = withServer {
        api.login(TEST_PASSWORD)
        val image = api.uploadImage(TestImages.jpeg(100, 100), "a.jpg", "image/jpeg")
        assertEquals(HttpStatusCode.NotFound, raw.get("/images/${image.fileName}") { bearerAuth("bogus") }.status)
    }

    @Test
    fun imageOnlyUsedByPrivateContentIsHidden() = withServer {
        api.login(TEST_PASSWORD)
        val iconPrivate = api.uploadImage(TestImages.png(50, 50), "a.png", "image/png")
        val iconPublic = api.uploadImage(TestImages.png(50, 50), "b.png", "image/png")
        val thumbPrivate = api.uploadImage(TestImages.jpeg(160, 90), "c.jpg", "image/jpeg")
        val galleryPublic = api.uploadImage(TestImages.jpeg(160, 90), "d.jpg", "image/jpeg")
        val galleryPrivate = api.uploadImage(TestImages.jpeg(160, 90), "e.jpg", "image/jpeg")
        context.database.transactionBlocking { conn ->
            conn.createStatement().use {
                it.execute("INSERT INTO social_links(name,address,is_public,icon_image_id,position) VALUES ('a','x',0,'${iconPrivate.id}',0)")
                it.execute("INSERT INTO social_links(name,address,is_public,icon_image_id,position) VALUES ('b','x',1,'${iconPublic.id}',1)")
                it.execute("INSERT INTO projects(title,created_at,is_public,summary,thumbnail_image_id,position) VALUES ('p','2026-01-01',0,'',  '${thumbPrivate.id}',0)")
                it.execute("INSERT INTO projects(title,created_at,is_public,summary,thumbnail_image_id,position) VALUES ('q','2026-01-01',1,'',NULL,1)")
                it.execute("INSERT INTO project_images VALUES (1,'${galleryPrivate.id}',0)")
                it.execute("INSERT INTO project_images VALUES (2,'${galleryPublic.id}',0)")
            }
        }
        fun status(i: com.usuallycoding.portfolio.ImageDto) = kotlinx.coroutines.runBlocking { raw.get("/images/${i.fileName}").status }

        assertEquals(HttpStatusCode.NotFound, status(iconPrivate))
        assertEquals(HttpStatusCode.OK, status(iconPublic))
        assertEquals(HttpStatusCode.NotFound, status(thumbPrivate))
        assertEquals(HttpStatusCode.OK, status(galleryPublic))
        assertEquals(HttpStatusCode.NotFound, status(galleryPrivate))
    }

    @Test
    fun imageSharedByPrivateAndPublicContentIsServed() = withServer {
        api.login(TEST_PASSWORD)
        val image = api.uploadImage(TestImages.jpeg(160, 90), "s.jpg", "image/jpeg")
        context.database.transactionBlocking { conn ->
            conn.createStatement().use {
                it.execute("INSERT INTO projects(title,created_at,is_public,summary,thumbnail_image_id,position) VALUES ('p','2026-01-01',0,'','${image.id}',0)")
                it.execute("INSERT INTO projects(title,created_at,is_public,summary,thumbnail_image_id,position) VALUES ('q','2026-01-01',1,'','${image.id}',1)")
            }
        }
        assertEquals(HttpStatusCode.OK, raw.get("/images/${image.fileName}").status)
    }
}
