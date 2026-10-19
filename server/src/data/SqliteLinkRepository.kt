package com.usuallycoding.portfolio.server.data

import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.server.domain.LinkFields
import com.usuallycoding.portfolio.server.domain.LinkRepository
import java.sql.Connection
import java.sql.Statement

class SqliteLinkRepository(private val conn: Connection) : LinkRepository {
    override fun list(publicOnly: Boolean): List<SocialLinkDto> =
        conn.createStatement().use { st ->
            st.executeQuery(
                """SELECT l.id, l.name, l.address, l.is_public, i.id, i.file_name, i.width, i.height
                   FROM social_links l LEFT JOIN images i ON i.id = l.icon_image_id
                   ${if (publicOnly) "WHERE l.is_public = 1" else ""}
                   ORDER BY l.position, l.id""",
            ).use { rs ->
                buildList {
                    while (rs.next()) add(SocialLinkDto(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4) == 1, rs.imageAt(5)))
                }
            }
        }

    override fun get(id: Long): SocialLinkDto? = list(publicOnly = false).firstOrNull { it.id == id }

    override fun create(fields: LinkFields): SocialLinkDto {
        val position = conn.nextPosition("social_links")
        val id = conn.prepareStatement(
            "INSERT INTO social_links(name, address, is_public, icon_image_id, position) VALUES (?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS,
        ).use {
            it.setString(1, fields.name)
            it.setString(2, fields.address)
            it.setInt(3, if (fields.isPublic) 1 else 0)
            it.setString(4, fields.iconId)
            it.setInt(5, position)
            it.executeUpdate()
            it.generatedKeys.use { keys -> keys.next(); keys.getLong(1) }
        }
        return get(id)!!
    }

    override fun update(id: Long, fields: LinkFields): SocialLinkDto {
        conn.prepareStatement("UPDATE social_links SET name = ?, address = ?, is_public = ?, icon_image_id = ? WHERE id = ?").use {
            it.setString(1, fields.name)
            it.setString(2, fields.address)
            it.setInt(3, if (fields.isPublic) 1 else 0)
            it.setString(4, fields.iconId)
            it.setLong(5, id)
            it.executeUpdate()
        }
        return get(id)!!
    }

    override fun delete(id: Long): Boolean =
        conn.prepareStatement("DELETE FROM social_links WHERE id = ?").use {
            it.setLong(1, id)
            it.executeUpdate() > 0
        }

    override fun ids(): Set<Long> = conn.ids("social_links")

    override fun reorder(ids: List<Long>) = conn.setPositions("social_links", ids)
}
