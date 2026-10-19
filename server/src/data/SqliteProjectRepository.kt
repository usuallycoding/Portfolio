package com.usuallycoding.portfolio.server.data

import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.server.domain.ProjectFields
import com.usuallycoding.portfolio.server.domain.ProjectRepository
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

class SqliteProjectRepository(private val conn: Connection) : ProjectRepository {
    private fun ResultSet.toSummary() =
        ProjectSummaryDto(getLong(1), getString(2), getString(3), getInt(4) == 1, getString(5), imageAt(6))

    override fun list(publicOnly: Boolean): List<ProjectSummaryDto> =
        conn.createStatement().use { st ->
            st.executeQuery(
                """SELECT p.id, p.title, p.created_at, p.is_public, p.summary, i.id, i.file_name, i.width, i.height
                   FROM projects p LEFT JOIN images i ON i.id = p.thumbnail_image_id
                   ${if (publicOnly) "WHERE p.is_public = 1" else ""}
                   ORDER BY p.position, p.id""",
            ).use { rs -> buildList { while (rs.next()) add(rs.toSummary()) } }
        }

    override fun get(id: Long, publicOnly: Boolean): ProjectDto? {
        val summary = conn.prepareStatement(
            """SELECT p.id, p.title, p.created_at, p.is_public, p.summary, i.id, i.file_name, i.width, i.height
               FROM projects p LEFT JOIN images i ON i.id = p.thumbnail_image_id
               WHERE p.id = ? ${if (publicOnly) "AND p.is_public = 1" else ""}""",
        ).use {
            it.setLong(1, id)
            it.executeQuery().use { rs -> if (rs.next()) rs.toSummary() else null }
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

    override fun create(fields: ProjectFields): ProjectDto {
        val id = conn.prepareStatement(
            "INSERT INTO projects(title, created_at, is_public, summary, thumbnail_image_id, position) VALUES (?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS,
        ).use {
            it.setString(1, fields.title)
            it.setString(2, fields.createdAt)
            it.setInt(3, if (fields.isPublic) 1 else 0)
            it.setString(4, fields.summary)
            it.setString(5, fields.thumbnailId)
            it.setInt(6, conn.nextPosition("projects"))
            it.executeUpdate()
            it.generatedKeys.use { keys -> keys.next(); keys.getLong(1) }
        }
        replaceImages(id, fields.imageIds)
        return get(id, publicOnly = false)!!
    }

    override fun update(id: Long, fields: ProjectFields): ProjectDto {
        conn.prepareStatement(
            "UPDATE projects SET title = ?, created_at = ?, is_public = ?, summary = ?, thumbnail_image_id = ? WHERE id = ?",
        ).use {
            it.setString(1, fields.title)
            it.setString(2, fields.createdAt)
            it.setInt(3, if (fields.isPublic) 1 else 0)
            it.setString(4, fields.summary)
            it.setString(5, fields.thumbnailId)
            it.setLong(6, id)
            it.executeUpdate()
        }
        replaceImages(id, fields.imageIds)
        return get(id, publicOnly = false)!!
    }

    override fun delete(id: Long): Boolean =
        conn.prepareStatement("DELETE FROM projects WHERE id = ?").use {
            it.setLong(1, id)
            it.executeUpdate() > 0
        }

    override fun ids(): Set<Long> = conn.ids("projects")

    override fun reorder(ids: List<Long>) = conn.setPositions("projects", ids)

    private fun replaceImages(projectId: Long, imageIds: List<String>) {
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
