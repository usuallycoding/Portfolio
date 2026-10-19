package com.usuallycoding.portfolio.server.service

import com.usuallycoding.portfolio.server.domain.Clock
import java.time.Duration

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
