package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProfileInput
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ProjectInput
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.SocialLinkInput
import java.sql.Connection
import java.sql.ResultSet

const val PLACEHOLDER_NAME = "Your name"
const val PLACEHOLDER_DESCRIPTION = "This portfolio has not been set up yet."
private const val MAX_PROJECT_IMAGES = 200

/** Reads an image from four consecutive columns (id, file_name, width, height), or null if the join found none. */
private fun ResultSet.imageAt(first: Int): ImageDto? {
    val id = getString(first) ?: return null
    return ImageDto(id, getString(first + 1), getInt(first + 2), getInt(first + 3))
}

private fun Connection.nextPosition(table: String): Int =
    createStatement().use { st -> st.executeQuery("SELECT COALESCE(MAX(position), -1) + 1 FROM $table").use { it.next(); it.getInt(1) } }

/** Rewrites positions to match [ids], which must be exactly the existing ids, each once. */
private fun Connection.reorder(table: String, ids: List<Long>) {
    val existing = createStatement().use { st ->
        st.executeQuery("SELECT id FROM $table").use { rs -> buildSet { while (rs.next()) add(rs.getLong(1)) } }
    }
    if (ids.size != ids.toSet().size || ids.toSet() != existing) {
        badRequest("The order must list every existing id exactly once")
    }
    prepareStatement("UPDATE $table SET position = ? WHERE id = ?").use { st ->
        ids.forEachIndexed { index, id ->
            st.setInt(1, index)
            st.setLong(2, id)
            st.executeUpdate()
        }
    }
}

class ProfileRepository(private val config: ServerConfig) {
    fun get(conn: Connection): ProfileDto =
        conn.createStatement().use { st ->
            st.executeQuery(
                """SELECT p.name, p.description, a.id, a.file_name, a.width, a.height, c.id, c.file_name, c.width, c.height
                   FROM profile p
                   LEFT JOIN images a ON a.id = p.profile_image_id
                   LEFT JOIN images c ON c.id = p.cover_image_id
                   WHERE p.id = 1""",
            ).use { rs ->
                if (rs.next()) ProfileDto(rs.getString(1), rs.getString(2), rs.imageAt(3), rs.imageAt(7))
                else ProfileDto(PLACEHOLDER_NAME, PLACEHOLDER_DESCRIPTION)
            }
        }

    fun update(conn: Connection, input: ProfileInput): ProfileDto {
        val name = Validation.text(input.name, "name", 200)
        val description = Validation.text(input.description, "description", 5000, required = false)
        val picture = Images.requireForAttach(conn, input.profileImageId, AspectRatio.SQUARE, config.ratioTolerance, "profile picture")
        val cover = Images.requireForAttach(conn, input.coverImageId, AspectRatio.COVER, config.ratioTolerance, "cover image")
        conn.prepareStatement(
            """INSERT INTO profile(id, name, description, profile_image_id, cover_image_id) VALUES (1, ?, ?, ?, ?)
               ON CONFLICT(id) DO UPDATE SET name = excluded.name, description = excluded.description,
               profile_image_id = excluded.profile_image_id, cover_image_id = excluded.cover_image_id""",
        ).use {
            it.setString(1, name)
            it.setString(2, description)
            it.setString(3, picture?.id)
            it.setString(4, cover?.id)
            it.executeUpdate()
        }
        return get(conn)
    }
}

class LinkRepository(private val config: ServerConfig) {
    fun list(conn: Connection, publicOnly: Boolean): List<SocialLinkDto> =
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

    private fun get(conn: Connection, id: Long): SocialLinkDto? = list(conn, publicOnly = false).firstOrNull { it.id == id }

    fun create(conn: Connection, input: SocialLinkInput): SocialLinkDto {
        val (name, address, icon) = validate(conn, input)
        val position = conn.nextPosition("social_links")
        val id = conn.prepareStatement(
            "INSERT INTO social_links(name, address, is_public, icon_image_id, position) VALUES (?,?,?,?,?)",
            java.sql.Statement.RETURN_GENERATED_KEYS,
        ).use {
            it.setString(1, name)
            it.setString(2, address)
            it.setInt(3, if (input.isPublic) 1 else 0)
            it.setString(4, icon)
            it.setInt(5, position)
            it.executeUpdate()
            it.generatedKeys.use { keys -> keys.next(); keys.getLong(1) }
        }
        return get(conn, id)!!
    }

    fun update(conn: Connection, id: Long, input: SocialLinkInput): SocialLinkDto {
        get(conn, id) ?: notFound("Link not found")
        val (name, address, icon) = validate(conn, input)
        conn.prepareStatement("UPDATE social_links SET name = ?, address = ?, is_public = ?, icon_image_id = ? WHERE id = ?").use {
            it.setString(1, name)
            it.setString(2, address)
            it.setInt(3, if (input.isPublic) 1 else 0)
            it.setString(4, icon)
            it.setLong(5, id)
            it.executeUpdate()
        }
        return get(conn, id)!!
    }

    fun delete(conn: Connection, id: Long) {
        val deleted = conn.prepareStatement("DELETE FROM social_links WHERE id = ?").use {
            it.setLong(1, id)
            it.executeUpdate()
        }
        if (deleted == 0) notFound("Link not found")
    }

    fun reorder(conn: Connection, ids: List<Long>): List<SocialLinkDto> {
        conn.reorder("social_links", ids)
        return list(conn, publicOnly = false)
    }

    private fun validate(conn: Connection, input: SocialLinkInput): Triple<String, String, String?> {
        val name = Validation.text(input.name, "name", 100)
        val address = Validation.linkAddress(input.address)
        val icon = Images.requireForAttach(conn, input.iconId, AspectRatio.SQUARE, config.ratioTolerance, "icon")
        return Triple(name, address, icon?.id)
    }
}

