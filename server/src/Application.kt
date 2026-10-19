package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.server.api.installApi
import com.usuallycoding.portfolio.server.api.startImageCleanup
import com.usuallycoding.portfolio.server.config.ServerConfig
import com.usuallycoding.portfolio.server.di.serverModules
import com.usuallycoding.portfolio.server.domain.Clock
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.core.module.Module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.KoinIsolated

fun main() {
    val config = ServerConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        serverApplication(config)
        startImageCleanup(config, get())
    }.start(wait = true)
}

/**
 * The composition root: an isolated Koin for this application (closed, with the database, when it stops), then
 * the HTTP side built from what Koin provides. [overrides] replace any definition, which is how tests supply their own.
 */
fun Application.serverApplication(config: ServerConfig, clock: Clock = Clock.System, overrides: List<Module> = emptyList()) {
    install(KoinIsolated) { modules(serverModules(config, clock, overrides)) }
    installApi(config, get(), get(), get(), get(), get())
}
