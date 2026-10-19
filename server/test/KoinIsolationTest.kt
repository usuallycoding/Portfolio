package com.usuallycoding.portfolio.server

import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.KoinIsolated
import org.koin.mp.KoinPlatform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the server relies on from Koin: one instance per application, closed when the application stops. */
class KoinIsolationTest {
    private class Resource(val name: String) {
        var closed = false
    }

    private fun run(name: String, onStarted: (Resource) -> Unit = {}): Resource {
        lateinit var resource: Resource
        testApplication {
            application {
                // A function creates a fresh module each time; a shared top-level module would share its singletons.
                install(KoinIsolated) {
                    modules(module { single { Resource(name) } onClose { it?.closed = true } })
                }
                resource = getKoin().get()
                onStarted(resource)
            }
            startApplication()
        }
        return resource
    }

    @Test
    fun eachApplicationHasItsOwnInstances() {
        val first = run("first")
        val second = run("second")
        assertEquals("first", first.name)
        assertEquals("second", second.name)
        assertNotSame(first, second)
    }

    @Test
    fun noGlobalKoinIsStarted() {
        run("only")
        assertNull(KoinPlatform.getKoinOrNull())
    }

    @Test
    fun singletonsAreClosedWhenTheApplicationStops() {
        var closedWhileRunning = true
        val resource = run("closing") { closedWhileRunning = it.closed }
        assertEquals(false, closedWhileRunning)
        assertTrue(resource.closed, "onClose should run when the application stops")
    }
}
