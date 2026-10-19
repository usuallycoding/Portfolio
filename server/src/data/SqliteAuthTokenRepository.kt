package com.usuallycoding.portfolio.server.data

import com.usuallycoding.portfolio.server.domain.AuthTokenRepository

class SqliteAuthTokenRepository(private val database: Database) : AuthTokenRepository {
    override suspend fun replaceExpiredWith(tokenHash: String, expiresAt: Long, now: Long) {
        database.transaction { conn ->
            conn.prepareStatement("DELETE FROM auth_tokens WHERE expires_at <= ?").use {
                it.setLong(1, now)
                it.executeUpdate()
            }
            conn.prepareStatement("INSERT INTO auth_tokens(token_hash, expires_at) VALUES (?, ?)").use {
                it.setString(1, tokenHash)
                it.setLong(2, expiresAt)
                it.executeUpdate()
            }
        }
    }

    override suspend fun isValid(tokenHash: String, now: Long): Boolean =
        database.transaction { conn ->
            conn.prepareStatement("SELECT 1 FROM auth_tokens WHERE token_hash = ? AND expires_at > ?").use {
                it.setString(1, tokenHash)
                it.setLong(2, now)
                it.executeQuery().use { rs -> rs.next() }
            }
        }
}
