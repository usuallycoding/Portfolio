package com.usuallycoding.portfolio.server.data

import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.server.domain.ProfileFields
import com.usuallycoding.portfolio.server.domain.ProfileRepository
import java.sql.Connection

class SqliteProfileRepository(private val conn: Connection) : ProfileRepository {
    override fun get(): ProfileDto? =
        conn.createStatement().use { st ->
            st.executeQuery(
                """SELECT p.name, p.description, a.id, a.file_name, a.width, a.height, c.id, c.file_name, c.width, c.height
                   FROM profile p
                   LEFT JOIN images a ON a.id = p.profile_image_id
                   LEFT JOIN images c ON c.id = p.cover_image_id
                   WHERE p.id = 1""",
            ).use { rs ->
                if (rs.next()) ProfileDto(rs.getString(1), rs.getString(2), rs.imageAt(3), rs.imageAt(7)) else null
            }
        }

    override fun save(fields: ProfileFields) {
        conn.prepareStatement(
            """INSERT INTO profile(id, name, description, profile_image_id, cover_image_id) VALUES (1, ?, ?, ?, ?)
               ON CONFLICT(id) DO UPDATE SET name = excluded.name, description = excluded.description,
               profile_image_id = excluded.profile_image_id, cover_image_id = excluded.cover_image_id""",
        ).use {
            it.setString(1, fields.name)
            it.setString(2, fields.description)
            it.setString(3, fields.profileImageId)
            it.setString(4, fields.coverImageId)
            it.executeUpdate()
        }
    }
}
