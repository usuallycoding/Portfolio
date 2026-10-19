package com.usuallycoding.portfolio.client

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.usuallycoding.portfolio.ui.PortfolioTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Records what is copied, and passes everything else to the real clipboard manager. */
@Suppress("DEPRECATION")
private class CapturingClipboard(private val real: ClipboardManager) : ClipboardManager by real {
    val copies = mutableListOf<String>()
    override fun setText(annotatedString: AnnotatedString) {
        copies += annotatedString.text
    }
}

@OptIn(ExperimentalTestApi::class)
class FullTextSheetTest {
    private fun lines(count: Int) = (1..count).joinToString("\n") { "Line number $it of the text" }

    private fun ComposeUiTest.screen(text: String, onClipboard: (CapturingClipboard) -> Unit = {}) {
        setContent {
            @Suppress("DEPRECATION")
            val real = LocalClipboardManager.current
            val clipboard = androidx.compose.runtime.remember { CapturingClipboard(real).also(onClipboard) }
            @Suppress("DEPRECATION")
            CompositionLocalProvider(LocalInspectionMode provides true, LocalClipboardManager provides clipboard) {
                PortfolioTheme {
                    Surface(Modifier.fillMaxSize()) {
                        Box { LongText(text, MaterialTheme.typography.bodyLarge, Modifier.width(400.dp)) }
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.settle() {
        waitForIdle()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
    }

    private fun ComposeUiTest.occurrences(text: String) = onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun ComposeUiTest.open(text: String) {
        onNodeWithText(text, substring = true, useUnmergedTree = true).performClick()
        settle()
    }

    // ---------- 1.2: the sheet ----------

    @Test
    fun tappingTruncatedTextOpensASheetWithTheFullText() = runDesktopComposeUiTest(800, 900) {
        val text = lines(30)
        screen(text)
        assertEquals(1, occurrences("Line number 30 of the text"), "only the page itself, so far")
        open(text)
        assertEquals(2, occurrences("Line number 30 of the text"), "the sheet now shows the whole text, including the part past row 10")
        onNodeWithContentDescription("Close").assertIsDisplayed()
        onNodeWithContentDescription("Copy text").assertIsDisplayed()
    }

    @Test
    fun theCloseIconClosesTheSheetAndLeavesThePageAsItWas() = runDesktopComposeUiTest(800, 900) {
        val text = lines(30)
        screen(text)
        open(text)
        onNodeWithContentDescription("Close").performClick()
        settle()
        assertEquals(1, occurrences("Line number 30 of the text"))
        assertEquals(0, onAllNodesWithContentDescription("Close").fetchSemanticsNodes().size, "no close icon is left behind")
        // The text on the page is still truncated and can be tapped again.
        open(text)
        assertEquals(2, occurrences("Line number 30 of the text"))
    }

    @Test
    fun tappingOutsideTheSheetClosesIt() = runDesktopComposeUiTest(800, 900) {
        val text = lines(30)
        screen(text)
        open(text)
        // The scrim covers the page above the sheet; tap near the top-left corner.
        onAllNodesWithContentDescription("Close").fetchSemanticsNodes() // the sheet is open
        onAllNodes(isRoot())[1].performTouchInput { click(Offset(20f, 20f)) }
        settle()
        assertEquals(1, occurrences("Line number 30 of the text"), "the sheet closed")
    }

    // The key tests use the sheet's content directly: the test harness sends key events only to the main window,
    // not to the separate window a modal sheet lives in.
    @Test
    fun escapeClosesTheSheetContentAndOtherKeysDoNot() = runDesktopComposeUiTest(800, 900) {
        var closed = 0
        setContent {
            PortfolioTheme { Surface(Modifier.fillMaxSize()) { FullTextSheetContent(lines(5), onClose = { closed++ }, copyToClipboard = {}) } }
        }
        waitForIdle()
        onRoot().performKeyInput { pressKey(Key.Enter) }
        onRoot().performKeyInput { pressKey(Key.A) }
        onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        assertEquals(0, closed, "other keys do nothing")
        onRoot().performKeyInput { pressKey(Key.Escape) } // no click first: it takes focus itself
        waitForIdle()
        assertEquals(1, closed)
    }

    @Test
    fun theCloseIconInTheContentClosesIt() = runDesktopComposeUiTest(800, 900) {
        var closed = 0
        setContent {
            PortfolioTheme { Surface(Modifier.fillMaxSize()) { FullTextSheetContent(lines(5), onClose = { closed++ }, copyToClipboard = {}) } }
        }
        onNodeWithContentDescription("Close").performClick()
        assertEquals(1, closed)
    }

    @Test
    fun aVeryLongTextScrollsInsideTheSheet() = runDesktopComposeUiTest(800, 700) {
        val text = lines(300)
        screen(text)
        open(text)
        val scrollers = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNodes()
        assertTrue(
            scrollers.any { it.config[SemanticsProperties.VerticalScrollAxisRange].maxValue() > 0f },
            "the long text sits in a scroll area that can scroll (found ${scrollers.size} scroll areas)",
        )
    }

    @Test
    fun aShortTextOpensTheSheetToo() = runDesktopComposeUiTest(800, 900) {
        screen("Just a short text.")
        assertEquals(0, onAllNodesWithContentDescription("Close").fetchSemanticsNodes().size, "closed to begin with")
        onNodeWithText("Just a short text.").performClick()
        settle()
        assertEquals(2, occurrences("Just a short text."), "the page and the sheet both show it")
        onNodeWithContentDescription("Close").assertIsDisplayed()
        onNodeWithContentDescription("Copy text").assertIsDisplayed()
    }

    @Test
    fun textsOfAnyLengthOpenTheSheetWithTheFullText() {
        for (rows in listOf(1, 5, 9, 10, 11, 25)) runDesktopComposeUiTest(800, 900) {
            val text = lines(rows)
            screen(text)
            open(text)
            assertEquals(2, occurrences("Line number $rows of the text"), "the sheet shows all $rows rows")
        }
    }

    @Test
    fun copyAndCloseWorkFromAShortTextSheetToo() = runDesktopComposeUiTest(800, 900) {
        var clip: CapturingClipboard? = null
        screen("A short bio.") { clip = it }
        open("A short bio.")
        onNodeWithContentDescription("Copy text").performClick()
        waitForIdle()
        assertEquals(listOf("A short bio."), clip!!.copies)
        onNodeWithContentDescription("Close").performClick()
        settle()
        assertEquals(1, occurrences("A short bio."), "the sheet closed")
    }

    // ---------- 1.3: copying ----------

    @Test
    fun copyPutsExactlyTheFullTextOnTheClipboard() = runDesktopComposeUiTest(800, 900) {
        val text = lines(14) + "\n\nÜnïcödé: café, 日本語, emoji 😀 and a \"quoted\" word.\n  indented line"
        var clip: CapturingClipboard? = null
        screen(text) { clip = it }
        open(text)
        onNodeWithContentDescription("Copy text").performClick()
        waitForIdle()
        assertEquals(listOf(text), clip!!.copies, "the whole text, with its line breaks and characters, copied once")
    }

    @Test
    fun aConfirmationAppearsAndGoesAwayByItself() = runDesktopComposeUiTest(800, 900) {
        val text = lines(30)
        screen(text)
        open(text)
        assertEquals(0, onAllNodesWithText("Copied").fetchSemanticsNodes().size, "nothing to confirm yet")
        onNodeWithContentDescription("Copy text").performClick()
        waitForIdle()
        onNodeWithText("Copied").assertIsDisplayed()
        onNodeWithContentDescription("Copied").assertIsDisplayed()

        mainClock.advanceTimeBy(CopiedConfirmationMillis + 500)
        waitForIdle()
        assertEquals(0, onAllNodesWithText("Copied").fetchSemanticsNodes().size, "the confirmation went away")
        onNodeWithContentDescription("Copy text").assertIsDisplayed()
    }

    @Test
    fun copyingTwiceWorksAndTheSheetStaysOpen() = runDesktopComposeUiTest(800, 900) {
        val text = lines(30)
        var clip: CapturingClipboard? = null
        screen(text) { clip = it }
        open(text)
        onNodeWithContentDescription("Copy text").performClick()
        waitForIdle()
        mainClock.advanceTimeBy(CopiedConfirmationMillis + 500)
        waitForIdle()
        onNodeWithContentDescription("Copy text").performClick()
        waitForIdle()
        assertEquals(listOf(text, text), clip!!.copies)
        assertEquals(2, occurrences("Line number 30 of the text"), "the sheet is still open")
    }

    @Test
    fun copyingAgainBeforeTheConfirmationEndsStillCopies() = runDesktopComposeUiTest(800, 900) {
        val text = lines(30)
        var clip: CapturingClipboard? = null
        screen(text) { clip = it }
        open(text)
        onNodeWithContentDescription("Copy text").performClick()
        waitForIdle()
        onNodeWithContentDescription("Copied").performClick() // the same button, now showing the check mark
        waitForIdle()
        assertEquals(2, clip!!.copies.size)
    }
}
