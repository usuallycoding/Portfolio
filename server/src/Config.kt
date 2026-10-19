package com.usuallycoding.portfolio.server

import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

data class ServerConfig(
    val ownerPassword: String,
    val databasePath: Path = Paths.get("data", "portfolio.db"),
    val imagesDir: Path = Paths.get("data", "images"),
    val port: Int = 8090,
    val tokenTtl: Duration = Duration.ofDays(7),
    val maxUploadBytes: Long = 15L * 1024 * 1024,
    val maxImagePixels: Long = 40_000_000L,
    val maxImageEdge: Int = 8_000,
    val thumbnailEdge: Int = 400,
    /** Allowed relative deviation from a required aspect ratio, e.g. 0.02 = 2%. */
    val ratioTolerance: Double = 0.02,
    /** Unreferenced images younger than this are kept, so uploads in progress are never collected. */
    val orphanGrace: Duration = Duration.ofMinutes(60),
    val cleanupInterval: Duration = Duration.ofDays(1),
    val cleanupInitialDelay: Duration = Duration.ofMinutes(5),
    /** Browser origins allowed to call the API (e.g. `http://localhost:8081`), or null to allow any. Auth is by bearer token, not cookies. */
    val corsOrigins: List<String>? = null,
) {
    companion object {
        /**
         * Reads configuration from environment variables. `PORTFOLIO_PASSWORD` is required;
         * the server refuses to start without it.
         */
        fun fromEnvironment(env: Map<String, String> = System.getenv()): ServerConfig {
            val password = env["PORTFOLIO_PASSWORD"]
            check(!password.isNullOrBlank()) {
                "PORTFOLIO_PASSWORD is not set. Set it to the owner's admin password before starting the server."
            }
            val defaults = ServerConfig(ownerPassword = password)
            return defaults.copy(
                databasePath = env["PORTFOLIO_DB"]?.let { Paths.get(it) } ?: defaults.databasePath,
                imagesDir = env["PORTFOLIO_IMAGES"]?.let { Paths.get(it) } ?: defaults.imagesDir,
                port = env["PORTFOLIO_PORT"]?.toInt() ?: defaults.port,
                corsOrigins = env["PORTFOLIO_CORS_ORIGINS"]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() },
            )
        }
    }
}
