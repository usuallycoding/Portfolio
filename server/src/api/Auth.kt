package com.usuallycoding.portfolio.server.api

import com.usuallycoding.portfolio.ApiPaths
import com.usuallycoding.portfolio.LoginRequest
import com.usuallycoding.portfolio.server.service.AuthService
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/** The name of the bearer authentication that guards every admin route. */
const val ADMIN_AUTH = "admin"

/**
 * True when the request carries a valid owner token. Unlike the auth plugin, a missing or invalid token
 * is not an error here: public routes treat it as an anonymous visitor.
 */
suspend fun ApplicationCall.hasValidOwnerToken(auth: AuthService): Boolean {
    val header = request.headers[HttpHeaders.Authorization] ?: return false
    if (!header.startsWith("Bearer ", ignoreCase = true)) return false
    return auth.isValid(header.substring(7).trim())
}

fun Route.loginRoute(auth: AuthService) {
    post(ApiPaths.ADMIN_LOGIN) {
        val client = call.request.origin.remoteHost
        auth.checkNotBlocked(client)
        val request = call.receive<LoginRequest>()
        call.respond(auth.login(client, request.password))
    }
}
