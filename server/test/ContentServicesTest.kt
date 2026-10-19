package com.usuallycoding.portfolio.server.service

import com.usuallycoding.portfolio.ProfileInput
import com.usuallycoding.portfolio.ProjectInput
import com.usuallycoding.portfolio.SocialLinkInput
import com.usuallycoding.portfolio.server.domain.DomainError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContentServicesTest {
    private val store = FakeTransactor().apply {
        addImage("square", 500, 500)
        addImage("cover", 900, 300)
        addImage("wide", 1600, 900)
    }
    private val profile = ProfileService(store, 0.02)
    private val links = LinkService(store, 0.02)
    private val projects = ProjectService(store, 0.02)

    private fun link(name: String = "GitHub", address: String = "https://github.com/a", isPublic: Boolean = true, iconId: String? = null) =
        SocialLinkInput(name, address, isPublic, iconId)

    private fun project(
        title: String = "App",
        isPublic: Boolean = true,
        thumbnailId: String? = null,
        imageIds: List<String> = emptyList(),
        createdAt: String = "2026-01-02",
    ) = ProjectInput(title, createdAt, isPublic, "summary", thumbnailId, imageIds)

    // --- profile ---

    @Test
    fun aMissingProfileIsAPlaceholder() = runTest {
        assertEquals(PLACEHOLDER_NAME, profile.get().name)
    }

    @Test
    fun profileIsTrimmedAndKeepsItsImages() = runTest {
        val saved = profile.update(ProfileInput("  Ada ", "Engineer", "square", "cover"))
        assertEquals("Ada", saved.name)
        assertEquals("square", saved.profileImage?.id)
        assertEquals("cover", saved.coverImage?.id)
    }

    @Test
    fun profileRulesAreEnforced() = runTest {
        assertFailsWith<DomainError.Invalid> { profile.update(ProfileInput(" ", "d")) }
        assertFailsWith<DomainError.Invalid> { profile.update(ProfileInput("n", "d", profileImageId = "cover")) } // wrong ratio
        assertFailsWith<DomainError.Invalid> { profile.update(ProfileInput("n", "d", coverImageId = "missing")) }
        assertNull(store.state.profile, "a rejected update stores nothing")
    }

    // --- links ---

    @Test
    fun linksAreCreatedInOrderAndFilteredForVisitors() = runTest {
        links.create(link("A"))
        links.create(link("B", isPublic = false))
        links.create(link("C"))
        assertEquals(listOf("A", "B", "C"), links.list(publicOnly = false).map { it.name })
        assertEquals(listOf("A", "C"), links.list(publicOnly = true).map { it.name })
    }

    @Test
    fun linkRulesAreEnforced() = runTest {
        assertFailsWith<DomainError.Invalid> { links.create(link(name = "")) }
        assertFailsWith<DomainError.Invalid> { links.create(link(address = "javascript:alert(1)")) }
        assertFailsWith<DomainError.Invalid> { links.create(link(iconId = "wide")) } // icons are square
        assertTrue(links.list(publicOnly = false).isEmpty())
    }

    @Test
    fun updatingOrDeletingAnUnknownLinkIsNotFound() = runTest {
        assertFailsWith<DomainError.NotFound> { links.update(99, link()) }
        assertFailsWith<DomainError.NotFound> { links.delete(99) }
    }

    @Test
    fun reorderingNeedsEveryIdExactlyOnce() = runTest {
        val a = links.create(link("A")).id
        val b = links.create(link("B")).id
        assertEquals(listOf("B", "A"), links.reorder(listOf(b, a)).map { it.name })
        assertFailsWith<DomainError.Invalid> { links.reorder(listOf(a)) }
        assertFailsWith<DomainError.Invalid> { links.reorder(listOf(a, a)) }
        assertFailsWith<DomainError.Invalid> { links.reorder(listOf(a, b, 99)) }
        assertEquals(listOf("B", "A"), links.list(publicOnly = false).map { it.name }, "failed reorders change nothing")
    }

    // --- projects ---

    @Test
    fun privateProjectsAreHiddenFromVisitors() = runTest {
        val id = projects.create(project(isPublic = false)).id
        assertEquals("App", projects.get(id, publicOnly = false).title)
        assertFailsWith<DomainError.NotFound> { projects.get(id, publicOnly = true) }
        assertTrue(projects.list(publicOnly = true).isEmpty())
    }

    @Test
    fun projectKeepsItsImagesInOrder() = runTest {
        val saved = projects.create(project(thumbnailId = "wide", imageIds = listOf("cover", "square")))
        assertEquals(listOf("cover", "square"), saved.images.map { it.id })
        assertEquals("wide", saved.thumbnail?.id)
    }

    @Test
    fun projectRulesAreEnforcedAndNothingIsKeptWhenOneFails() = runTest {
        assertFailsWith<DomainError.Invalid> { projects.create(project(title = " ")) }
        assertFailsWith<DomainError.Invalid> { projects.create(project(createdAt = "yesterday")) }
        assertFailsWith<DomainError.Invalid> { projects.create(project(thumbnailId = "square")) } // thumbnails are 16:9
        assertFailsWith<DomainError.Invalid> { projects.create(project(imageIds = listOf("square", "square"))) }
        assertFailsWith<DomainError.Invalid> { projects.create(project(imageIds = listOf("square", "missing"))) }
        assertFailsWith<DomainError.Invalid> { projects.create(project(imageIds = List(201) { "square" })) }
        assertTrue(projects.list(publicOnly = false).isEmpty())
    }

    @Test
    fun aFailedUpdateLeavesTheProjectAsItWas() = runTest {
        val id = projects.create(project(title = "Before", imageIds = listOf("square"))).id
        assertFailsWith<DomainError.Invalid> { projects.update(id, project(title = "After", imageIds = listOf("square", "missing"))) }
        val kept = projects.get(id, publicOnly = false)
        assertEquals("Before", kept.title)
        assertEquals(listOf("square"), kept.images.map { it.id })
    }

    @Test
    fun updatingOrDeletingAnUnknownProjectIsNotFound() = runTest {
        assertFailsWith<DomainError.NotFound> { projects.update(99, project()) }
        assertFailsWith<DomainError.NotFound> { projects.delete(99) }
    }
}
