package com.usuallycoding.portfolio.client

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/** On big screens (web, tablets) nothing is held to a narrow column. */
@OptIn(ExperimentalTestApi::class)
class FullWidthTest {
    private val noImages: ImageUrl = { _, _ -> null }
    private val project = ProjectDto(
        7, "Chapter 1", "2026-01-02", true, "s", null,
        images = List(4) { ImageDto("i$it", "i$it.jpg", 800, 600) },
    )

    @Test
    fun theProfileCoverSpansTheWholeWindow() = runDesktopComposeUiTest(1600, 900) {
        setContent {
            PortfolioTheme { Surface(Modifier.fillMaxSize()) { HomeContent(HomeData(ProfileDto("Ada", "d"), emptyList(), emptyList()), noImages, {}, {}) } }
        }
        val cover = onNodeWithContentDescription("Cover image").fetchSemanticsNode().boundsInRoot
        assertEquals(0f, cover.left, 0.5f)
        assertEquals(1600f, cover.width, 0.5f)
    }

    @Test
    fun theProjectGridSpansTheWholeWindow() = runDesktopComposeUiTest(1600, 900) {
        setContent { PortfolioTheme { Surface(Modifier.fillMaxSize()) { ProjectContent(project, noImages, {}, {}) } } }
        val grid = onNodeWithTag("grid").fetchSemanticsNode().boundsInRoot
        assertEquals(0f, grid.left, 0.5f)
        assertEquals(1600f, grid.width, 0.5f)
    }

    @Test
    fun theReaderSpansTheWholeWindowAndPhonesAreUnchanged() {
        for (width in listOf(1600, 1000, 400)) runDesktopComposeUiTest(width, 1200) {
            setContent {
                PortfolioTheme { Surface(Modifier.fillMaxSize()) { ProjectContent(project, noImages, {}, {}, mode = GalleryMode.Reader) } }
            }
            val reader = onNodeWithTag("reader").fetchSemanticsNode().boundsInRoot
            assertEquals(width.toFloat(), reader.width, 0.5f, "reader width in a ${width}px window")
            val firstImage = onNodeWithContentDescription("Chapter 1 image 1").fetchSemanticsNode().boundsInRoot
            assertEquals(width.toFloat(), firstImage.width, 0.5f)
        }
    }
}
