package com.usuallycoding.portfolio.server.data

import com.usuallycoding.portfolio.ImageDto
import java.sql.Connection
import java.sql.ResultSet

/** Reads an image from four consecutive columns (id, file_name, width, height), or null if the join found none. */
internal fun ResultSet.imageAt(first: Int): ImageDto? {
    val id = getString(first) ?: return null
    return ImageDto(id, getString(first + 1), getInt(first + 2), getInt(first + 3))
}

internal fun Connection.nextPosition(table: String): Int =
    createStatement().use { st -> st.executeQuery("SELECT COALESCE(MAX(position), -1) + 1 FROM $table").use { it.next(); it.getInt(1) } }

internal fun Connection.ids(table: String): Set<Long> =
    createStatement().use { st ->
        st.executeQuery("SELECT id FROM $table").use { rs -> buildSet { while (rs.next()) add(rs.getLong(1)) } }
    }

/** Rewrites positions to match [ids]. */
internal fun Connection.setPositions(table: String, ids: List<Long>) {
    prepareStatement("UPDATE $table SET position = ? WHERE id = ?").use { st ->
        ids.forEachIndexed { index, id ->
            st.setInt(1, index)
            st.setLong(2, id)
            st.executeUpdate()
        }
    }
}
