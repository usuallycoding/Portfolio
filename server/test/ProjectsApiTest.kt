package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.ProjectInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectsApiTest {
    private fun project(
        title: String,
        public: Boolean = true,
        createdAt: String = "2026-01-15",
        thumbnailId: String? = null,
        images: List<String> = emptyList(),
    ) = ProjectInput(title, createdAt, public, "About $title", thumbnailId, images)

    @Test
    fun createdProjectHasAllFieldsAndImagesInTheGivenOrder() = withServer {
        val thumb = wide()
        val (i1, i2, i3) = listOf(upload(300, 200), upload(300, 200), upload(300, 200))

        val created = api.createProject(project("App", thumbnailId = thumb.id, images = listOf(i3.id, i1.id, i2.id)))

        assertEquals("App", created.title)
        assertEquals("2026-01-15", created.createdAt)
        assertEquals("About App", created.summary)
        assertEquals(thumb, created.thumbnail)
        assertEquals(listOf(i3, i1, i2), created.images)
        assertEquals(created, api.adminProject(created.id))
    }

    @Test
    fun publicDetailShowsMetadataThumbnailAndOrderedImages() = withServer {
        val a = upload(300, 200)
        val b = upload(300, 200)
        val thumb = wide()
        val p = api.createProject(project("P", thumbnailId = thumb.id, images = listOf(b.id, a.id)))

        val seen = asVisitor { project(p.id) }

        assertEquals(listOf(b, a), seen.images)
        assertEquals(thumb, seen.thumbnail)
        assertEquals(200, statusOf("/images/${a.fileName}").value)
        assertEquals(200, statusOf("/images/${thumb.fileName}?size=thumb").value)
    }

    @Test
    fun privateProjectsAreAbsentFromListsAnd404ByIdForVisitors() = withServer {
        api.login(TEST_PASSWORD)
        val shown = api.createProject(project("shown"))
        val hidden = api.createProject(project("hidden", public = false))

        assertEquals(listOf("shown"), asVisitor { projects() }.map { it.title })
        assertStatus(404) { asVisitor { project(hidden.id) } }
        assertEquals(shown.id, asVisitor { project(shown.id) }.id)
        assertEquals(listOf("shown", "hidden"), api.adminProjects().map { it.title })
        assertEquals("hidden", api.adminProject(hidden.id).title)
        assertStatus(401) { asVisitor { adminProject(hidden.id) } }
    }

    @Test
    fun privateProjectImagesAreNotServedToVisitors() = withServer {
        val image = upload(300, 200)
        api.createProject(project("hidden", public = false, images = listOf(image.id)))
        assertEquals(404, statusOf("/images/${image.fileName}").value)
    }

    @Test
    fun projectsFollowTheOwnersOrderNotTheCreationDate() = withServer {
        api.login(TEST_PASSWORD)
        val old = api.createProject(project("old", createdAt = "2020-01-01"))
        val new = api.createProject(project("new", createdAt = "2026-06-01"))
        val mid = api.createProject(project("mid", createdAt = "2023-01-01"))
        assertEquals(listOf("old", "new", "mid"), asVisitor { projects() }.map { it.title })

        api.reorderProjects(listOf(new.id, mid.id, old.id))

        assertEquals(listOf("new", "mid", "old"), asVisitor { projects() }.map { it.title })
    }

    @Test
    fun incompleteOrDuplicatedProjectOrdersAreRejected() = withServer {
        api.login(TEST_PASSWORD)
        val a = api.createProject(project("a"))
        val b = api.createProject(project("b"))
        assertStatus(400) { api.reorderProjects(listOf(a.id)) }
        assertStatus(400) { api.reorderProjects(listOf(a.id, a.id)) }
        assertStatus(400) { api.reorderProjects(listOf(b.id, a.id, 77)) }
        assertEquals(listOf("a", "b"), api.adminProjects().map { it.title })
    }

    @Test
    fun savingReordersAndRemovesImagesAndRemovedOnesBecomeOrphans() = withServer {
        val a = upload(300, 200)
        val b = upload(300, 200)
        val c = upload(300, 200)
        val p = api.createProject(project("P", images = listOf(a.id, b.id, c.id)))

        val updated = api.updateProject(p.id, project("P", images = listOf(c.id, a.id)))

        assertEquals(listOf(c, a), updated.images)
        ageAndCleanUp()
        assertFalse(imageRowExists(b.id))
        assertTrue(imageRowExists(a.id) && imageRowExists(c.id))
    }

    @Test
    fun wrongThumbnailRatioIsRejected() = withServer {
        api.login(TEST_PASSWORD)
        assertStatus(400) { api.createProject(project("p", thumbnailId = square().id)) }
        val p = api.createProject(project("p", thumbnailId = wide().id))
        assertStatus(400) { api.updateProject(p.id, project("p", thumbnailId = cover().id)) }
        assertEquals(p.thumbnail, api.adminProject(p.id).thumbnail)
    }

    @Test
    fun aFailedSaveLeavesTheProjectCompletelyUnchanged() = withServer {
        val a = upload(300, 200)
        val p = api.createProject(project("Original", images = listOf(a.id)))

        assertStatus(400) { api.updateProject(p.id, project("Changed", images = listOf(a.id, "no-such-image"))) }

        assertEquals(p, api.adminProject(p.id))
    }

    @Test
    fun invalidInputIsRejected() = withServer {
        api.login(TEST_PASSWORD)
        val a = upload(300, 200)
        assertStatus(400) { api.createProject(project("  ")) }
        assertStatus(400) { api.createProject(project("p", createdAt = "31/01/2026")) }
        assertStatus(400) { api.createProject(project("p", createdAt = "2026-02-30")) }
        assertStatus(400) { api.createProject(project("p", images = listOf(a.id, a.id))) }
        assertStatus(400) { api.createProject(project("p", thumbnailId = "nope")) }
        assertTrue(api.adminProjects().isEmpty())
    }

    @Test
    fun deletingAProjectRemovesItAndOrphansItsImages() = withServer {
        val thumb = wide()
        val image = upload(300, 200)
        val keep = api.createProject(project("keep"))
        val gone = api.createProject(project("gone", thumbnailId = thumb.id, images = listOf(image.id)))

        api.deleteProject(gone.id)

        assertEquals(listOf(keep.id), api.adminProjects().map { it.id })
        assertStatus(404) { api.adminProject(gone.id) }
        ageAndCleanUp()
        assertFalse(imageRowExists(thumb.id))
        assertFalse(imageRowExists(image.id))
    }

    @Test
    fun sameImageCanBeUsedByTwoProjectsAndSurvivesDeletingOne() = withServer {
        val shared = upload(300, 200)
        val p1 = api.createProject(project("p1", images = listOf(shared.id)))
        api.createProject(project("p2", images = listOf(shared.id)))

        api.deleteProject(p1.id)
        ageAndCleanUp()

        assertTrue(imageRowExists(shared.id))
    }

    @Test
    fun missingProjectsAre404AndWritesNeedAToken() = withServer {
        api.login(TEST_PASSWORD)
        assertStatus(404) { api.updateProject(9, project("p")) }
        assertStatus(404) { api.deleteProject(9) }
        assertStatus(404) { asVisitor { project(9) } }
        assertStatus(401) { asVisitor { createProject(project("p")) } }
        assertStatus(401) { asVisitor { deleteProject(1) } }
        assertStatus(401) { asVisitor { reorderProjects(emptyList()) } }
    }
}
