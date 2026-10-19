package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.server.data.*
import com.usuallycoding.portfolio.server.service.*

import com.usuallycoding.portfolio.server.domain.*
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageRatioTest {
    private suspend fun TestEnv.attach(width: Int, height: Int, ratio: AspectRatio?) =
        api.uploadImage(TestImages.jpeg(width, height), "a.jpg", "image/jpeg").let { image ->
            database.transaction { conn ->
                SqliteImageRepository(conn).requireForAttach(image.id, ratio, config.ratioTolerance, "field")
            }
        }

    @Test
    fun exactRatiosAreAccepted() = withServer {
        api.login(TEST_PASSWORD)
        assertNotNull(attach(500, 500, AspectRatio.SQUARE))
        assertNotNull(attach(900, 300, AspectRatio.COVER))
        assertNotNull(attach(1600, 900, AspectRatio.THUMBNAIL))
    }

    @Test
    fun smallRoundingErrorsAreAccepted() = withServer {
        api.login(TEST_PASSWORD)
        assertNotNull(attach(1001, 1000, AspectRatio.SQUARE))
        assertNotNull(attach(1999, 666, AspectRatio.COVER)) // 3:1 cropped with integer rounding
        assertNotNull(attach(1599, 900, AspectRatio.THUMBNAIL))
    }

    @Test
    fun wrongRatiosAreRejectedWithA400() = withServer {
        api.login(TEST_PASSWORD)
        for ((w, h, ratio) in listOf(
            Triple(800, 600, AspectRatio.SQUARE),
            Triple(900, 400, AspectRatio.COVER),
            Triple(1000, 1000, AspectRatio.THUMBNAIL),
            Triple(300, 900, AspectRatio.SQUARE),
        )) {
            val e = assertFailsWith<DomainError.Invalid> { attach(w, h, ratio) }
            assertTrue(ratio.label in e.message, e.message)
        }
    }

    @Test
    fun noRatioMeansAnyShapeIsFine() = withServer {
        api.login(TEST_PASSWORD)
        assertNotNull(attach(123, 987, null))
    }

    @Test
    fun nullIdClearsTheFieldAndUnknownIdIsRejected() = withServer {
        database.transaction { conn ->
            assertNull(SqliteImageRepository(conn).requireForAttach(null, AspectRatio.SQUARE, 0.02, "field"))
            assertFailsWith<DomainError.Invalid> { SqliteImageRepository(conn).requireForAttach("does-not-exist", null, 0.02, "field") }
        }
    }
}
