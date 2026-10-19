package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.admin.data.AdminSession
import com.usuallycoding.portfolio.admin.di.adminModules
import com.usuallycoding.portfolio.admin.domain.AdminRepository
import com.usuallycoding.portfolio.admin.presentation.LinksViewModel
import com.usuallycoding.portfolio.admin.presentation.ProfileViewModel
import com.usuallycoding.portfolio.admin.presentation.ProjectsViewModel
import com.usuallycoding.portfolio.ui.ThemePreference
import com.usuallycoding.portfolio.ui.ThemeStore
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class AdminModuleTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private object MemoryStore : ThemeStore {
        override fun load() = ThemePreference.System
        override fun save(preference: ThemePreference) {}
    }

    private fun koin(url: String = "http://server", overrides: List<org.koin.core.module.Module> = emptyList()) =
        koinApplication { modules(adminModules(url, MemoryStore, overrides)) }.koin

    @Test
    fun imagesLoadThroughTheSessionsClient() {
        val koin = koin()
        assertSame(koin.get<AdminSession>().httpClient, koin.get<HttpClient>())
        assertEquals("http://server/images/a.jpg", koin.get<AdminRepository>().imageUrl("a.jpg", thumbnail = false))
    }

    @Test
    fun theSectionViewModelsAreBuilt() {
        val koin = koin()
        koin.get<ProfileViewModel>()
        koin.get<LinksViewModel>()
        koin.get<ProjectsViewModel>()
    }

    @Test
    fun overridesReplaceDefinitions() {
        val session = testSession { json("{}") }
        val koin = koin(overrides = listOf(module { single { session } }))
        assertSame(session, koin.get<AdminSession>())
    }

    @Test
    fun eachKoinIsIsolatedFromTheOthers() {
        assertNotSame(koin("http://one").get<AdminRepository>(), koin("http://two").get<AdminRepository>())
    }
}
