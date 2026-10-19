package com.usuallycoding.portfolio.client

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.toAwtImage
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import com.usuallycoding.portfolio.ui.ThemePreference
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LinkRowsTest {
    private val noImages: ImageUrl = { _, _ -> null }
    private val longAddress = "https://example.com/" + "a-very-long-path-segment/".repeat(12)

    private val links = listOf(
        SocialLinkDto(1, "GitHub", "https://github.com/ada", true, ImageDto("i1", "i1.png", 64, 64)),
        SocialLinkDto(2, "Email", "mailto:ada@example.com", true),
        SocialLinkDto(3, "Long one", longAddress, true),
    )

    private fun data(links: List<SocialLinkDto> = this.links) =
        HomeData(ProfileDto("Ada", "Analyst"), links, emptyList())


    @Test
    fun eachLinkShowsItsNameAndItsAddress() = runDesktopComposeUiTest(480, 1000) {
        setContent { PortfolioTheme { Surface(Modifier.fillMaxSize()) { HomeContent(data(), noImages, {}, {}) } } }
        onNodeWithText("GitHub").assertIsDisplayed()
        onNodeWithText("https://github.com/ada").assertIsDisplayed()
        onNodeWithText("Email").assertIsDisplayed()
        onNodeWithText("mailto:ada@example.com").assertIsDisplayed()
    }

    @Test
    fun aLongAddressIsEllipsizedAndTheNameStaysVisible() = runDesktopComposeUiTest(480, 1000) {
        setContent { PortfolioTheme { Surface(Modifier.fillMaxSize()) { HomeContent(data(), noImages, {}, {}) } } }
        onNodeWithText("Long one").assertIsDisplayed()
        val node = onNodeWithText(longAddress).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        assertEquals(1, results.single().lineCount, "a long address must stay on one line")
        assertTrue(results.single().hasVisualOverflow, "and be cut with an ellipsis")
        assertTrue(node.size.width <= 480, "it must not widen the row beyond the screen")
    }

    @Test
    fun tappingARowOpensThatLinksAddress() = runDesktopComposeUiTest(480, 1000) {
        var opened: SocialLinkDto? = null
        setContent { PortfolioTheme { Surface(Modifier.fillMaxSize()) { HomeContent(data(), noImages, { opened = it }, {}) } } }
        onNodeWithText("mailto:ada@example.com").performClick()
        assertEquals("mailto:ada@example.com", opened?.address)
        onNodeWithText("GitHub").performClick()
        assertEquals("https://github.com/ada", opened?.address)
    }

    @Test
    fun iconsAreLargeWithOrWithoutAnImage() = runDesktopComposeUiTest(480, 1000) {
        setContent { PortfolioTheme { Surface(Modifier.fillMaxSize()) { HomeContent(data(), noImages, {}, {}) } } }
        for (name in listOf("GitHub", "Email")) {
            val size = onNodeWithContentDescription("$name icon", useUnmergedTree = true).fetchSemanticsNode().size
            val minPx = 40 * density.density
            assertTrue(size.width >= minPx && size.height >= minPx, "$name icon is ${size.width}x${size.height}px, expected at least $minPx")
            assertEquals(size.width, size.height, "icons are square")
        }
    }

    @Test
    fun noLinksMeansNoLinkRows() = runDesktopComposeUiTest(480, 1000) {
        setContent { PortfolioTheme { Surface(Modifier.fillMaxSize()) { HomeContent(data(emptyList()), noImages, {}, {}) } } }
        onNodeWithText("Ada").assertIsDisplayed()
        assertNull(runCatching { onNodeWithText("GitHub").assertIsDisplayed() }.getOrNull())
    }

    @Test
    fun screenshotsInBothThemes() {
        for (preference in listOf(ThemePreference.Light, ThemePreference.Dark)) runDesktopComposeUiTest(480, 900) {
            setContent { PortfolioTheme(preference) { Surface(Modifier.fillMaxSize()) { HomeContent(data(), noImages, {}, {}) } } }
            onNodeWithText("GitHub").assertIsDisplayed()
            val dir = File("build/ui-screenshots").also { it.mkdirs() }
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "links-${preference.name.lowercase()}.png"))
        }
    }
}

/** Links and projects share one list, and their database ids start at 1 independently, so ids can collide. */
@OptIn(ExperimentalTestApi::class)
class ListKeysTest {
    private val noImages: ImageUrl = { _, _ -> null }

    @Test
    fun aLinkAndAProjectWithTheSameIdAreBothShown() = runDesktopComposeUiTest(480, 1400) {
        val data = HomeData(
            ProfileDto("Ada", "Analyst"),
            links = listOf(
                SocialLinkDto(1, "GitHub", "https://github.com/ada", true),
                SocialLinkDto(2, "Email", "mailto:ada@example.com", true),
            ),
            projects = listOf(
                com.usuallycoding.portfolio.ProjectSummaryDto(1, "First project", "2026-01-02", true, "s"),
                com.usuallycoding.portfolio.ProjectSummaryDto(2, "Second project", "2026-01-03", true, "s"),
            ),
        )
        setContent { PortfolioTheme { Surface(Modifier.fillMaxSize()) { HomeContent(data, noImages, {}, {}) } } }
        onNodeWithText("GitHub").assertIsDisplayed()
        onNodeWithText("Email").assertIsDisplayed()
        onNodeWithText("First project").assertIsDisplayed()
        onNodeWithText("Second project").assertIsDisplayed()
    }
}
