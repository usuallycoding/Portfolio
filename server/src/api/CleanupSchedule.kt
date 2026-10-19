package com.usuallycoding.portfolio.server.api

import com.usuallycoding.portfolio.server.config.ServerConfig
import com.usuallycoding.portfolio.server.service.ImageCleanupService
import com.usuallycoding.portfolio.server.service.runPeriodically
import io.ktor.server.application.Application
import io.ktor.server.application.log
import kotlinx.coroutines.launch

/** Runs the cleanup once a day for as long as the application is running. */
fun Application.startImageCleanup(config: ServerConfig, cleanup: ImageCleanupService) {
    launch {
        runPeriodically(
            initialDelay = config.cleanupInitialDelay,
            interval = config.cleanupInterval,
            onError = { log.error("Image cleanup failed", it) },
        ) {
            val result = cleanup.run()
            log.info("Image cleanup removed ${result.orphanImages} orphan image(s) and ${result.strayFiles} stray file(s)")
        }
    }
}
