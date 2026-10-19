package com.usuallycoding.portfolio.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

/**
 * A single SQLite connection guarded by a lock. Work runs on the IO dispatcher inside a transaction,
 * so concurrent requests are serialized and each unit of work is atomic.
 */
class Database(path: Path) : AutoCloseable {
    private val lock = Any()
    private val connection: Connection

    init {
        path.toAbsolutePath().parent?.let { Files.createDirectories(it) }
        connection = DriverManager.getConnection("jdbc:sqlite:${path.toAbsolutePath()}")
        connection.createStatement().use {
            it.execute("PRAGMA foreign_keys = ON")
            it.execute("PRAGMA journal_mode = WAL")
            it.execute("PRAGMA busy_timeout = 5000")
        }
        migrate()
    }

    /** Runs [block] in a transaction; any exception rolls everything back. */
    fun <T> transactionBlocking(block: (Connection) -> T): T = synchronized(lock) {
        connection.autoCommit = false
        try {
            val result = block(connection)
            connection.commit()
            result
        } catch (e: Throwable) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }
    }

    suspend fun <T> transaction(block: (Connection) -> T): T =
        withContext(Dispatchers.IO) { transactionBlocking(block) }

    private fun migrate() {
        val version = connection.createStatement().use { st ->
            st.executeQuery("PRAGMA user_version").use { it.next(); it.getInt(1) }
        }
        if (version >= SCHEMA_VERSION) return
        transactionBlocking { conn ->
            conn.createStatement().use { st ->
                SCHEMA.forEach { st.execute(it) }
                st.execute("PRAGMA user_version = $SCHEMA_VERSION")
            }
        }
    }

    override fun close() = synchronized(lock) { connection.close() }

    companion object {
        const val SCHEMA_VERSION = 1

        private val SCHEMA = listOf(
            """CREATE TABLE images (
                id TEXT PRIMARY KEY,
                file_name TEXT NOT NULL UNIQUE,
                format TEXT NOT NULL,
                width INTEGER NOT NULL,
                height INTEGER NOT NULL,
                uploaded_at INTEGER NOT NULL
            )""",
            """CREATE TABLE profile (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                profile_image_id TEXT REFERENCES images(id),
                cover_image_id TEXT REFERENCES images(id)
            )""",
            """CREATE TABLE social_links (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                address TEXT NOT NULL,
                is_public INTEGER NOT NULL,
                icon_image_id TEXT REFERENCES images(id),
                position INTEGER NOT NULL
            )""",
            """CREATE TABLE projects (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                created_at TEXT NOT NULL,
                is_public INTEGER NOT NULL,
                summary TEXT NOT NULL,
                thumbnail_image_id TEXT REFERENCES images(id),
                position INTEGER NOT NULL
            )""",
            """CREATE TABLE project_images (
                project_id INTEGER NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                image_id TEXT NOT NULL REFERENCES images(id),
                position INTEGER NOT NULL,
                PRIMARY KEY (project_id, image_id)
            )""",
            """CREATE TABLE auth_tokens (
                token_hash TEXT PRIMARY KEY,
                expires_at INTEGER NOT NULL
            )""",
            "CREATE INDEX idx_project_images_image ON project_images(image_id)",
            "CREATE INDEX idx_images_uploaded_at ON images(uploaded_at)",
        )
    }
}
