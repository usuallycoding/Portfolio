package com.usuallycoding.portfolio.server.service

import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ProjectInput
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.server.domain.AspectRatio
import com.usuallycoding.portfolio.server.domain.ProjectFields
import com.usuallycoding.portfolio.server.domain.Repositories
import com.usuallycoding.portfolio.server.domain.Transactor
import com.usuallycoding.portfolio.server.domain.Validation
import com.usuallycoding.portfolio.server.domain.invalid
import com.usuallycoding.portfolio.server.domain.notFound

private const val MAX_PROJECT_IMAGES = 200

class ProjectService(private val transactor: Transactor, private val ratioTolerance: Double) {
    suspend fun list(publicOnly: Boolean): List<ProjectSummaryDto> = transactor.transaction { projects.list(publicOnly) }

    /** Throws not-found if the project does not exist or (with [publicOnly]) is private. */
    suspend fun get(id: Long, publicOnly: Boolean): ProjectDto =
        transactor.transaction { projects.get(id, publicOnly) } ?: notFound("Project not found")

    suspend fun create(input: ProjectInput): ProjectDto = transactor.transaction { projects.create(fields(input)) }

    suspend fun update(id: Long, input: ProjectInput): ProjectDto = transactor.transaction {
        projects.get(id, publicOnly = false) ?: notFound("Project not found")
        projects.update(id, fields(input))
    }

    suspend fun delete(id: Long) = transactor.transaction {
        if (!projects.delete(id)) notFound("Project not found")
    }

    suspend fun reorder(ids: List<Long>): List<ProjectSummaryDto> = transactor.transaction {
        requireExactOrder(ids, projects.ids())
        projects.reorder(ids)
        projects.list(publicOnly = false)
    }

    private fun Repositories.fields(input: ProjectInput): ProjectFields {
        val thumbnail = images.requireForAttach(input.thumbnailId, AspectRatio.THUMBNAIL, ratioTolerance, "thumbnail")
        if (input.imageIds.size > MAX_PROJECT_IMAGES) invalid("A project can have at most $MAX_PROJECT_IMAGES images")
        if (input.imageIds.size != input.imageIds.toSet().size) invalid("An image can only appear once in a project")
        input.imageIds.forEach { images.requireForAttach(it, null, ratioTolerance, "image") }
        return ProjectFields(
            title = Validation.text(input.title, "title", 200),
            createdAt = Validation.isoDate(input.createdAt, "createdAt"),
            isPublic = input.isPublic,
            summary = Validation.text(input.summary, "summary", 5000, required = false),
            thumbnailId = thumbnail?.id,
            imageIds = input.imageIds,
        )
    }
}