class ProjectRepository(private val config: ServerConfig) {
    fun list(conn: Connection, publicOnly: Boolean): List<ProjectSummaryDto> =
        conn.createStatement().use { st ->
            st.executeQuery(
                """SELECT p.id, p.title, p.created_at, p.is_public, p.summary, i.id, i.file_name, i.width, i.height
                   FROM projects p LEFT JOIN images i ON i.id = p.thumbnail_image_id
                   ${if (publicOnly) "WHERE p.is_public = 1" else ""}
                   ORDER BY p.position, p.id""",
            ).use { rs ->
                buildList {
                    while (rs.next()) {
                        add(ProjectSummaryDto(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4) == 1, rs.getString(5), rs.imageAt(6)))
                    }
                }
            }
        }

    /** Returns the project, or null if it does not exist or (with [publicOnly]) is private. */
    fun get(conn: Connection, id: Long, publicOnly: Boolean): ProjectDto? {
        val summary = conn.prepareStatement(
            """SELECT p.id, p.title, p.created_at, p.is_public, p.summary, i.id, i.file_name, i.width, i.height
               FROM projects p LEFT JOIN images i ON i.id = p.thumbnail_image_id
               WHERE p.id = ? ${if (publicOnly) "AND p.is_public = 1" else ""}""",
        ).use {
            it.setLong(1, id)
            it.executeQuery().use { rs ->
                if (rs.next()) ProjectSummaryDto(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4) == 1, rs.getString(5), rs.imageAt(6)) else null
            }
        } ?: return null
        val images = conn.prepareStatement(
            """SELECT i.id, i.file_name, i.width, i.height FROM project_images pi
               JOIN images i ON i.id = pi.image_id WHERE pi.project_id = ? ORDER BY pi.position""",
        ).use {
            it.setLong(1, id)
            it.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.imageAt(1)!!) } }
        }
        return ProjectDto(summary.id, summary.title, summary.createdAt, summary.isPublic, summary.summary, summary.thumbnail, images)
    }

    fun create(conn: Connection, input: ProjectInput): ProjectDto {
        val valid = validate(conn, input)
        val id = conn.prepareStatement(
            "INSERT INTO projects(title, created_at, is_public, summary, thumbnail_image_id, position) VALUES (?,?,?,?,?,?)",
            java.sql.Statement.RETURN_GENERATED_KEYS,
        ).use {
            it.setString(1, valid.title)
            it.setString(2, valid.createdAt)
            it.setInt(3, if (input.isPublic) 1 else 0)
            it.setString(4, valid.summary)
            it.setString(5, valid.thumbnailId)
            it.setInt(6, conn.nextPosition("projects"))
            it.executeUpdate()
            it.generatedKeys.use { keys -> keys.next(); keys.getLong(1) }
        }
        replaceImages(conn, id, input.imageIds)
        return get(conn, id, publicOnly = false)!!
    }

    fun update(conn: Connection, id: Long, input: ProjectInput): ProjectDto {
        get(conn, id, publicOnly = false) ?: notFound("Project not found")
        val valid = validate(conn, input)
        conn.prepareStatement(
            "UPDATE projects SET title = ?, created_at = ?, is_public = ?, summary = ?, thumbnail_image_id = ? WHERE id = ?",
        ).use {
            it.setString(1, valid.title)
            it.setString(2, valid.createdAt)
            it.setInt(3, if (input.isPublic) 1 else 0)
            it.setString(4, valid.summary)
            it.setString(5, valid.thumbnailId)
            it.setLong(6, id)
            it.executeUpdate()
        }
        replaceImages(conn, id, input.imageIds)
        return get(conn, id, publicOnly = false)!!
    }

    fun delete(conn: Connection, id: Long) {
        val deleted = conn.prepareStatement("DELETE FROM projects WHERE id = ?").use {
            it.setLong(1, id)
            it.executeUpdate()
        }
        if (deleted == 0) notFound("Project not found")
    }

    fun reorder(conn: Connection, ids: List<Long>): List<ProjectSummaryDto> {
        conn.reorder("projects", ids)
        return list(conn, publicOnly = false)
    }

    private class Valid(val title: String, val createdAt: String, val summary: String, val thumbnailId: String?)

    private fun validate(conn: Connection, input: ProjectInput): Valid {
        val thumbnail = Images.requireForAttach(conn, input.thumbnailId, AspectRatio.THUMBNAIL, config.ratioTolerance, "thumbnail")
        if (input.imageIds.size > MAX_PROJECT_IMAGES) badRequest("A project can have at most $MAX_PROJECT_IMAGES images")
        if (input.imageIds.size != input.imageIds.toSet().size) badRequest("An image can only appear once in a project")
        input.imageIds.forEach { Images.requireForAttach(conn, it, null, config.ratioTolerance, "image") }
        return Valid(
            title = Validation.text(input.title, "title", 200),
            createdAt = Validation.isoDate(input.createdAt, "createdAt"),
            summary = Validation.text(input.summary, "summary", 5000, required = false),
            thumbnailId = thumbnail?.id,
        )
    }

    private fun replaceImages(conn: Connection, projectId: Long, imageIds: List<String>) {
        conn.prepareStatement("DELETE FROM project_images WHERE project_id = ?").use {
            it.setLong(1, projectId)
            it.executeUpdate()
        }
        conn.prepareStatement("INSERT INTO project_images(project_id, image_id, position) VALUES (?,?,?)").use { st ->
            imageIds.forEachIndexed { index, imageId ->
                st.setLong(1, projectId)
                st.setString(2, imageId)
                st.setInt(3, index)
                st.executeUpdate()
            }
        }
    }
}
