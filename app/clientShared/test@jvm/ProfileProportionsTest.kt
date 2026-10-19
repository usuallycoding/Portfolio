package com.usuallycoding.portfolio.client

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import com.usuallycoding.portfolio.ui.ThemePreference
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** On wide screens the cover is not enormous and text is not stretched across the whole window. */
@OptIn(ExperimentalTestApi::class)
class ProfileProportionsTest {
    private val noImages: ImageUrl = { _, _ -> null }
    private val description = "A long description of the owner, written the way people do, which goes on for a good while and would run across a very wide screen if nothing limited it. ".repeat(3)

    private val data = HomeData(
        ProfileDto("Ada Lovelace", description),
        listOf(
            SocialLinkDto(1, "GitHub", "https://github.com/ada", true),
            SocialLinkDto(2, "Email", "mailto:ada@example.com", true),
        ),
        (1L..10L).map { ProjectSummaryDto(it, "Project $it", "2026-01-02", true, "summary") },
    )

    private fun ComposeUiTest.show(onOpenLink: (SocialLinkDto) -> Unit = {}, preference: ThemePreference = ThemePreference.Light) {
        setContent {
            PortfolioTheme(preference) { Surface(Modifier.fillMaxSize()) { HomeContent(data, noImages, onOpenLink, {}) } }
        }
    }

    private fun ComposeUiTest.cover() = onNodeWithContentDescription("Cover image").fetchSemanticsNode().boundsInRoot

    @Test
    fun theCoverKeepsItsFullWidthButIsNeverTallerThan240() {
        for (width in listOf(1600, 1200, 1000, 721)) runDesktopComposeUiTest(width, 1000) {
            show()
            val c = cover()
            assertEquals(width.toFloat(), c.width, 0.5f, "cover width in a ${width}px window")
            assertEquals(240f, c.height, 0.5f, "cover height in a ${width}px window")
        }
    }

    @Test
    fun belowThatTheCoverIsStillThreeToOne() {
        for (width in listOf(720, 600, 411, 360)) runDesktopComposeUiTest(width, 1000) {
            show()
            val c = cover()
            assertEquals(width.toFloat(), c.width, 0.5f)
            assertEquals(width / 3f, c.height, 0.6f, "a ${width}px window keeps the 3:1 cover")
        }
    }

    @Test
    fun onAPhoneTheTextAndLinksUseTheFullWidthAsBefore() = runDesktopComposeUiTest(411, 1400) {
        show()
        val text = onNodeWithText(description.trim(), substring = true).fetchSemanticsNode().boundsInRoot
        assertEquals(411f - 32f, text.width, 1f, "description fills the width minus the gutters")
        assertEquals(411f, onNodeWithText("GitHub").fetchSemanticsNode().boundsInRoot.width, 0.5f)
    }

    @Test
    fun theAvatarStillOverlapsTheBottomEdgeOfTheCover() {
        for (width in listOf(411, 1600)) runDesktopComposeUiTest(width, 1200) {
            show()
            val c = cover()
            val avatar = onNodeWithContentDescription("Profile picture").fetchSemanticsNode().boundsInRoot
            assertTrue(avatar.top < c.bottom && avatar.bottom > c.bottom, "the avatar straddles the cover's bottom edge at ${width}px")
            assertEquals(c.bottom, avatar.center.y, 1f, "centred on the edge")
        }
    }

    @Test
    fun screenshotsInBothThemes() {
        for ((width, label) in listOf(1600 to "wide", 411 to "phone")) for (preference in ThemePreference.entries.filter { it != ThemePreference.System }) {
            runDesktopComposeUiTest(width, 1000) {
                show(preference = preference)
                val dir = File("build/ui-screenshots").also { it.mkdirs() }
                ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "profile-$label-${preference.name.lowercase()}.png"))
            }
        }
    }
}
