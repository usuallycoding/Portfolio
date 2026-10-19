package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.admin.data.*
import com.usuallycoding.portfolio.admin.presentation.*

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.graphics.toAwtImage
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.SocialLinkInput
import com.usuallycoding.portfolio.ui.Load
import com.usuallycoding.portfolio.ui.PortfolioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MovedTest {
    @Test
    fun movesElementsAndIgnoresImpossibleMoves() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), list.moved(0, 1))
        assertEquals(listOf("a", "c", "b"), list.moved(1, 1))
        assertEquals(listOf("c", "a", "b"), list.moved(2, -2))
        assertEquals(list, list.moved(0, -1))
        assertEquals(list, list.moved(2, 1))
        assertEquals(list, list.moved(1, 0))
        assertEquals(list, list.moved(9, 1))
    }
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
class LinksTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun seeded(): Pair<FakeServer, AdminSession> {
        val server = FakeServer()
        server.links += listOf(
            SocialLinkDto(1, "GitHub", "https://github.com/ada", true),
            SocialLinkDto(2, "Email", "mailto:ada@example.com", false),
            SocialLinkDto(3, "Blog", "https://ada.example", true),
        )
        return server to server.session().also { runBlocking { it.login("secret") } }
    }

    private fun <T> waitFor(condition: () -> T?): T = runBlocking {
        kotlinx.coroutines.withTimeout(5_000) {
            var r = condition(); while (r == null) { kotlinx.coroutines.delay(10); r = condition() }; r
        }
    }

    private fun LinksViewModel.names() = (assertIs<Load.Loaded<List<SocialLinkDto>>>(loadState)).value.map { it.name }

    @Test
    fun loadsLinksInTheServersOrderIncludingPrivateOnes() {
        val (_, session) = seeded()
        val vm = LinksViewModel(session)
        waitFor { vm.loadState.takeIf { it is Load.Loaded } }
        assertEquals(listOf("GitHub", "Email", "Blog"), vm.names())
    }

    @Test
    fun moveSavesTheNewOrderImmediately() {
        val (server, session) = seeded()
        val vm = LinksViewModel(session)
        waitFor { vm.loadState.takeIf { it is Load.Loaded } }

        vm.move(2, -1)
        waitFor { vm.names().takeIf { it == listOf("GitHub", "Blog", "Email") } }

        assertEquals(listOf("GitHub", "Blog", "Email"), server.links.map { it.name })
    }

    @Test
    fun movingPastTheEndsDoesNothing() {
        val (server, session) = seeded()
        val vm = LinksViewModel(session)
        waitFor { vm.loadState.takeIf { it is Load.Loaded } }
        val before = server.requests.size
        vm.move(0, -1); vm.move(2, 1)
        assertEquals(before, server.requests.size)
    }

    @Test
    fun createUpdateAndDelete() {
        val (server, session) = seeded()
        val vm = LinksViewModel(session)
        waitFor { vm.loadState.takeIf { it is Load.Loaded } }

        var done = false
        vm.save(null, SocialLinkInput("Mastodon", "https://m.example", true)) { done = true }
        waitFor { if (done) true else null }
        assertEquals(listOf("GitHub", "Email", "Blog", "Mastodon"), vm.names())

        vm.save(2, SocialLinkInput("Mail", "mailto:x@y.z", true))
        waitFor { vm.names().takeIf { "Mail" in it } }
        assertTrue(server.links.single { it.id == 2L }.isPublic)

        vm.delete(1)
        waitFor { vm.names().takeIf { "GitHub" !in it } }
        assertEquals(listOf("Mail", "Blog", "Mastodon"), server.links.map { it.name })
    }

    @Test
    fun screenListsLinksWithVisibilityAndControls() = runDesktopComposeUiTest(1000, 700) {
        val (_, session) = seeded()
        val vm = LinksViewModel(session)
        setContent { PortfolioTheme { TestContent(session) { LinksScreen(vm) } } }
        awaitText("GitHub")
        onNodeWithText("Email").assertIsDisplayed()
        onNodeWithText("Private").assertIsDisplayed()
        assertEquals(2, onAllNodesWithText("Public").fetchSemanticsNodes().size)
        onNodeWithText("Add link").assertIsDisplayed()
        val dir = File("build/ui-screenshots").also { it.mkdirs() }
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "admin-links.png"))
    }

    @Test
    fun addingALinkThroughTheDialog() = runDesktopComposeUiTest(1000, 800) {
        val (server, session) = seeded()
        val vm = LinksViewModel(session)
        setContent { PortfolioTheme { TestContent(session) { LinksScreen(vm) } } }
        awaitText("GitHub")

        onNodeWithText("Add link").performClick()
        onNodeWithText("Name").performTextInput("Mastodon")
        onNodeWithText("https://").performTextClearance()
        onNodeWithText("Address").performTextInput("https://m.example/@ada")
        onNodeWithText("Private: only you can see it").assertIsDisplayed()
        onNodeWithText("Save").performClick()

        awaitText("Mastodon")
        assertEquals("https://m.example/@ada", server.links.last().address)
        assertEquals(false, server.links.last().isPublic)
    }

    @Test
    fun deleteAsksForConfirmationFirst() = runDesktopComposeUiTest(1000, 700) {
        val (server, session) = seeded()
        val vm = LinksViewModel(session)
        setContent { PortfolioTheme { TestContent(session) { LinksScreen(vm) } } }
        awaitText("GitHub")

        onAllNodesWithText("Delete")[0].performClick()
        onNodeWithText("Delete \"GitHub\"?").assertIsDisplayed()
        onNodeWithText("Cancel").performClick()
        assertEquals(3, server.links.size, "cancelling keeps the link")

        onAllNodesWithText("Delete")[0].performClick()
        val deletes = onAllNodesWithText("Delete")
        deletes[deletes.fetchSemanticsNodes().size - 1].performClick()
        waitFor { if (server.links.size == 2) true else null }
        assertEquals(listOf("Email", "Blog"), server.links.map { it.name })
    }
}
