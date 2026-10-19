package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.ProfileInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileApiTest {
    @Test
    fun freshInstallServesPlaceholdersToAnonymousVisitors() = withServer {
        val profile = asVisitor { profile() }
        assertEquals(PLACEHOLDER_NAME, profile.name)
        assertEquals(PLACEHOLDER_DESCRIPTION, profile.description)
        assertNull(profile.profileImage)
        assertNull(profile.coverImage)
    }

    @Test
    fun ownerUpdatesAndVisitorsSeeTheNewValues() = withServer {
        val picture = square()
        val cover = cover()
        api.updateProfile(ProfileInput("Ada Lovelace", "Analyst", picture.id, cover.id))

        val seen = asVisitor { profile() }
        assertEquals("Ada Lovelace", seen.name)
        assertEquals("Analyst", seen.description)
        assertEquals(picture, seen.profileImage)
        assertEquals(cover, seen.coverImage)
        // The images are served publicly now that the public profile uses them.
        assertEquals(200, statusOf("/images/${picture.fileName}").value)
        assertEquals(200, statusOf("/images/${cover.fileName}?size=thumb").value)
    }

    @Test
    fun updatingTextOnlyKeepsNoImages() = withServer {
        api.login(TEST_PASSWORD)
        val saved = api.updateProfile(ProfileInput("  Ada  ", ""))
        assertEquals("Ada", saved.name)
        assertEquals("", saved.description)
        assertNull(saved.profileImage)
    }

    @Test
    fun wrongRatioIsRejectedAndProfileIsUnchanged() = withServer {
        val ok = square()
        api.updateProfile(ProfileInput("Before", "d", ok.id))

        assertStatus(400) { api.updateProfile(ProfileInput("After", "d", profileImageId = wide().id)) }
        assertStatus(400) { api.updateProfile(ProfileInput("After", "d", coverImageId = square().id)) }

        val seen = asVisitor { profile() }
        assertEquals("Before", seen.name)
        assertEquals(ok, seen.profileImage)
    }

    @Test
    fun unknownImageAndInvalidTextAreRejected() = withServer {
        api.login(TEST_PASSWORD)
        assertStatus(400) { api.updateProfile(ProfileInput("n", "d", profileImageId = "nope")) }
        assertStatus(400) { api.updateProfile(ProfileInput("   ", "d")) }
        assertStatus(400) { api.updateProfile(ProfileInput("x".repeat(201), "d")) }
    }

    @Test
    fun anonymousUpdateIs401() = withServer {
        assertStatus(401) { asVisitor { updateProfile(ProfileInput("hacked", "")) } }
        assertEquals(PLACEHOLDER_NAME, asVisitor { profile() }.name)
    }

    @Test
    fun replacedPictureBecomesAnOrphanAndIsCollected() = withServer {
        val first = square()
        val second = square()
        api.updateProfile(ProfileInput("n", "d", first.id))
        api.updateProfile(ProfileInput("n", "d", second.id))

        ageAndCleanUp()

        assertFalse(imageRowExists(first.id))
        assertTrue(imageRowExists(second.id))
        assertEquals(second, asVisitor { profile() }.profileImage)
    }

    @Test
    fun clearingAnImageDetachesIt() = withServer {
        val picture = square()
        api.updateProfile(ProfileInput("n", "d", picture.id))
        api.updateProfile(ProfileInput("n", "d", null))

        assertNull(asVisitor { profile() }.profileImage)
        ageAndCleanUp()
        assertFalse(imageRowExists(picture.id))
    }

    @Test
    fun adminProfileRequiresAToken() = withServer {
        assertStatus(401) { asVisitor { adminProfile() } }
        api.login(TEST_PASSWORD)
        assertEquals(PLACEHOLDER_NAME, api.adminProfile().name)
    }
}
