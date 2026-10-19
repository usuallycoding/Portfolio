package com.usuallycoding.portfolio.server.api

import com.usuallycoding.portfolio.ApiPaths
import com.usuallycoding.portfolio.ProfileInput
import com.usuallycoding.portfolio.server.service.ProfileService
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put

fun Route.publicProfileRoutes(profile: ProfileService) {
    get(ApiPaths.PROFILE) { call.respond(profile.get()) }
}

/** Must be mounted inside the admin `authenticate` block. */
fun Route.adminProfileRoutes(profile: ProfileService) {
    get(ApiPaths.ADMIN_PROFILE) { call.respond(profile.get()) }
    put(ApiPaths.ADMIN_PROFILE) { call.respond(profile.update(call.receive<ProfileInput>())) }
}
