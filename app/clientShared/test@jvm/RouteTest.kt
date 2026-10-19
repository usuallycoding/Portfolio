package com.usuallycoding.portfolio.client

import kotlin.test.Test
import kotlin.test.assertEquals

class RouteTest {
    @Test
    fun theProfileIsTheRootAndTheDefault() {
        assertEquals(Route.Home, Routes.parse("#/"))
        assertEquals(Route.Home, Routes.parse("#"))
        assertEquals(Route.Home, Routes.parse(""))
        assertEquals(Route.Home, Routes.parse("/"))
    }

    @Test
    fun aProjectAddressNamesThatProject() {
        assertEquals(Route.Project(7), Routes.parse("#/projects/7"))
        assertEquals(Route.Project(123456789012), Routes.parse("#/projects/123456789012"))
        assertEquals(Route.Project(7), Routes.parse("#/projects/7/"), "a trailing slash is fine")
        assertEquals(Route.Project(7), Routes.parse("#//projects//7"), "so are doubled slashes")
        assertEquals(Route.Project(7), Routes.parse("projects/7"), "and a missing #")
    }

    @Test
    fun queryStringsAndFragmentsAreIgnored() {
        assertEquals(Route.Project(7), Routes.parse("#/projects/7?utm_source=x"))
        assertEquals(Route.Home, Routes.parse("#/?foo=bar"))
        assertEquals(Route.Project(7), Routes.parse("#/projects/7#section"))
    }

    @Test
    fun anythingElseIsTheProfile() {
        for (junk in listOf(
            "#/projects", "#/projects/", "#/projects/abc", "#/projects/7a", "#/projects/-3", "#/projects/0", "#/projects/1.5",
            "#/projects/7/extra", "#/projects/7/images/2", "#/project/7", "#/Projects/7", "#/admin", "#/projects/99999999999999999999",
            "#/projects/ 7", "#/../projects/7", "#/%2Fprojects%2F7", "#!/projects/7", "#/projects/7 ", "#/projects/٧",
        )) {
            assertEquals(Route.Home, Routes.parse(junk), "\"$junk\" should show the profile")
        }
    }

    @Test
    fun formattingGivesTheCanonicalHash() {
        assertEquals("#/", Routes.format(Route.Home))
        assertEquals("#/projects/7", Routes.format(Route.Project(7)))
    }

    @Test
    fun parsingWhatWeFormatGivesTheSameRoute() {
        for (route in listOf(Route.Home, Route.Project(1), Route.Project(42), Route.Project(Long.MAX_VALUE))) {
            assertEquals(route, Routes.parse(Routes.format(route)))
        }
    }

    @Test
    fun formattingWhatWeParseIsStable() {
        for (hash in listOf("#/", "#/projects/7")) assertEquals(hash, Routes.format(Routes.parse(hash)))
    }
}
