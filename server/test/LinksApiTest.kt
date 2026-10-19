package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.SocialLinkInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinksApiTest {
    private fun link(name: String, public: Boolean = true, iconId: String? = null, address: String = "https://example.com/$name") =
        SocialLinkInput(name, address, public, iconId)

    @Test
    fun newLinksAreAppendedInCreationOrder() = withServer {
        api.login(TEST_PASSWORD)
        api.createLink(link("a")); api.createLink(link("b")); api.createLink(link("c"))
        assertEquals(listOf("a", "b", "c"), asVisitor { links() }.map { it.name })
    }

    @Test
    fun privateLinksAreHiddenFromVisitorsButVisibleToTheOwner() = withServer {
        api.login(TEST_PASSWORD)
        api.createLink(link("public"))
        api.createLink(link("private", public = false))

        assertEquals(listOf("public"), asVisitor { links() }.map { it.name })
        assertEquals(listOf("public", "private"), api.adminLinks().map { it.name })
        assertStatus(401) { asVisitor { adminLinks() } }
    }

    @Test
    fun reorderChangesBothViewsAndKeepsPrivateLinksInPlace() = withServer {
        api.login(TEST_PASSWORD)
        val a = api.createLink(link("a"))
        val b = api.createLink(link("b", public = false))
        val c = api.createLink(link("c"))

        val result = api.reorderLinks(listOf(c.id, b.id, a.id))

        assertEquals(listOf("c", "b", "a"), result.map { it.name })
        assertEquals(listOf("c", "a"), asVisitor { links() }.map { it.name })
    }

    @Test
    fun incompleteDuplicatedOrUnknownOrdersAreRejectedAndKeepTheOldOrder() = withServer {
        api.login(TEST_PASSWORD)
        val a = api.createLink(link("a"))
        val b = api.createLink(link("b"))

        assertStatus(400) { api.reorderLinks(listOf(b.id)) }
        assertStatus(400) { api.reorderLinks(listOf(b.id, b.id)) }
        assertStatus(400) { api.reorderLinks(listOf(b.id, a.id, 999)) }
        assertStatus(400) { api.reorderLinks(listOf(b.id, 999)) }

        assertEquals(listOf("a", "b"), api.adminLinks().map { it.name })
    }

    @Test
    fun ownerEditsAllFields() = withServer {
        api.login(TEST_PASSWORD)
        val icon = square()
        val created = api.createLink(link("old", public = false))

        val updated = api.updateLink(created.id, SocialLinkInput("New", "mailto:me@example.com", true, icon.id))

        assertEquals(SocialLinkDto2(created.id, "New", "mailto:me@example.com", true, icon), updated.let { SocialLinkDto2(it.id, it.name, it.address, it.isPublic, it.icon) })
        assertEquals(listOf("New"), asVisitor { links() }.map { it.name })
    }

    @Test
    fun deletingALinkRemovesItAndOrphansItsIcon() = withServer {
        api.login(TEST_PASSWORD)
        val icon = square()
        val l = api.createLink(link("a", iconId = icon.id))
        api.createLink(link("b"))

        api.deleteLink(l.id)

        assertEquals(listOf("b"), api.adminLinks().map { it.name })
        ageAndCleanUp()
        assertFalse(imageRowExists(icon.id))
    }

    @Test
    fun wrongIconRatioIsRejectedAndKeepsThePreviousIcon() = withServer {
        api.login(TEST_PASSWORD)
        val icon = square()
        val l = api.createLink(link("a", iconId = icon.id))

        assertStatus(400) { api.updateLink(l.id, link("a", iconId = wide().id)) }
        assertStatus(400) { api.createLink(link("b", iconId = cover().id)) }

        assertEquals(icon, api.adminLinks().single().icon)
    }

    @Test
    fun unsafeOrMissingAddressesAreRejected() = withServer {
        api.login(TEST_PASSWORD)
        assertStatus(400) { api.createLink(link("x", address = "javascript:alert(1)")) }
        assertStatus(400) { api.createLink(link("x", address = "example.com")) }
        assertStatus(400) { api.createLink(link("x", address = " ")) }
        assertStatus(400) { api.createLink(link("  ")) }
        assertTrue(api.adminLinks().isEmpty())
        for (ok in listOf("http://a.b", "https://a.b/c", "mailto:a@b.c", "tel:+40123")) api.createLink(link("ok", address = ok))
        assertEquals(4, api.adminLinks().size)
    }

    @Test
    fun missingLinksAre404AndWritesNeedAToken() = withServer {
        api.login(TEST_PASSWORD)
        assertStatus(404) { api.updateLink(42, link("a")) }
        assertStatus(404) { api.deleteLink(42) }
        assertStatus(401) { asVisitor { createLink(link("a")) } }
        assertStatus(401) { asVisitor { deleteLink(1) } }
        assertStatus(401) { asVisitor { reorderLinks(emptyList()) } }
    }

    @Test
    fun linkWithoutIconHasNullIcon() = withServer {
        api.login(TEST_PASSWORD)
        assertNull(api.createLink(link("a")).icon)
    }

    private data class SocialLinkDto2(val id: Long, val name: String, val address: String, val isPublic: Boolean, val icon: com.usuallycoding.portfolio.ImageDto?)
}
