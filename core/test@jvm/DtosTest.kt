package com.usuallycoding.portfolio

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DtosTest {
    private val json = Json

    private val image = ImageDto("a1", "a1.jpg", 800, 450)

    private inline fun <reified T> roundTrip(value: T) {
        assertEquals(value, json.decodeFromString<T>(json.encodeToString(value)))
    }

    @Test
    fun profileRoundTrips() = roundTrip(ProfileDto("Ada", "Engineer", image, image))

    @Test
    fun profileDefaultsToNoImages() {
        val decoded = json.decodeFromString<ProfileDto>("""{"name":"n","description":"d"}""")
        assertNull(decoded.profileImage)
        assertNull(decoded.coverImage)
    }

    @Test
    fun linkRoundTrips() = roundTrip(SocialLinkDto(1, "GitHub", "https://github.com", true, image))

    @Test
    fun projectRoundTrips() =
        roundTrip(ProjectDto(2, "App", "2026-01-31", false, "summary", image, listOf(image, image)))

    @Test
    fun projectInputKeepsImageOrder() {
        val input = ProjectInput("t", "2026-01-01", true, "s", "thumb", listOf("c", "a", "b"))
        assertEquals(listOf("c", "a", "b"), json.decodeFromString<ProjectInput>(json.encodeToString(input)).imageIds)
    }

    @Test
    fun orderLoginAndErrorRoundTrip() {
        roundTrip(OrderRequest(listOf(3, 1, 2)))
        roundTrip(LoginRequest("pw"))
        roundTrip(LoginResponse("tok", 123))
        roundTrip(ErrorDto("nope"))
    }
}
