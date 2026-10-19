package com.usuallycoding.portfolio.server.api

import com.usuallycoding.portfolio.server.config.ServerConfig
import com.usuallycoding.portfolio.server.service.AuthService
import com.usuallycoding.portfolio.server.service.ImageService
import com.usuallycoding.portfolio.server.service.LinkService
import com.usuallycoding.portfolio.server.service.ProfileService
import com.usuallycoding.portfolio.server.service.ProjectService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

private const val OWNER = "owner"

/** The HTTP side: plugins and every route, each taking only the services it uses. */
fun Application.installApi(
    config: ServerConfig,
    auth: AuthService,
    images: ImageService,
    profile: ProfileService,
    links: LinkService,
    projects: ProjectService,
) {
    install(ContentNegotiation) { json() }
    installErrorHandling()
    installCors(config)
    install(Authentication) {
        bearer(ADMIN_AUTH) {
            authenticate { credential ->
                if (auth.isValid(credential.token)) UserIdPrincipal(OWNER) else null
            }
        }
    }

    routing {
        get("/") { call.respondText("Portfolio server") }

        publicImageRoutes(images, auth)
        publicProfileRoutes(profile)
        publicLinkRoutes(links)
        publicProjectRoutes(projects)

        loginRoute(auth)

        authenticate(ADMIN_AUTH) {
            get("/admin/session") { call.respond(HttpStatusCode.NoContent) }
            adminImageRoutes(images)
            adminProfileRoutes(profile)
            adminLinkRoutes(links)
            adminProjectRoutes(projects)
        }
    }
}

private fun Application.installCors(config: ServerConfig) {
    install(CORS) {
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        val origins = config.corsOrigins
        if (origins == null) {
            anyHost()
        } else {
            origins.forEach { origin ->
                val scheme = origin.substringBefore("://", missingDelimiterValue = "http")
                allowHost(origin.substringAfter("://"), schemes = listOf(scheme))
            }
        }
    }
}
