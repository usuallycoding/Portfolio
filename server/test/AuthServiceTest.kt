package com.usuallycoding.portfolio.server.service

import com.usuallycoding.portfolio.server.MutableClock
import com.usuallycoding.portfolio.server.domain.AuthTokenRepository
import com.usuallycoding.portfolio.server.domain.DomainError
import com.usuallycoding.portfolio.server.domain.PasswordVerifier
import kotlinx.coroutines.test.runTest
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthServiceTest {
    /** Keeps what it is given in memory, so the tests can look at exactly what would be stored. */
    private class FakeTokens : AuthTokenRepository {
        val stored = mutableMapOf<String, Long>()
        override suspend fun replaceExpiredWith(tokenHash: String, expiresAt: Long, now: Long) {
            stored.values.removeAll { it <= now }
            stored[tokenHash] = expiresAt
        }
        override suspend fun isValid(tokenHash: String, now: Long) = stored[tokenHash]?.let { it > now } ?: false
    }

    private val clock = MutableClock()
    private val tokens = FakeTokens()
    private val ttl = Duration.ofDays(7)
    private val service = AuthService(PasswordVerifier { it == "secret" }, tokens, LoginThrottle(clock), clock, ttl)

    @Test
    fun theRightPasswordIssuesATokenThatIsValidUntilItExpires() = runTest {
        val login = service.login("c", "secret")
        assertTrue(service.isValid(login.token))
        assertEquals(clock.now().plus(ttl).epochSecond, login.expiresAtEpochSeconds)
        clock.advance(ttl.plusSeconds(1))
        assertFalse(service.isValid(login.token))
    }

    @Test
    fun aWrongPasswordIsUnauthorizedAndIssuesNothing() = runTest {
        assertFailsWith<DomainError.Unauthorized> { service.login("c", "nope") }
        assertTrue(tokens.stored.isEmpty())
    }

    @Test
    fun unknownTokensAreInvalid() = runTest {
        assertFalse(service.isValid("made-up"))
    }

    @Test
    fun onlyTheHashOfATokenIsStored() = runTest {
        val login = service.login("c", "secret")
        assertFalse(login.token in tokens.stored.keys)
        assertEquals(1, tokens.stored.size)
    }

    @Test
    fun everyLoginGetsADifferentToken() = runTest {
        assertNotEquals(service.login("c", "secret").token, service.login("c", "secret").token)
    }

    @Test
    fun expiredTokensAreForgottenWhenTheNextOneIsIssued() = runTest {
        service.login("c", "secret")
        clock.advance(ttl.plusSeconds(1))
        service.login("c", "secret")
        assertEquals(1, tokens.stored.size)
    }

    @Test
    fun fiveFailuresBlockAClientEvenForTheRightPasswordUntilTheWindowPasses() = runTest {
        repeat(5) { assertFailsWith<DomainError.Unauthorized> { service.login("c", "nope") } }
        assertFailsWith<DomainError.TooManyAttempts> { service.checkNotBlocked("c") }
        service.checkNotBlocked("other")
        clock.advance(Duration.ofMinutes(11))
        service.checkNotBlocked("c")
        assertTrue(service.isValid(service.login("c", "secret").token))
    }

    @Test
    fun aSuccessfulLoginClearsEarlierFailures() = runTest {
        repeat(4) { assertFailsWith<DomainError.Unauthorized> { service.login("c", "nope") } }
        service.login("c", "secret")
        repeat(4) { assertFailsWith<DomainError.Unauthorized> { service.login("c", "nope") } }
        service.checkNotBlocked("c")
    }
}
