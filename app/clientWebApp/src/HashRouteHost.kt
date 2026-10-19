package com.usuallycoding.portfolio.client

import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * Keeps the browser's address in step with the page being shown, using hash URLs (`#/`, `#/projects/7`).
 * Hash URLs need no help from the web host: reload, bookmarks and shared links always load the app, which then
 * reads the hash. Pushing a route adds a history entry, so the browser's back and forward buttons work.
 */
class HashRouteHost : RouteHost {
    override fun current(): Route = readAddress()

    override fun push(route: Route) {
        val target = Routes.format(route)
        if (window.location.hash != target) window.location.hash = target
    }

    override fun replace(route: Route) {
        window.history.replaceState(null, "", Routes.format(route))
    }

    override fun onChanged(listener: (Route) -> Unit): () -> Unit {
        val handler: (Event) -> Unit = { listener(readAddress()) }
        window.addEventListener("hashchange", handler)
        return { window.removeEventListener("hashchange", handler) }
    }

    /** Reads the address; one that does not name a known page is replaced by the profile's, so the URL never lies. */
    private fun readAddress(): Route {
        val hash = window.location.hash
        val route = Routes.parse(hash)
        if (hash.isNotEmpty() && hash != Routes.format(route)) replace(route)
        return route
    }
}
