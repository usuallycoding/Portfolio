package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.ApiPaths
import com.usuallycoding.portfolio.ErrorDto
import com.usuallycoding.portfolio.LoginRequest
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
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey

const val ADMIN_AUTH = "admin"

/** Everything the routes need, created once per server (or per test). */
class ServerContext(
    val config: ServerConfig,
    val database: Database,
    val clock: Clock,
    val auth: OwnerAuth,
    val throttle: LoginThrottle,
    val images: ImageStore,
    val cleanup: ImageCleanup,
)

val ServerContextKey = AttributeKey<ServerContext>("ServerContext")

fun createContext(config: ServerConfig, clock: Clock = Clock.System): ServerContext {
    val database = Database(config.databasePath)
    val images = ImageStore(config, database, clock)
    return ServerContext(
        config = config,
        database = database,
        clock = clock,
        auth = OwnerAuth(config.ownerPassword, database, clock, config.tokenTtl),
        throttle = LoginThrottle(clock),
        images = images,
        cleanup = ImageCleanup(config, database, images, clock),
    )
}

fun main() {
    val config = ServerConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        val context = createContext(config)
        module(context)
        startImageCleanup(context)
    }.start(wait = true)
}

fun Application.module(context: ServerContext) {
    attributes.put(ServerContextKey, context)

    install(ContentNegotiation) { json() }
    installErrorHandling()
    installCors(context.config)
    install(Authentication) {
        bearer(ADMIN_AUTH) {
            authenticate { credential ->
                if (context.auth.isValid(credential.token)) UserIdPrincipal(OWNER) else null
            }
        }
    }

    routing {
        get("/") { call.respondText("Portfolio server") }

        val repos = Repositories(context.config)
        publicImageRoutes(context)
        publicContentRoutes(context, repos)

        post(ApiPaths.ADMIN_LOGIN) {
            val key = call.request.origin.remoteHost
            if (context.throttle.isBlocked(key)) {
                return@post call.respond(HttpStatusCode.TooManyRequests, ErrorDto("Too many failed attempts"))
            }
            val request = call.receive<LoginRequest>()
            if (context.auth.passwordMatches(request.password)) {
                context.throttle.reset(key)
                call.respond(context.auth.issueToken())
            } else {
                context.throttle.recordFailure(key)
                call.respond(HttpStatusCode.Unauthorized, ErrorDto("Invalid password"))
            }
        }

        authenticate(ADMIN_AUTH) {
            get("/admin/session") { call.respond(HttpStatusCode.NoContent) }
            adminImageRoutes(context)
            adminContentRoutes(context, repos)
        }
    }
}

private const val OWNER = "owner"

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
