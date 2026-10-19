package com.usuallycoding.portfolio.server.domain

/**
 * Stores the owner's login tokens, as hashes only. Each call is its own atomic unit, since tokens are never
 * changed together with other data.
 */
interface AuthTokenRepository {
    /** Forgets tokens that expired at or before [now], then remembers [tokenHash] until [expiresAt]. */
    suspend fun replaceExpiredWith(tokenHash: String, expiresAt: Long, now: Long)

    /** True when [tokenHash] is stored and has not expired as of [now]. */
    suspend fun isValid(tokenHash: String, now: Long): Boolean
}
