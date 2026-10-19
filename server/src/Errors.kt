package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.ErrorDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException

/** An error with a client-facing message and the HTTP status to return. */
class ApiError(val status: HttpStatusCode, override val message: String) : RuntimeException(message)

fun badRequest(message: String): Nothing = throw ApiError(HttpStatusCode.BadRequest, message)
fun notFound(message: String = "Not found"): Nothing = throw ApiError(HttpStatusCode.NotFound, message)

fun Application.installErrorHandling() {
    install(StatusPages) {
        exception<ApiError> { call, e -> call.respond(e.status, ErrorDto(e.message)) }
        exception<SerializationException> { call, _ -> call.respond(HttpStatusCode.BadRequest, ErrorDto("Malformed request body")) }
        exception<BadRequestException> { call, _ -> call.respond(HttpStatusCode.BadRequest, ErrorDto("Bad request")) }
    }
}
