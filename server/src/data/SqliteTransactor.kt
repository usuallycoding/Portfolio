package com.usuallycoding.portfolio.server.data

import com.usuallycoding.portfolio.server.domain.ImageRepository
import com.usuallycoding.portfolio.server.domain.LinkRepository
import com.usuallycoding.portfolio.server.domain.ProfileRepository
import com.usuallycoding.portfolio.server.domain.ProjectRepository
import com.usuallycoding.portfolio.server.domain.Repositories
import com.usuallycoding.portfolio.server.domain.Transactor
import java.sql.Connection

/** Each unit of work gets repositories bound to the one connection, inside the database's transaction and lock. */
class SqliteTransactor(private val database: Database) : Transactor {
    override suspend fun <T> transaction(block: Repositories.() -> T): T =
        database.transaction { conn -> SqliteRepositories(conn).block() }
}

private class SqliteRepositories(conn: Connection) : Repositories {
    override val profile: ProfileRepository = SqliteProfileRepository(conn)
    override val links: LinkRepository = SqliteLinkRepository(conn)
    override val projects: ProjectRepository = SqliteProjectRepository(conn)
    override val images: ImageRepository = SqliteImageRepository(conn)
}
