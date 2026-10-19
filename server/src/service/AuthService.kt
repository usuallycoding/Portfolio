package com.usuallycoding.portfolio.server.service

import com.usuallycoding.portfolio.LoginResponse
import com.usuallycoding.portfolio.server.domain.AuthTokenRepository
import com.usuallycoding.portfolio.server.domain.Clock
import com.usuallycoding.portfolio.server.domain.DomainError
import com.usuallycoding.portfolio.server.domain.PasswordVerifier
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64

/**
 * Single-owner authentication. Tokens are random, and only their SHA-256 hash is persisted. Repeated failed
 * logins from one client are throttled.
 */
class AuthService(
    private val password: PasswordVerifier,
    private val tokens: AuthTokenRepository,
    private val throttle: LoginThrottle,
    private val clock: Clock,
    private val tokenTtl: Duration,
) {
    private val random = SecureRandom()

    /** Throws [DomainError.TooManyAttempts] while [client] is blocked, even for the right password. */
    fun checkNotBlocked(client: String) {
        if (throttle.isBlocked(client)) throw DomainError.TooManyAttempts()
    }

    /** Throws [DomainError.Unauthorized] for a wrong password; a right one clears [client]'s failures. */
    suspend fun login(client: String, candidate: String): LoginResponse {
        if (!password.matches(candidate)) {
            throttle.recordFailure(client)
            throw DomainError.Unauthorized("Invalid password")
        }
        throttle.reset(client)
        return issueToken()
    }

    suspend fun isValid(token: String): Boolean = tokens.isValid(sha256(token), clock.now().epochSecond)

    private suspend fun issueToken(): LoginResponse {
        val token = ByteArray(32).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
        val now = clock.now().epochSecond
        val expiresAt = clock.now().plus(tokenTtl).epochSecond
        tokens.replaceExpiredWith(sha256(token), expiresAt, now)
        return LoginResponse(token, expiresAt)
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
