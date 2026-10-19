package com.usuallycoding.portfolio.server

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CleanupScheduleTest {
    private val day = Duration.ofDays(1)

    @Test
    fun runsOncePerIntervalAfterTheInitialDelay() = runTest {
        var runs = 0
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            runPeriodically(Duration.ofMinutes(5), day, onError = {}) { runs++ }
        }
        advanceTimeBy(Duration.ofMinutes(4).toMillis()); runCurrent()
        assertEquals(0, runs, "must not run before the initial delay")

        advanceTimeBy(Duration.ofMinutes(2).toMillis()); runCurrent()
        assertEquals(1, runs)

        advanceTimeBy(Duration.ofHours(23).toMillis()); runCurrent()
        assertEquals(1, runs, "must not run again within a day")

        advanceTimeBy(Duration.ofHours(2).toMillis()); runCurrent()
        assertEquals(2, runs)
        job.cancel()
    }

    @Test
    fun aFailingRunIsReportedAndTheScheduleContinues() = runTest {
        var runs = 0
        val errors = mutableListOf<Exception>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            runPeriodically(Duration.ZERO, day, onError = { errors += it }) {
                runs++
                if (runs == 1) error("disk on fire")
            }
        }
        runCurrent()
        assertEquals(1, errors.size)
        advanceTimeBy(day.toMillis() + 1); runCurrent()
        assertEquals(2, runs)
        assertEquals(1, errors.size)
        job.cancel()
    }
}
