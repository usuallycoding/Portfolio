package com.usuallycoding.portfolio.server.data

import com.usuallycoding.portfolio.server.domain.PasswordVerifier
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Hashes the configured password in memory at construction (random salt, never stored) and compares in constant time. */
class Pbkdf2PasswordVerifier(password: String) : PasswordVerifier {
    private val salt = ByteArray(16).also(SecureRandom()::nextBytes)
    private val passwordHash = hash(password)

    private fun hash(password: String): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password.toCharArray(), salt, 120_000, 256))
            .encoded

    override fun matches(candidate: String): Boolean = MessageDigest.isEqual(passwordHash, hash(candidate))
}
