package com.usuallycoding.portfolio.admin.di

import com.usuallycoding.portfolio.admin.data.AdminRepositoryImpl
import com.usuallycoding.portfolio.admin.data.AdminSession
import com.usuallycoding.portfolio.admin.domain.AdminRepository
import com.usuallycoding.portfolio.admin.presentation.LinksViewModel
import com.usuallycoding.portfolio.admin.presentation.ProfileViewModel
import com.usuallycoding.portfolio.admin.presentation.ProjectsViewModel
import com.usuallycoding.portfolio.ui.ThemeStore
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** What differs per device: where the server is and how to remember the theme. */
fun platformModule(serverUrl: String, themeStore: ThemeStore): Module = module {
    single { themeStore }
    single { AdminSession(serverUrl) }
    // Images load through the session's client, so staged and private images can be previewed with the owner's token.
    single<HttpClient> { get<AdminSession>().httpClient }
}

/**
 * A function, not a top-level value: a [Module] owns its singleton instances, so one shared value would hand every
 * Koin that loads it the same repository.
 */
fun adminModule(): Module = module {
    single<AdminRepository> { AdminRepositoryImpl(get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { LinksViewModel(get()) }
    viewModel { ProjectsViewModel(get()) }
}

/** [overrides] load last and win, so a test can swap any definition. */
fun adminModules(serverUrl: String, themeStore: ThemeStore, overrides: List<Module> = emptyList()): List<Module> =
    listOf(platformModule(serverUrl, themeStore), adminModule()) + overrides
