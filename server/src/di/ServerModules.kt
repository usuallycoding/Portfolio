package com.usuallycoding.portfolio.server.di

import com.usuallycoding.portfolio.server.config.ServerConfig
import com.usuallycoding.portfolio.server.data.AwtImageProcessor
import com.usuallycoding.portfolio.server.data.Database
import com.usuallycoding.portfolio.server.data.FileImageStorage
import com.usuallycoding.portfolio.server.data.Pbkdf2PasswordVerifier
import com.usuallycoding.portfolio.server.data.SqliteTransactor
import com.usuallycoding.portfolio.server.domain.ImageFiles
import com.usuallycoding.portfolio.server.domain.ImageLimits
import com.usuallycoding.portfolio.server.domain.ImageProcessor
import com.usuallycoding.portfolio.server.domain.Transactor
import com.usuallycoding.portfolio.server.service.ImageCleanupService
import com.usuallycoding.portfolio.server.service.ImageService
import com.usuallycoding.portfolio.server.service.LinkService
import com.usuallycoding.portfolio.server.service.ProfileService
import com.usuallycoding.portfolio.server.service.ProjectService
import com.usuallycoding.portfolio.server.data.SqliteAuthTokenRepository
import com.usuallycoding.portfolio.server.domain.AuthTokenRepository
import com.usuallycoding.portfolio.server.domain.Clock
import com.usuallycoding.portfolio.server.domain.PasswordVerifier
import com.usuallycoding.portfolio.server.service.AuthService
import com.usuallycoding.portfolio.server.service.LoginThrottle
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

/** What a run supplies from outside: the settings and the clock. */
fun platformModule(config: ServerConfig, clock: Clock): Module = module {
    single { config }
    single { clock }
}

/**
 * Every definition is built by a function call, never held in a top-level value: a Koin [Module] owns its
 * singleton instances, so a shared value would hand every server the same database.
 */
fun serverModule(): Module = module {
    single { Database(get<ServerConfig>().databasePath) } onClose { it?.close() }
    single<PasswordVerifier> { Pbkdf2PasswordVerifier(get<ServerConfig>().ownerPassword) }
    single<AuthTokenRepository> { SqliteAuthTokenRepository(get()) }
    single { LoginThrottle(get()) }
    single { AuthService(get(), get(), get(), get(), get<ServerConfig>().tokenTtl) }
    single<Transactor> { SqliteTransactor(get()) }
    single<ImageFiles> { FileImageStorage(get<ServerConfig>().imagesDir) }
    single<ImageProcessor> { AwtImageProcessor(get<ServerConfig>().thumbnailEdge) }
    single {
        val config = get<ServerConfig>()
        ImageLimits(config.maxUploadBytes, config.maxImageEdge, config.maxImagePixels)
    }
    single { ImageService(get(), get(), get(), get(), get()) }
    single { ImageCleanupService(get(), get(), get(), get<ServerConfig>().orphanGrace) }
    single { ProfileService(get(), get<ServerConfig>().ratioTolerance) }
    single { LinkService(get(), get<ServerConfig>().ratioTolerance) }
    single { ProjectService(get(), get<ServerConfig>().ratioTolerance) }
}

/** [overrides] load last and win, so a test can swap any definition. */
fun serverModules(config: ServerConfig, clock: Clock, overrides: List<Module> = emptyList()): List<Module> =
    listOf(platformModule(config, clock), serverModule()) + overrides
