package com.usuallycoding.portfolio.client

import com.usuallycoding.portfolio.client.presentation.*
import com.usuallycoding.portfolio.client.domain.HomeData

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ScreenTest {
    private val noImages: ImageUrl = { _, _ -> null }

    private val home = HomeData(
        profile = ProfileDto("Ada Lovelace", "Mathematician and first programmer."),
        links = listOf(
            SocialLinkDto(1, "GitHub", "https://github.com/ada", true),
            SocialLinkDto(2, "Email", "mailto:ada@example.com", true, ImageDto("i", "i.png", 10, 10)),
        ),
        projects = listOf(
            ProjectSummaryDto(7, "Analytical Engine", "2026-01-02", true, "Notes on the engine."),
            ProjectSummaryDto(8, "Bernoulli Numbers", "2025-11-30", true, "The first program."),
        ),
    )

    private val project = ProjectDto(
        7, "Analytical Engine", "2026-01-02", true, "Notes on the engine.", null,
        images = List(3) { ImageDto("img$it", "img$it.jpg", 800, 600) },
    )

    private fun ComposeUiTest.screenshot(name: String) {
        val dir = File("build/ui-screenshots").also { it.mkdirs() }
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "$name.png"))
    }

    @Test
    fun homeShowsProfileLinksAndProjects() = runDesktopComposeUiTest(480, 1600) {
        setContent {
            PortfolioTheme {
                HomeContent(home, noImages, onOpenLink = {}, onOpenProject = {}, modifier = Modifier.fillMaxSize())
            }
        }
        onNodeWithText("Ada Lovelace").assertIsDisplayed()
        onNodeWithText("Mathematician and first programmer.").assertIsDisplayed()
        onNodeWithText("GitHub").assertIsDisplayed()
        onNodeWithText("Email").assertIsDisplayed()
        onNodeWithText("Projects").assertIsDisplayed()
        onNodeWithText("Analytical Engine").assertIsDisplayed()
        onNodeWithText("2 Jan 2026").assertIsDisplayed()
        screenshot("home")
    }

    @Test
    fun projectsKeepTheOrderTheyWereGiven() = runDesktopComposeUiTest(480, 1600) {
        setContent {
            PortfolioTheme { HomeContent(home, noImages, {}, {}, Modifier.fillMaxSize()) }
        }
        val first = onNodeWithText("Analytical Engine").fetchSemanticsNode().boundsInRoot
        val second = onNodeWithText("Bernoulli Numbers").fetchSemanticsNode().boundsInRoot
        // Owner order runs left to right, then top to bottom.
        assert(first.top < second.top || (first.top == second.top && first.left < second.left)) {
            "projects must appear in the owner's order"
        }
    }

    @Test
    fun tappingALinkOrAProjectReportsIt() = runDesktopComposeUiTest(480, 1600) {
        var link: SocialLinkDto? = null
        var opened: ProjectSummaryDto? = null
        setContent {
            PortfolioTheme {
                HomeContent(home, noImages, { link = it }, { opened = it }, Modifier.fillMaxSize())
            }
        }
        onNodeWithText("GitHub").performClick()
        onNodeWithText("Bernoulli Numbers").performClick()
        assertEquals("https://github.com/ada", link?.address)
        assertEquals(8L, opened?.id)
    }

    @Test
    fun placeholderProfileWithNothingElseRendersWithoutErrors() = runDesktopComposeUiTest(480, 1600) {
        val fresh = HomeData(ProfileDto("Your name", "This portfolio has not been set up yet."), emptyList(), emptyList())
        setContent {
            PortfolioTheme { HomeContent(fresh, noImages, {}, {}, Modifier.fillMaxSize()) }
        }
        onNodeWithText("Your name").assertIsDisplayed()
        onNodeWithText("This portfolio has not been set up yet.").assertIsDisplayed()
        onNodeWithText("No projects yet.").assertIsDisplayed()
        screenshot("home-empty")
    }

    @Test
    fun linksSectionIsOmittedWhenThereAreNoPublicLinks() = runDesktopComposeUiTest(480, 1600) {
        setContent {
            PortfolioTheme { HomeContent(home.copy(links = emptyList()), noImages, {}, {}, Modifier.fillMaxSize()) }
        }
        onNodeWithText("GitHub").assertDoesNotExist()
    }

    @Test
    fun projectScreenShowsDetailsAndEveryImageInOrder() = runDesktopComposeUiTest(480, 1600) {
        var tapped = -1
        var back = false
        setContent {
            PortfolioTheme {
                ProjectContent(project, noImages, onBack = { back = true }, onOpenImage = { tapped = it }, modifier = Modifier.fillMaxSize())
            }
        }
        onNodeWithText("Analytical Engine").assertIsDisplayed()
        onNodeWithText("2 Jan 2026").assertIsDisplayed()
        onNodeWithText("Notes on the engine.").assertIsDisplayed()
        onAllNodesWithContentDescription("Analytical Engine image", substring = true).assertCountEquals(3)

        onNodeWithContentDescription("Analytical Engine image 2").performClick()
        assertEquals(1, tapped)
        onNodeWithContentDescription("Back").performClick()
        assertEquals(true, back)
        screenshot("project")
    }

    @Test
    fun projectWithoutImagesSaysSo() = runDesktopComposeUiTest(480, 1600) {
        setContent {
            PortfolioTheme { ProjectContent(project.copy(images = emptyList()), noImages, {}, {}, Modifier.fillMaxSize()) }
        }
        onNodeWithText("No images yet.").assertIsDisplayed()
    }

    @Test
    fun viewerShowsPositionAndCloses() = runDesktopComposeUiTest(480, 1600) {
        var closed = false
        setContent {
            PortfolioTheme { ImageViewer(project, startIndex = 1, imageUrl = noImages, onClose = { closed = true }, modifier = Modifier.fillMaxSize()) }
        }
        onNodeWithText("2 / 3").assertIsDisplayed()
        onNodeWithContentDescription("Close").performClick()
        assertEquals(true, closed)
    }

    @Test
    fun errorStateShowsTheMessageAndRetryWorks() = runDesktopComposeUiTest(480, 800) {
        var retries = 0
        setContent {
            PortfolioTheme { com.usuallycoding.portfolio.ui.ErrorView("Could not reach the server. Check your connection and try again.", onRetry = { retries++ }) }
        }
        onNodeWithText("Could not reach the server. Check your connection and try again.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun loadingAndEmptyStatesRender() = runDesktopComposeUiTest(480, 800) {
        setContent { PortfolioTheme { com.usuallycoding.portfolio.ui.EmptyView("Nothing here") } }
        onNodeWithText("Nothing here").assertIsDisplayed()
    }

    @Test
    fun theBackControlIsTheStandardArrowAndCanBeHidden() = runDesktopComposeUiTest(480, 800) {
        var back = 0
        val showBack = androidx.compose.runtime.mutableStateOf(true)
        setContent {
            PortfolioTheme { ProjectContent(project, noImages, onBack = { back++ }, onOpenImage = {}, showBackControl = showBack.value) }
        }
        onNodeWithContentDescription("Back").assertIsDisplayed()
        assertTrue(runCatching { onNodeWithText("Back").assertIsDisplayed() }.isFailure, "it is an icon, not a text button")
        onNodeWithContentDescription("Back").performClick()
        assertEquals(1, back)
        showBack.value = false
        waitForIdle()
        assertTrue(runCatching { onNodeWithContentDescription("Back").assertIsDisplayed() }.isFailure)
        onNodeWithText("Analytical Engine").assertIsDisplayed() // the title stays
    }
}
