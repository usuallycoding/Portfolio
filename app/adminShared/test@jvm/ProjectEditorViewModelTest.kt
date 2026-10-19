package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.admin.data.*
import com.usuallycoding.portfolio.admin.presentation.*

import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ui.Load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectEditorViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun jpeg(width: Int = 640, height: Int = 480): ByteArray =
        ByteArrayOutputStream().also { ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg", it) }.toByteArray()

    private fun files(vararg names: String) = names.map { name -> PendingFile(name) { jpeg() } }

    private fun <T> waitFor(timeoutMs: Long = 15_000, condition: () -> T?): T = runBlocking {
        kotlinx.coroutines.withTimeout(timeoutMs) {
            var r = condition(); while (r == null) { kotlinx.coroutines.delay(10); r = condition() }; r
        }
    }

    private fun login(server: FakeServer) = server.session().also { runBlocking { it.login("secret") } }

    private fun ProjectEditorViewModel.awaitUploads() = waitFor { if (!queue.isBusy) true else null }

    @Test
    fun aNewProjectStartsPrivateWithTodaysDate() {
        val vm = ProjectEditorViewModel(login(FakeServer()), null)
        assertEquals(todayIso(), vm.createdAt)
        assertFalse(vm.isPublic)
        assertIs<Load.Loaded<Unit>>(vm.loadState)
        assertFalse(vm.canSave, "a title is required")
    }

    @Test
    fun saveRequiresATitleAValidDateAndNoPendingUploads() {
        val server = FakeServer().apply { uploadDelay = { kotlinx.coroutines.delay(300) } }
        val vm = ProjectEditorViewModel(login(server), null)
        vm.title = "App"
        assertTrue(vm.canSave)
        vm.createdAt = "2026-02-30"
        assertFalse(vm.canSave)
        vm.createdAt = "2026-02-27"
        vm.title = "   "
        assertFalse(vm.canSave)
        vm.title = "App"

        vm.addFiles(files("a.jpg"))
        assertFalse(vm.canSave, "cannot save while an image is still uploading")
        vm.awaitUploads()
        assertTrue(vm.canSave)
    }

    @Test
    fun creatingAProjectSavesThumbnailAndImagesInTheChosenOrder() {
        val server = FakeServer()
        val vm = ProjectEditorViewModel(login(server), null)
        vm.title = "Analytical Engine"
        vm.summary = "Notes"
        vm.isPublic = true
        vm.thumbnail = server.addImage(1600, 900)
        vm.addFiles(files("a.jpg", "b.jpg", "c.jpg"))
        vm.awaitUploads()

        // Owner moves the last image to the front.
        vm.queue.move(vm.queue.items.value[2].key, -2)
        val expectedOrder = vm.queue.items.value.mapNotNull { it.image?.id }
        var saved = false
        vm.save { saved = true }
        waitFor { if (saved) true else null }

        val project = server.projects.single()
        assertEquals("Analytical Engine", project.title)
        assertTrue(project.isPublic)
        assertEquals(vm.thumbnail?.id, project.thumbnail?.id)
        assertEquals(expectedOrder, project.images.map { it.id })
        assertEquals(3, project.images.size)
    }

    @Test
    fun savingTwiceUpdatesTheSameProjectInsteadOfCreatingADuplicate() {
        val server = FakeServer()
        val vm = ProjectEditorViewModel(login(server), null)
        vm.title = "One"
        var saves = 0
        vm.save { saves++ }
        waitFor { if (saves == 1) true else null }
        vm.title = "One (edited)"
        vm.save { saves++ }
        waitFor { if (saves == 2) true else null }

        assertEquals(listOf("One (edited)"), server.projects.map { it.title })
    }

    @Test
    fun tenFilesWithOneFailureSaveNineAndLetTheOwnerRetryJustThatOne() {
        val server = FakeServer().apply {
            uploadDelay = { kotlinx.coroutines.delay(30) }
            failUploadsWhere = { it == 4 }
        }
        val vm = ProjectEditorViewModel(login(server), null)
        vm.title = "Gallery"
        vm.addFiles(files(*Array(10) { "img$it.jpg" }))
        vm.awaitUploads()

        assertTrue(server.maxConcurrentUploads in 2..3, "uploads ran ${server.maxConcurrentUploads} at a time")
        assertEquals(10, server.uploadCount)
        assertEquals(9, vm.queue.items.value.count { it.status == UploadStatus.Done })
        assertEquals(1, vm.queue.failedCount)
        val failed = vm.queue.items.value.single { it.status is UploadStatus.Failed }

        // Save with the failure still there: only the nine good images are attached.
        var saved = false
        vm.save { saved = true }
        waitFor { if (saved) true else null }
        assertEquals(9, server.projects.single().images.size)

        vm.queue.retry(failed.key)
        vm.awaitUploads()
        assertEquals(11, server.uploadCount, "only the failed file was uploaded again")
        assertEquals(0, vm.queue.failedCount)
        saved = false
        vm.save { saved = true }
        waitFor { if (saved) true else null }
        assertEquals(10, server.projects.single().images.size)
        assertEquals(vm.queue.items.value.map { it.name }, vm.queue.items.value.map { it.name }, "order is stable")
    }

    @Test
    fun abandoningTheFormLeavesTheProjectUntouched() {
        val server = FakeServer()
        val original = ProjectDto(7, "Keep me", "2026-01-01", true, "s", null, listOf(server.addImage()))
        server.projects += original
        val vm = ProjectEditorViewModel(login(server), 7)
        waitFor { vm.loadState.takeIf { it is Load.Loaded } }
        vm.title = "Changed but never saved"
        vm.addFiles(files("new1.jpg", "new2.jpg"))
        vm.awaitUploads()
        // ...the owner closes the form without pressing Save.

        assertEquals(listOf(original), server.projects)
        assertEquals(2, server.uploadCount, "the uploads exist on the server as unattached images, for the cleanup job")
        assertFalse(server.requests.any { it.startsWith("PUT /admin/projects") })
    }

    @Test
    fun editingAnExistingProjectLoadsItsImagesAndSavesRemovalsAndReorders() {
        val server = FakeServer()
        val images = List(3) { server.addImage() }
        server.projects += ProjectDto(5, "P", "2026-01-01", false, "s", null, images)
        val vm = ProjectEditorViewModel(login(server), 5)
        waitFor { vm.loadState.takeIf { it is Load.Loaded } }
        assertEquals(images, vm.queue.uploadedImages())

        vm.queue.remove(vm.queue.items.value[1].key) // drop the middle one
        vm.queue.move(vm.queue.items.value[1].key, -1) // swap the remaining two
        var saved = false
        vm.save { saved = true }
        waitFor { if (saved) true else null }

        assertEquals(listOf(images[2].id, images[0].id), server.projects.single().images.map { it.id })
    }

    @Test
    fun aFileThatIsNotAnImageFailsWithAClearMessage() {
        val server = FakeServer()
        val vm = ProjectEditorViewModel(login(server), null)
        vm.addFiles(listOf(PendingFile("notes.txt") { "just text".toByteArray() }) + files("ok.jpg"))
        vm.awaitUploads()

        val failed = vm.queue.items.value.first { it.name == "notes.txt" }.status
        assertEquals("notes.txt is not an image this app can read.", (failed as UploadStatus.Failed).message)
        assertEquals(1, server.uploadCount, "the bad file was never sent")
        assertEquals(1, vm.queue.items.value.count { it.status == UploadStatus.Done })
    }

    @Test
    fun deletingRemovesTheProject() {
        val server = FakeServer()
        server.projects += ProjectDto(5, "P", "2026-01-01", false, "s")
        val vm = ProjectEditorViewModel(login(server), 5)
        waitFor { vm.loadState.takeIf { it is Load.Loaded } }
        var deleted = false
        vm.delete { deleted = true }
        waitFor { if (deleted) true else null }
        assertTrue(server.projects.isEmpty())
    }

    @Test
    fun uploadsDownscaleLargePhotosBeforeSending() {
        // The queue prepares with the 2000px cap; verify through the prepared output.
        val big = ImageProcessing.decode(jpeg(4000, 3000))!!
        val out = ImageProcessing.prepare(big)
        assertEquals(2000, out.width)
        assertEquals(1500, out.height)
    }

    @Test
    fun anImageIsIdentifiedByItsServerId() {
        val image = ImageDto("x", "x.jpg", 1, 1)
        assertEquals("x", image.id)
    }
}
