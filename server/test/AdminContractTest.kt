package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.ProfileInput
import com.usuallycoding.portfolio.ProjectInput
import com.usuallycoding.portfolio.SocialLinkInput
import com.usuallycoding.portfolio.admin.CropAspect
import com.usuallycoding.portfolio.admin.CropState
import com.usuallycoding.portfolio.admin.ImageProcessing
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The admin app crops and downscales images before upload and the server validates them. These tests run the
 * admin's real image code against the real server routes, so a change to either side that breaks the
 * contract (ratio tolerance, formats, size limits) fails here.
 */
class AdminContractTest {
    private fun photo(width: Int, height: Int, png: Boolean = false, alpha: Boolean = false): ByteArray {
        val image = BufferedImage(width, height, if (alpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color(30, 90, 200); g.fillRect(0, 0, width, height)
        g.color = Color.WHITE; g.fillOval(width / 4, height / 4, width / 2, height / 2)
        g.dispose()
        return ByteArrayOutputStream().also { ImageIO.write(image, if (png) "png" else "jpg", it) }.toByteArray()
    }

    private data class Case(val width: Int, val height: Int, val zoom: Float, val panX: Float, val panY: Float)

    // Awkward sizes and positions, the kind a real crop produces.
    private val cases = listOf(
        Case(4032, 3024, 1f, 0f, 0f),
        Case(3024, 4032, 1.7f, 120.3f, -88.8f),
        Case(1999, 1333, 3.3f, 40f, 40f),
        Case(8000, 1000, 1f, 0f, 0f),
        Case(900, 900, 7.9f, -50f, 50f),
        Case(640, 480, 1.01f, 3f, 3f),
        Case(2500, 2500, 2f, 0f, 0f),
    )

    private suspend fun TestEnv.uploadCropped(case: Case, aspect: CropAspect, png: Boolean = false, alpha: Boolean = false) =
        ImageProcessing.decode(photo(case.width, case.height, png, alpha))!!.let { source ->
            val state = CropState(source.width, source.height, aspect).apply { zoomTo(case.zoom); panBy(case.panX, case.panY) }
            val prepared = ImageProcessing.prepare(source, state.rect(), aspect)
            api.uploadImage(prepared.bytes, "photo.${prepared.extension}", prepared.contentType)
        }

    @Test
    fun croppedProfilePicturesCoversAndThumbnailsAreAcceptedByTheServer() = withServer {
        api.login(TEST_PASSWORD)
        for (case in cases) {
            val picture = uploadCropped(case, CropAspect.Square)
            val cover = uploadCropped(case, CropAspect.Cover)
            val thumb = uploadCropped(case, CropAspect.Thumbnail)
            api.updateProfile(ProfileInput("n", "d", picture.id, cover.id))
            api.createProject(ProjectInput("p", "2026-01-01", true, "", thumb.id))
            assertTrue(maxOf(picture.width, picture.height) <= 2000 && maxOf(cover.width, cover.height) <= 2000, "$case")
        }
    }

    @Test
    fun croppedIconsKeepTransparencyAndAreAccepted() = withServer {
        api.login(TEST_PASSWORD)
        val icon = uploadCropped(Case(512, 512, 1.3f, 5f, 5f), CropAspect.Square, png = true, alpha = true)
        val link = api.createLink(SocialLinkInput("GitHub", "https://github.com", true, icon.id))
        assertEquals(icon, link.icon)
        assertTrue(icon.fileName.endsWith(".png"))
    }

    @Test
    fun downscaledGalleryPhotosAreAcceptedWhateverTheirShape() = withServer {
        api.login(TEST_PASSWORD)
        val images = listOf(Case(6000, 4000, 1f, 0f, 0f), Case(3000, 9000, 1f, 0f, 0f), Case(1200, 300, 1f, 0f, 0f), Case(50, 50, 1f, 0f, 0f))
            .map { case ->
                val source = ImageProcessing.decode(photo(case.width, case.height))!!
                val prepared = ImageProcessing.prepare(source)
                assertTrue(maxOf(prepared.width, prepared.height) <= 2000)
                api.uploadImage(prepared.bytes, "g.jpg", prepared.contentType)
            }
        val project = api.createProject(ProjectInput("Gallery", "2026-01-01", true, "", null, images.map { it.id }))
        assertEquals(images.map { it.id }, project.images.map { it.id })
    }

    @Test
    fun anExactlyRatioedImageSurvivesTheServersThumbnailStep() = withServer {
        api.login(TEST_PASSWORD)
        val cover = uploadCropped(cases.first(), CropAspect.Cover)
        api.updateProfile(ProfileInput("n", "d", coverImageId = cover.id))
        val thumb = TestImages.read(raw.get("/images/${cover.fileName}?size=thumb") { bearerAuth(api.token!!) }.bodyAsBytes())
        assertEquals(400, thumb.width)
        assertTrue(kotlin.math.abs(thumb.width.toDouble() / thumb.height - 3.0) < 0.05)
    }
}
