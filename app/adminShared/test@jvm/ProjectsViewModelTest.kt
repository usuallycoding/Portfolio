package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.admin.data.*
import com.usuallycoding.portfolio.admin.presentation.*

import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.ui.Load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun seeded(): Pair<FakeServer, ProjectsViewModel> {
        val server = FakeServer()
        server.projects += listOf(
            ProjectDto(1, "First", "2026-01-01", true, ""),
            ProjectDto(2, "Second", "2025-01-01", false, ""),
            ProjectDto(3, "Third", "2024-01-01", true, ""),
        )
        val session = server.session().also { runBlocking { it.login("secret") } }
        return server to ProjectsViewModel(session)
    }

    private fun ProjectsViewModel.titles() = assertIs<Load.Loaded<List<ProjectSummaryDto>>>(loadState).value.map { it.title }

    @Test
    fun listsAllProjectsIncludingPrivateOnesInOrder() {
        val (_, vm) = seeded()
        assertEquals(listOf("First", "Second", "Third"), vm.titles())
    }

    @Test
    fun movingSavesTheOrderOnTheServer() {
        val (server, vm) = seeded()
        vm.move(2, -2)
        assertEquals(listOf("Third", "First", "Second"), vm.titles())
        assertEquals(listOf("Third", "First", "Second"), server.projects.map { it.title })
    }

    @Test
    fun movingBeyondTheEndsSendsNothing() {
        val (server, vm) = seeded()
        val before = server.requests.size
        vm.move(0, -1); vm.move(2, 1)
        assertEquals(before, server.requests.size)
    }

    @Test
    fun aFailedReorderShowsAnErrorAndKeepsTheOldOrder() {
        val (server, vm) = seeded()
        server.rejectToken = true
        vm.move(1, -1)
        assertEquals(listOf("First", "Second", "Third"), vm.titles())
        assertEquals("Your session expired. Please log in again.", vm.error)
    }

    @Test
    fun everyEditorOpeningIsFreshAndClosingReloadsTheList() {
        val (server, vm) = seeded()
        assertNull(vm.editor)

        vm.open(null)
        val first = vm.editor!!
        assertNull(first.id)
        vm.closeEditor()
        assertNull(vm.editor)

        vm.open(null)
        assertNotEquals(first.generation, vm.editor!!.generation, "a new project form must not reuse the previous draft")

        server.projects += ProjectDto(9, "Added elsewhere", "2026-02-02", true, "")
        vm.closeEditor()
        assertEquals(4, vm.titles().size, "closing the editor reloads the list")
    }
}
