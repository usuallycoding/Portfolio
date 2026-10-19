package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.admin.data.*
import com.usuallycoding.portfolio.admin.presentation.*

import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ui.Load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private suspend fun loggedIn(server: FakeServer) = server.session().also { it.login("secret") }

    private fun <T> waitFor(timeoutMs: Long = 5_000, condition: () -> T?): T = kotlinx.coroutines.runBlocking {
        kotlinx.coroutines.withTimeout(timeoutMs) {
            var result = condition()
            while (result == null) { kotlinx.coroutines.delay(10); result = condition() }
            result
        }
    }

    @Test
    fun loadsTheCurrentProfileIntoTheForm() = runTest {
        val server = FakeServer().apply {
            profile = ProfileDto("Ada", "Analyst", addImage(), addImage(900, 300))
        }
        val vm = ProfileViewModel(loggedIn(server))
        waitFor { vm.loadState.takeIf { it is Load.Loaded } }

        assertEquals("Ada", vm.name)
        assertEquals("Analyst", vm.description)
        assertEquals("img-1", vm.profileImage?.id)
        assertEquals("img-2", vm.coverImage?.id)
        assertFalse(vm.hasChanges)
    }

    @Test
    fun savingSendsTheEditedFieldsAndImageIds() = runTest {
        val server = FakeServer()
        val vm = ProfileViewModel(loggedIn(server))
        waitFor { vm.loadState.takeIf { it is Load.Loaded } }

        vm.name = "Grace Hopper"
        vm.description = "Compiler pioneer"
        vm.profileImage = server.addImage()
        vm.coverImage = server.addImage(900, 300)
        assertTrue(vm.hasChanges)
        vm.save()
        waitFor { vm.message }

        assertEquals("Saved.", vm.message)
        assertFalse(vm.messageIsError)
        assertEquals("Grace Hopper", server.profile.name)
        assertEquals("Compiler pioneer", server.profile.description)
        assertEquals("img-1", server.profile.profileImage?.id)
        assertEquals("img-2", server.profile.coverImage?.id)
        assertFalse(vm.hasChanges, "after saving there is nothing left to save")
    }

    @Test
    fun removingAnImageClearsItOnTheServer() = runTest {
        val server = FakeServer().apply { profile = ProfileDto("Ada", "d", addImage()) }
        val vm = ProfileViewModel(loggedIn(server))
        waitFor { vm.loadState.takeIf { it is Load.Loaded } }

        vm.profileImage = null
        vm.save()
        waitFor { vm.message }

        assertNull(server.profile.profileImage)
    }

    @Test
    fun discardRestoresTheSavedValues() = runTest {
        val server = FakeServer().apply { profile = ProfileDto("Ada", "d") }
        val vm = ProfileViewModel(loggedIn(server))
        waitFor { vm.loadState.takeIf { it is Load.Loaded } }

        vm.name = "typo"
        vm.discard()

        assertEquals("Ada", vm.name)
        assertFalse(vm.hasChanges)
    }

    @Test
    fun aRejectedSaveShowsTheErrorAndKeepsTheEdits() = runTest {
        val server = FakeServer()
        val session = loggedIn(server)
        val vm = ProfileViewModel(session)
        waitFor { vm.loadState.takeIf { it is Load.Loaded } }
        vm.name = "Edited"

        server.rejectToken = true // the token expired
        vm.save()
        waitFor { vm.message }

        assertTrue(vm.messageIsError)
        assertEquals("Your session expired. Please log in again.", vm.message)
        assertEquals("Edited", vm.name, "unsaved edits survive the failed save")
        assertFalse(session.loggedIn.value, "an expired token sends the app back to login")
    }

    @Test
    fun loadFailureOffersRetry() = runTest {
        val server = FakeServer()
        val session = loggedIn(server)
        server.rejectToken = true
        val vm = ProfileViewModel(session)
        assertIs<Load.Failed>(waitFor { vm.loadState.takeIf { it is Load.Failed } })
    }
}
