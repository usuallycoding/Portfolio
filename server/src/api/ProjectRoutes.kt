package com.usuallycoding.portfolio.server.api

import com.usuallycoding.portfolio.ApiPaths
import com.usuallycoding.portfolio.OrderRequest
import com.usuallycoding.portfolio.ProjectInput
import com.usuallycoding.portfolio.server.service.ProjectService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

fun Route.publicProjectRoutes(projects: ProjectService) {
    get(ApiPaths.PROJECTS) { call.respond(projects.list(publicOnly = true)) }
    get("${ApiPaths.PROJECTS}/{id}") { call.respond(projects.get(call.idParam(), publicOnly = true)) }
}

/** Must be mounted inside the admin `authenticate` block. */
fun Route.adminProjectRoutes(projects: ProjectService) {
    get(ApiPaths.ADMIN_PROJECTS) { call.respond(projects.list(publicOnly = false)) }
    post(ApiPaths.ADMIN_PROJECTS) { call.respond(HttpStatusCode.Created, projects.create(call.receive<ProjectInput>())) }
    put("${ApiPaths.ADMIN_PROJECTS}/order") { call.respond(projects.reorder(call.receive<OrderRequest>().ids)) }
    get("${ApiPaths.ADMIN_PROJECTS}/{id}") { call.respond(projects.get(call.idParam(), publicOnly = false)) }
    put("${ApiPaths.ADMIN_PROJECTS}/{id}") {
        val id = call.idParam()
        call.respond(projects.update(id, call.receive<ProjectInput>()))
    }
    delete("${ApiPaths.ADMIN_PROJECTS}/{id}") {
        projects.delete(call.idParam())
        call.respond(HttpStatusCode.NoContent)
    }
}
