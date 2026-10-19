package com.usuallycoding.portfolio.client

import com.usuallycoding.portfolio.client.di.clientModules
import com.usuallycoding.portfolio.client.domain.PortfolioRepository
import com.usuallycoding.portfolio.client.presentation.ClientPlatform
import com.usuallycoding.portfolio.client.presentation.HomeViewModel
import com.usuallycoding.portfolio.client.presentation.ProjectViewModel
import com.usuallycoding.portfolio.ui.ThemePreference
import com.usuallycoding.portfolio.ui.ThemeStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class ClientModuleTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private object MemoryStore : ThemeStore {
        override fun load() = ThemePreference.System
        override fun save(preference: ThemePreference) {}
    }

    private fun koin(url: String = "http://server") = koinApplication {
        val client = HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) })
        modules(clientModules(url, ClientPlatform.Mobile, MemoryStore, client))
    }.koin

    @Test
    fun theModulesProvideWhatTheShellsPassIn() {
        val koin = koin()
        assertEquals(ClientPlatform.Mobile, koin.get<ClientPlatform>())
        assertSame(MemoryStore, koin.get<ThemeStore>())
        assertEquals("http://server/images/a.jpg", koin.get<PortfolioRepository>().imageUrl("a.jpg", thumbnail = false))
    }

    @Test
    fun viewModelsAreBuiltOverTheSharedRepository() {
        val koin = koin()
        koin.get<HomeViewModel>()
        koin.get<ProjectViewModel> { parametersOf(7L) }
        assertSame(koin.get<PortfolioRepository>(), koin.get<PortfolioRepository>())
    }

    @Test
    fun eachKoinIsIsolatedFromTheOthers() {
        val first = koin("http://one")
        val second = koin("http://two")
        assertNotSame(first.get<PortfolioRepository>(), second.get<PortfolioRepository>())
        assertEquals("http://two/images/a.jpg", second.get<PortfolioRepository>().imageUrl("a.jpg", false))
    }
}
