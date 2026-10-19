package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.ApiPaths
import com.usuallycoding.portfolio.OrderRequest
import com.usuallycoding.portfolio.ProfileInput
import com.usuallycoding.portfolio.ProjectInput
import com.usuallycoding.portfolio.SocialLinkInput
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

private fun RoutingCall.idParam(): Long = parameters["id"]?.toLongOrNull() ?: notFound()

/** Read-only routes for visitors: only public content is returned. */
fun Route.publicContentRoutes(context: ServerContext, repos: Repositories) {
    val db = context.database

    get(ApiPaths.PROFILE) { call.respond(db.transaction { repos.profile.get(it) }) }
    get(ApiPaths.LINKS) { call.respond(db.transaction { repos.links.list(it, publicOnly = true) }) }
    get(ApiPaths.PROJECTS) { call.respond(db.transaction { repos.projects.list(it, publicOnly = true) }) }
    get("${ApiPaths.PROJECTS}/{id}") {
        val id = call.idParam()
        call.respond(db.transaction { repos.projects.get(it, id, publicOnly = true) } ?: notFound("Project not found"))
    }
}

/** Owner-only routes. Must be mounted inside the admin `authenticate` block. */
fun Route.adminContentRoutes(context: ServerContext, repos: Repositories) {
    val db = context.database

    get(ApiPaths.ADMIN_PROFILE) { call.respond(db.transaction { repos.profile.get(it) }) }
    put(ApiPaths.ADMIN_PROFILE) {
        val input = call.receive<ProfileInput>()
        call.respond(db.transaction { repos.profile.update(it, input) })
    }

    get(ApiPaths.ADMIN_LINKS) { call.respond(db.transaction { repos.links.list(it, publicOnly = false) }) }
    post(ApiPaths.ADMIN_LINKS) {
        val input = call.receive<SocialLinkInput>()
        call.respond(HttpStatusCode.Created, db.transaction { repos.links.create(it, input) })
    }
    put("${ApiPaths.ADMIN_LINKS}/order") {
        val order = call.receive<OrderRequest>()
        call.respond(db.transaction { repos.links.reorder(it, order.ids) })
    }
    put("${ApiPaths.ADMIN_LINKS}/{id}") {
        val id = call.idParam()
        val input = call.receive<SocialLinkInput>()
        call.respond(db.transaction { repos.links.update(it, id, input) })
    }
    delete("${ApiPaths.ADMIN_LINKS}/{id}") {
        val id = call.idParam()
        db.transaction { repos.links.delete(it, id) }
        call.respond(HttpStatusCode.NoContent)
    }

    get(ApiPaths.ADMIN_PROJECTS) { call.respond(db.transaction { repos.projects.list(it, publicOnly = false) }) }
    post(ApiPaths.ADMIN_PROJECTS) {
        val input = call.receive<ProjectInput>()
        call.respond(HttpStatusCode.Created, db.transaction { repos.projects.create(it, input) })
    }
    put("${ApiPaths.ADMIN_PROJECTS}/order") {
        val order = call.receive<OrderRequest>()
        call.respond(db.transaction { repos.projects.reorder(it, order.ids) })
    }
    get("${ApiPaths.ADMIN_PROJECTS}/{id}") {
        val id = call.idParam()
        call.respond(db.transaction { repos.projects.get(it, id, publicOnly = false) } ?: notFound("Project not found"))
    }
    put("${ApiPaths.ADMIN_PROJECTS}/{id}") {
        val id = call.idParam()
        val input = call.receive<ProjectInput>()
        call.respond(db.transaction { repos.projects.update(it, id, input) })
    }
    delete("${ApiPaths.ADMIN_PROJECTS}/{id}") {
        val id = call.idParam()
        db.transaction { repos.projects.delete(it, id) }
        call.respond(HttpStatusCode.NoContent)
    }
}

class Repositories(config: ServerConfig) {
    val profile = ProfileRepository(config)
    val links = LinkRepository(config)
    val projects = ProjectRepository(config)
}
