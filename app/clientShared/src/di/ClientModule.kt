package com.usuallycoding.portfolio.client.di

import com.usuallycoding.portfolio.PortfolioApi
import com.usuallycoding.portfolio.client.data.PortfolioRepositoryImpl
import com.usuallycoding.portfolio.client.domain.PortfolioRepository
import com.usuallycoding.portfolio.client.presentation.ClientPlatform
import com.usuallycoding.portfolio.client.presentation.HomeViewModel
import com.usuallycoding.portfolio.client.presentation.ProjectViewModel
import com.usuallycoding.portfolio.ui.ThemeStore
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** What differs per device: where the server is, which kind of device this is, how to remember the theme. */
fun platformModule(serverUrl: String, platform: ClientPlatform, themeStore: ThemeStore, httpClient: HttpClient): Module =
    module {
        single { platform }
        single { themeStore }
        single { httpClient }
        single { PortfolioApi(serverUrl, get()) }
    }

/**
 * A function, not a top-level value: a [Module] owns its singleton instances, so one shared value would hand every
 * Koin that loads it the same repository (and the first one's HTTP client).
 */
fun clientModule(): Module = module {
    single<PortfolioRepository> { PortfolioRepositoryImpl(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { params -> ProjectViewModel(get(), params.get()) }
}

fun clientModules(serverUrl: String, platform: ClientPlatform, themeStore: ThemeStore, httpClient: HttpClient): List<Module> =
    listOf(platformModule(serverUrl, platform, themeStore, httpClient), clientModule())
