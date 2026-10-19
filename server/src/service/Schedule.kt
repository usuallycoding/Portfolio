package com.usuallycoding.portfolio.server.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration

/**
 * Calls [action] after [initialDelay] and then every [interval], until cancelled.
 * A failing run is reported to [onError] and does not stop the schedule.
 */
suspend fun runPeriodically(
    initialDelay: Duration,
    interval: Duration,
    onError: (Exception) -> Unit,
    action: suspend () -> Unit,
) {
    delay(initialDelay.toMillis())
    while (currentCoroutineContext().isActive) {
        try {
            action()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onError(e)
        }
        delay(interval.toMillis())
    }
}
