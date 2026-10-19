package com.usuallycoding.portfolio.server.api

import com.usuallycoding.portfolio.ApiPaths
import com.usuallycoding.portfolio.OrderRequest
import com.usuallycoding.portfolio.SocialLinkInput
import com.usuallycoding.portfolio.server.service.LinkService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

fun Route.publicLinkRoutes(links: LinkService) {
    get(ApiPaths.LINKS) { call.respond(links.list(publicOnly = true)) }
}

/** Must be mounted inside the admin `authenticate` block. */
fun Route.adminLinkRoutes(links: LinkService) {
    get(ApiPaths.ADMIN_LINKS) { call.respond(links.list(publicOnly = false)) }
    post(ApiPaths.ADMIN_LINKS) { call.respond(HttpStatusCode.Created, links.create(call.receive<SocialLinkInput>())) }
    put("${ApiPaths.ADMIN_LINKS}/order") { call.respond(links.reorder(call.receive<OrderRequest>().ids)) }
    put("${ApiPaths.ADMIN_LINKS}/{id}") {
        val id = call.idParam()
        call.respond(links.update(id, call.receive<SocialLinkInput>()))
    }
    delete("${ApiPaths.ADMIN_LINKS}/{id}") {
        links.delete(call.idParam())
        call.respond(HttpStatusCode.NoContent)
    }
}
