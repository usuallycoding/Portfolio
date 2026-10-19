package com.usuallycoding.portfolio.server.api

import com.usuallycoding.portfolio.ErrorDto
import com.usuallycoding.portfolio.server.domain.DomainError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException

private fun DomainError.status(): HttpStatusCode = when (this) {
    is DomainError.Invalid -> HttpStatusCode.BadRequest
    is DomainError.NotFound -> HttpStatusCode.NotFound
    is DomainError.PayloadTooLarge -> HttpStatusCode.PayloadTooLarge
    is DomainError.UnsupportedImage -> HttpStatusCode.UnsupportedMediaType
    is DomainError.TooManyAttempts -> HttpStatusCode.TooManyRequests
    is DomainError.Unauthorized -> HttpStatusCode.Unauthorized
}

fun Application.installErrorHandling() {
    install(StatusPages) {
        exception<DomainError> { call, e -> call.respond(e.status(), ErrorDto(e.message)) }
        exception<SerializationException> { call, _ -> call.respond(HttpStatusCode.BadRequest, ErrorDto("Malformed request body")) }
        exception<BadRequestException> { call, _ -> call.respond(HttpStatusCode.BadRequest, ErrorDto("Bad request")) }
    }
}
