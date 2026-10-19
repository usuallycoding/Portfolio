package com.usuallycoding.portfolio.client

import com.usuallycoding.portfolio.client.presentation.*

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ReaderTest {
    private fun image(i: Int, w: Int, h: Int) = ImageDto("img$i", "img$i.jpg", w, h)

    private val project = ProjectDto(
        7, "Chapter 1", "2026-01-02", true, "First chapter.", null,
        images = listOf(image(0, 800, 600), image(1, 800, 1600), image(2, 1000, 500), image(3, 600, 600)),
    )

    private class Requests {
        val thumbnails = mutableSetOf<String>()
        val originals = mutableSetOf<String>()
        val url: ImageUrl = { image, thumb ->
            image?.let { (if (thumb) thumbnails else originals) += it.id; "http://x/${it.id}" }
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.content(
        project: ProjectDto = this@ReaderTest.project,
        requests: Requests = Requests(),
        initial: GalleryMode = GalleryMode.Grid,
        onModeSeen: (GalleryMode) -> Unit = {},
    ) {
        setContent {
            var mode by mutableStateOf(initial).let { androidx.compose.runtime.remember { it } }
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalInspectionMode provides true) {
                PortfolioTheme {
                    Surface(Modifier.fillMaxSize()) {
                        ProjectContent(project, requests.url, {}, {}, mode = mode, onModeChange = { mode = it; onModeSeen(it) })
                    }
                }
            }
        }
    }

    private fun SemanticsNodeInteractionsProvider.bounds(index: Int) =
        onNodeWithContentDescription("Chapter 1 image ${index + 1}").fetchSemanticsNode().boundsInRoot

    @Test
    fun theGridIsShownFirstAndTheSwitchTogglesBetweenViews() = runDesktopComposeUiTest(480, 1800) {
        content()
        onNodeWithTag("grid").assertIsDisplayed()
        assertTrue(runCatching { onNodeWithTag("reader").assertIsDisplayed() }.isFailure, "reader must not be shown first")

        onNodeWithText("Reader").performClick()
        onNodeWithTag("reader").assertIsDisplayed()
        assertTrue(runCatching { onNodeWithTag("grid").assertIsDisplayed() }.isFailure)

        onNodeWithText("Grid").performClick()
        onNodeWithTag("grid").assertIsDisplayed()
    }

    @Test
    fun theReaderShowsEveryImageInOrderAsASeamlessStrip() = runDesktopComposeUiTest(480, 2400) {
        content(initial = GalleryMode.Reader)
        val b = (0..3).map { bounds(it) }
        for (i in 0..2) {
            assertTrue(b[i].top < b[i + 1].top, "image ${i + 1} must come before image ${i + 2}")
            assertEquals(b[i].bottom, b[i + 1].top, 0.5f, "no gap between image ${i + 1} and ${i + 2}")
        }
        for (r in b) {
            assertEquals(0f, r.left, 0.5f, "images start at the left edge")
            assertEquals(480f, r.right, 0.5f, "and span the full width")
        }
    }

    @Test
    fun eachImageHasItsProportionalHeightBeforeItLoads() = runDesktopComposeUiTest(480, 2400) {
        content(initial = GalleryMode.Reader) // image urls never load in tests, so these are placeholders
        val expected = listOf(480 * 600 / 800f, 480 * 1600 / 800f, 480 * 500 / 1000f, 480f)
        expected.forEachIndexed { i, height -> assertEquals(height, bounds(i).height, 0.5f, "image ${i + 1}") }
    }

    @Test
    fun theReaderRequestsOriginalsAndTheGridRequestsThumbnails() {
        val grid = Requests()
        runDesktopComposeUiTest(480, 2400) { content(requests = grid) }
        assertEquals(setOf("img0", "img1", "img2", "img3"), grid.thumbnails)
        assertTrue(grid.originals.isEmpty(), "the grid must not download originals")

        val reader = Requests()
        runDesktopComposeUiTest(480, 2400) { content(requests = reader, initial = GalleryMode.Reader) }
        assertEquals(setOf("img0", "img1", "img2", "img3"), reader.originals)
        assertTrue(reader.thumbnails.isEmpty(), "the reader must use the originals")
    }

    @Test
    fun aWideWindowStillSpansTheFullWidth() = runDesktopComposeUiTest(1400, 2400) {
        content(initial = GalleryMode.Reader)
        val r = bounds(0)
        assertEquals(1400f, r.width, 0.5f)
        assertEquals(0f, r.left, 0.5f)
    }

    @Test
    fun anEmptyProjectSaysSoInBothViews() {
        for (mode in GalleryMode.entries) runDesktopComposeUiTest(480, 800) {
            content(project = project.copy(images = emptyList()), initial = mode)
            onNodeWithText("No images yet.").assertIsDisplayed()
        }
    }

    @Test
    fun aLongChapterOnlyRequestsTheImagesNearTheScreen() = runDesktopComposeUiTest(480, 900) {
        val requests = Requests()
        val long = project.copy(images = List(60) { image(it, 800, 1200) })
        content(project = long, requests = requests, initial = GalleryMode.Reader)
        waitForIdle()
        assertTrue(requests.originals.size in 1..12, "requested ${requests.originals.size} of 60 images for a 900px screen")
        assertTrue("img0" in requests.originals)
        assertTrue("img59" !in requests.originals)
    }

    @Test
    fun theReaderIsScrollableToTheEnd() = runDesktopComposeUiTest(480, 900) {
        val long = project.copy(images = List(10) { image(it, 800, 1200) })
        content(project = long, initial = GalleryMode.Reader)
        onNodeWithTag("reader").performScrollToIndexCompat(9)
        onNodeWithContentDescription("Chapter 1 image 10").assertIsDisplayed()
    }

    @Test
    fun screenshotsOfTheReader() = runDesktopComposeUiTest(480, 900) {
        content(initial = GalleryMode.Reader)
        waitForIdle()
        val dir = File("build/ui-screenshots").also { it.mkdirs() }
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "reader.png"))
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.performScrollToIndexCompat(index: Int) =
    performScrollToIndex(index)
