package com.usuallycoding.portfolio.admin

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProjectDto
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
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ProjectsScreenTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun seeded(): FakeServer {
        val server = FakeServer()
        val images = List(3) { ImageDto("img$it", "img$it.jpg", 800, 600).also { img -> server.images[img.id] = img } }
        server.projects += ProjectDto(1, "Analytical Engine", "2026-01-02", true, "Notes on the engine.", null, images)
        server.projects += ProjectDto(2, "Private draft", "2026-02-01", false, "")
        return server
    }

    private fun login(server: FakeServer) = server.session().also { runBlocking { it.login("secret") } }

    @Test
    fun listShowsEveryProjectWithItsVisibility() = runDesktopComposeUiTest(1000, 600) {
        val session = login(seeded())
        setContent {
            TestContent(session) { ProjectsScreen(session, ProjectsViewModel(session)) }
        }
        awaitText("Analytical Engine")
        onNodeWithText("Private draft").assertIsDisplayed()
        onNodeWithText("Public").assertIsDisplayed()
        onNodeWithText("Private").assertIsDisplayed()
        onNodeWithText("New project").assertIsDisplayed()
    }

    @Test
    fun newProjectFormSavesWhenTheTitleIsFilledIn() = runDesktopComposeUiTest(1000, 1400) {
        val server = seeded()
        val session = login(server)
        val vm = ProjectsViewModel(session)
        setContent {
            TestContent(session) { ProjectsScreen(session, vm) }
        }
        awaitText("New project")
        onNodeWithText("New project").performClick()
        awaitText("Title")
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithText("Title").performTextInput("Bernoulli Numbers")
        onNodeWithText("Save").assertIsEnabled()
        onNodeWithText("Private: only you can see this project").assertIsDisplayed()
        onNodeWithText("Save").performClick()

        waitUntil(timeoutMillis = 5_000) { server.projects.size == 3 }
        assertEquals("Bernoulli Numbers", server.projects.last().title)
    }

    @Test
    fun invalidDateIsFlaggedAndBlocksSaving() = runDesktopComposeUiTest(1000, 1400) {
        val session = login(seeded())
        val vm = ProjectEditorViewModel(session, null)
        setContent { TestContent(session) { ProjectEditorScreen(vm, onClose = {}) } }
        awaitText("Title")
        onNodeWithText("Title").performTextInput("X")
        onNodeWithText(todayIso()).performTextClearance()
        onNodeWithText("Creation date").performTextInput("31/01/2026")
        onNodeWithText("Use the format yyyy-MM-dd, e.g. 2026-01-31").assertIsDisplayed()
        onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun existingProjectShowsItsGalleryAndCanReorderIt() = runDesktopComposeUiTest(1100, 1500) {
        val server = seeded()
        val session = login(server)
        val vm = ProjectEditorViewModel(session, 1)
        setContent { TestContent(session) { ProjectEditorScreen(vm, onClose = {}) } }
        awaitText("Edit project")
        onNodeWithText("Analytical Engine").assertIsDisplayed()
        onNodeWithText("3 in gallery").assertIsDisplayed()
        assertEquals(3, onAllNodesWithText("Remove").fetchSemanticsNodes().size)
        val dir = File("build/ui-screenshots").also { it.mkdirs() }
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "admin-project-editor.png"))

        // Move the first image later, then save: the server order follows.
        onAllNodesWithText("→")[0].performClick()
        onNodeWithText("Save").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { server.projects.first().images.map { it.id } == listOf("img1", "img0", "img2") }
    }

    @Test
    fun removingAGalleryImageAndSaving() = runDesktopComposeUiTest(1100, 1500) {
        val server = seeded()
        val session = login(server)
        val vm = ProjectEditorViewModel(session, 1)
        setContent { TestContent(session) { ProjectEditorScreen(vm, onClose = {}) } }
        awaitText("Edit project")
        onAllNodesWithText("Remove")[1].performClick()
        onNodeWithText("2 in gallery").assertIsDisplayed()
        onNodeWithText("Save").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { server.projects.first().images.size == 2 }
        assertEquals(listOf("img0", "img2"), server.projects.first().images.map { it.id })
    }

    @Test
    fun deletingAProjectAsksForConfirmation() = runDesktopComposeUiTest(1100, 1500) {
        val server = seeded()
        val session = login(server)
        var closed = false
        setContent {
            TestContent(session) { ProjectEditorScreen(ProjectEditorViewModel(session, 2), onClose = { closed = true }) }
        }
        awaitText("Edit project")
        onNodeWithText("Delete project").performScrollTo().performClick()
        onNodeWithText("Delete \"Private draft\"?").assertIsDisplayed()
        onNodeWithText("Cancel").performClick()
        assertEquals(2, server.projects.size)

        onNodeWithText("Delete project").performClick()
        val deletes = onAllNodesWithText("Delete")
        deletes[deletes.fetchSemanticsNodes().size - 1].performClick()
        waitUntil(timeoutMillis = 5_000) { server.projects.size == 1 }
        assertTrue(closed)
    }

    @Test
    fun anExpiredSessionReturnsToLoginAndTheUnsavedFormIsStillThereAfterwards() = runDesktopComposeUiTest(1100, 1400) {
        val server = seeded()
        val session = login(server)
        setContent { AdminApp(session) }
        awaitText("Projects")
        onAllNodesWithText("Projects")[0].performClick()
        awaitText("New project")
        onNodeWithText("New project").performClick()
        awaitText("Title")
        onNodeWithText("Title").performTextInput("Half written draft")

        server.rejectToken = true // the token expires while the owner is typing
        onNodeWithText("Save").performClick()
        awaitText("Log in")
        onNodeWithText("You have been logged out.").assertIsDisplayed()
        assertEquals(2, server.projects.size, "nothing was saved")

        server.rejectToken = false
        onNodeWithText("Password").performTextInput("secret")
        onNodeWithText("Log in").performClick()
        awaitText("Half written draft")
        onNodeWithText("Save").assertIsEnabled()
    }
}
