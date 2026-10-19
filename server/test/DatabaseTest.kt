package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.server.data.*

import java.nio.file.Files
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DatabaseTest {
    private fun newDb() = Database(Files.createTempDirectory("db-test").resolve("p.db"))

    @Test
    fun freshDatabaseHasAllTables() {
        newDb().use { db ->
            val tables = db.transactionBlocking { conn ->
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'").use { rs ->
                        buildSet { while (rs.next()) add(rs.getString(1)) }
                    }
                }
            }
            assertTrue(
                tables.containsAll(
                    listOf("images", "profile", "social_links", "projects", "project_images", "auth_tokens"),
                ),
                "tables were $tables",
            )
        }
    }

    @Test
    fun foreignKeysAreEnforced() {
        newDb().use { db ->
            assertFailsWith<SQLException> {
                db.transactionBlocking { conn ->
                    conn.createStatement().use {
                        it.execute("INSERT INTO project_images(project_id, image_id, position) VALUES (99, 'nope', 0)")
                    }
                }
            }
        }
    }

    @Test
    fun failedTransactionRollsBack() {
        newDb().use { db ->
            runCatching {
                db.transactionBlocking { conn ->
                    conn.createStatement().use {
                        it.execute("INSERT INTO images VALUES ('a','a.jpg','jpg',1,1,0)")
                    }
                    error("boom")
                }
            }
            val count = db.transactionBlocking { conn ->
                conn.createStatement().use { st -> st.executeQuery("SELECT COUNT(*) FROM images").use { it.next(); it.getInt(1) } }
            }
            assertEquals(0, count)
        }
    }

    @Test
    fun reopeningKeepsDataAndDoesNotRecreateSchema() {
        val path = Files.createTempDirectory("db-test").resolve("p.db")
        Database(path).use { db ->
            db.transactionBlocking { conn ->
                conn.createStatement().use { it.execute("INSERT INTO images VALUES ('a','a.jpg','jpg',1,1,0)") }
            }
        }
        Database(path).use { db ->
            val count = db.transactionBlocking { conn ->
                conn.createStatement().use { st -> st.executeQuery("SELECT COUNT(*) FROM images").use { it.next(); it.getInt(1) } }
            }
            assertEquals(1, count)
        }
    }
}
