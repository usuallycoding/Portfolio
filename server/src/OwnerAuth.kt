package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.LoginResponse
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Single-owner authentication. The configured password is hashed in memory at construction
 * and never stored. Tokens are random, and only their SHA-256 hash is persisted.
 */
class OwnerAuth(
    password: String,
    private val database: Database,
    private val clock: Clock,
    private val tokenTtl: Duration,
) {
    private val random = SecureRandom()
    private val salt = ByteArray(16).also(random::nextBytes)
    private val passwordHash = hashPassword(password)

    private fun hashPassword(password: String): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password.toCharArray(), salt, 120_000, 256))
            .encoded

    fun passwordMatches(candidate: String): Boolean =
        MessageDigest.isEqual(passwordHash, hashPassword(candidate))

    suspend fun issueToken(): LoginResponse {
        val token = ByteArray(32).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
        val expiresAt = clock.now().plus(tokenTtl).epochSecond
        val now = clock.now().epochSecond
        database.transaction { conn ->
            conn.prepareStatement("DELETE FROM auth_tokens WHERE expires_at <= ?").use {
                it.setLong(1, now)
                it.executeUpdate()
            }
            conn.prepareStatement("INSERT INTO auth_tokens(token_hash, expires_at) VALUES (?, ?)").use {
                it.setString(1, sha256(token))
                it.setLong(2, expiresAt)
                it.executeUpdate()
            }
        }
        return LoginResponse(token, expiresAt)
    }

    suspend fun isValid(token: String): Boolean {
        val now = clock.now().epochSecond
        return database.transaction { conn ->
            conn.prepareStatement("SELECT 1 FROM auth_tokens WHERE token_hash = ? AND expires_at > ?").use {
                it.setString(1, sha256(token))
                it.setLong(2, now)
                it.executeQuery().use { rs -> rs.next() }
            }
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

/** Blocks a client key after too many failures within a sliding window. */
class LoginThrottle(
    private val clock: Clock,
    private val maxFailures: Int = 5,
    private val window: Duration = Duration.ofMinutes(10),
) {
    private val failures = HashMap<String, ArrayDeque<Long>>()

    @Synchronized
    fun isBlocked(key: String): Boolean = trimmed(key)?.let { it.size >= maxFailures } ?: false

    @Synchronized
    fun recordFailure(key: String) {
        trimmed(key)
        failures.getOrPut(key) { ArrayDeque() }.addLast(clock.now().epochSecond)
    }

    @Synchronized
    fun reset(key: String) {
        failures.remove(key)
    }

    /** Drops failures older than the window; returns the remaining ones, or null (and forgets the key) if none. */
    private fun trimmed(key: String): ArrayDeque<Long>? {
        val queue = failures[key] ?: return null
        val cutoff = clock.now().minus(window).epochSecond
        while (queue.isNotEmpty() && queue.first() <= cutoff) queue.removeFirst()
        if (queue.isEmpty()) failures.remove(key)
        return queue.takeIf { it.isNotEmpty() }
    }
}
