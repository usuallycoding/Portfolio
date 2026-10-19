package com.usuallycoding.portfolio.client

import com.usuallycoding.portfolio.client.presentation.*
import com.usuallycoding.portfolio.client.domain.HomeData

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.ui.PortfolioTheme
import com.usuallycoding.portfolio.ui.ThemePreference
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** Writes screenshots for a visual check; the assertions only make sure they were produced. */
@OptIn(ExperimentalTestApi::class)
class CollapsibleScreenshotsTest {
    private val noImages: ImageUrl = { _, _ -> null }
    private val description = (1..30).joinToString("\n") { "Line $it of a long personal description that runs on and on." }
    private val dir = File("build/ui-screenshots").also { it.mkdirs() }

    private val data = HomeData(
        ProfileDto("Ada Lovelace", description),
        (1..14).map { SocialLinkDto(it.toLong(), "Link $it", "https://example.com/link-$it", true) },
        (1L..30L).map { ProjectSummaryDto(it, "Project $it", "2026-01-02", true, "Summary of project $it") },
    )

    private fun ComposeUiTest.show(preference: ThemePreference, sections: ExpandedSections) {
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalInspectionMode provides true) {
                PortfolioTheme(preference) { Surface(Modifier.fillMaxSize()) { HomeContent(data, noImages, {}, {}, sections = sections) } }
            }
        }
    }

    private fun ComposeUiTest.save(name: String, rootIndex: Int = 0) {
        val image = onAllNodes(isRoot())[rootIndex].captureToImage().toAwtImage()
        val file = File(dir, "$name.png")
        ImageIO.write(image, "png", file)
        assertTrue(file.length() > 0)
    }

    @Test
    fun collapsedExpandedAndWithTheSheetOpen() {
        for ((width, label) in listOf(411 to "phone", 1600 to "wide")) for (p in listOf(ThemePreference.Light, ThemePreference.Dark)) {
            val tag = "${label}-${p.name.lowercase()}"
            runDesktopComposeUiTest(width, 1900) {
                show(p, ExpandedSections())
                save("collapsible-collapsed-$tag")
            }
            runDesktopComposeUiTest(width, 3600) {
                show(p, ExpandedSections(socials = true, projects = true))
                save("collapsible-expanded-$tag")
            }
            runDesktopComposeUiTest(width, 1000) {
                show(p, ExpandedSections())
                onNodeWithText(description, substring = true, useUnmergedTree = true).performClick()
                waitForIdle()
                mainClock.advanceTimeBy(1_000)
                waitForIdle()
                save("collapsible-sheet-$tag", rootIndex = 1)
            }
        }
    }
}
