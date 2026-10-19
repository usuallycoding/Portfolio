package com.usuallycoding.portfolio.client

/** A page of the client app. */
sealed interface Route {
    data object Home : Route
    data class Project(val id: Long) : Route
}

object Routes {
    /**
     * Reads a URL hash such as `#/projects/7`. Anything that is not a recognised page (empty, unknown, a missing or
     * non-numeric id, extra path segments) means [Route.Home].
     */
    fun parse(hash: String): Route {
        val path = hash.removePrefix("#").substringBefore('?').substringBefore('#')
        val segments = path.split('/').filter { it.isNotEmpty() }
        if (segments.size == 2 && segments[0] == "projects") {
            // ASCII digits only: other Unicode digits parse differently on different platforms.
            val id = segments[1].takeIf { text -> text.all { it in '0'..'9' } }?.toLongOrNull()
            if (id != null && id > 0) return Route.Project(id)
        }
        return Route.Home
    }

    /** The canonical hash for [route]: `#/` for the profile, `#/projects/<id>` for a project. */
    fun format(route: Route): String = when (route) {
        Route.Home -> "#/"
        is Route.Project -> "#/projects/${route.id}"
    }
}

/**
 * Connects the app's current page to the outside world's address, which only the web has (the browser's URL and
 * history). Android and iOS pass no host and keep their own in-memory navigation.
 */
interface RouteHost {
    /** The page the address currently names. */
    fun current(): Route

    /** Shows [route] in the address and adds a history entry, so the browser's back returns to the current page. */
    fun push(route: Route)

    /** Shows [route] in the address in place of the current entry, without adding history. */
    fun replace(route: Route)

    /**
     * Calls [listener] when the address changes from outside the app (the browser's back or forward, an edited URL).
     * Returns a function that stops listening.
     */
    fun onChanged(listener: (Route) -> Unit): () -> Unit
}
