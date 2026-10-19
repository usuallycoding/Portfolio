package com.usuallycoding.portfolio.server.domain

/** A failure the caller can understand. It carries no HTTP detail; the api layer decides how to report it. */
sealed class DomainError(override val message: String) : RuntimeException(message) {
    /** The input breaks a rule (wrong shape, too long, unknown reference). */
    class Invalid(message: String) : DomainError(message)

    class NotFound(message: String = "Not found") : DomainError(message)

    class PayloadTooLarge(message: String) : DomainError(message)

    /** The upload is not an image this server accepts. */
    class UnsupportedImage(message: String) : DomainError(message)

    class TooManyAttempts(message: String = "Too many failed attempts") : DomainError(message)

    class Unauthorized(message: String = "Unauthorized") : DomainError(message)
}

fun invalid(message: String): Nothing = throw DomainError.Invalid(message)
fun notFound(message: String = "Not found"): Nothing = throw DomainError.NotFound(message)
