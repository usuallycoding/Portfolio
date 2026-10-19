package com.usuallycoding.portfolio.server.service

import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.server.domain.ImageFormat
import com.usuallycoding.portfolio.server.domain.ImageRecord
import com.usuallycoding.portfolio.server.domain.ImageRepository
import com.usuallycoding.portfolio.server.domain.LinkFields
import com.usuallycoding.portfolio.server.domain.LinkRepository
import com.usuallycoding.portfolio.server.domain.ProfileFields
import com.usuallycoding.portfolio.server.domain.ProfileRepository
import com.usuallycoding.portfolio.server.domain.ProjectFields
import com.usuallycoding.portfolio.server.domain.ProjectRepository
import com.usuallycoding.portfolio.server.domain.Repositories
import com.usuallycoding.portfolio.server.domain.Transactor

/** Everything the fake repositories hold, as plain immutable values so a snapshot is a cheap copy. */
data class FakeState(
    val images: Map<String, ImageRecord> = emptyMap(),
    val profile: ProfileFields? = null,
    val links: List<Pair<Long, LinkFields>> = emptyList(),
    val projects: List<Pair<Long, ProjectFields>> = emptyList(),
    val nextId: Long = 1,
)

/**
 * In-memory repositories with the same atomic behaviour as the real transactor: if a unit of work throws,
 * the state is put back exactly as it was.
 */
class FakeTransactor(var state: FakeState = FakeState()) : Transactor {
    val repositories: Repositories = object : Repositories {
        override val profile = FakeProfiles()
        override val links = FakeLinks()
        override val projects = FakeProjects()
        override val images = FakeImages()
    }

    override suspend fun <T> transaction(block: Repositories.() -> T): T {
        val before = state
        try {
            return repositories.block()
        } catch (e: Throwable) {
            state = before
            throw e
        }
    }

    /** Which images count as reachable from public content. */
    var publicImageIds: Set<String> = emptySet()

    fun addImage(id: String, width: Int, height: Int, uploadedAt: Long = 0) {
        state = state.copy(images = state.images + (id to ImageRecord(id, "$id.jpg", ImageFormat.JPEG, width, height, uploadedAt)))
    }

    private fun image(id: String?): ImageDto? = id?.let { state.images[it]?.toDto() }

    inner class FakeImages : ImageRepository {
        override fun find(id: String) = state.images[id]
        override fun findByFileName(fileName: String) = state.images.values.firstOrNull { it.fileName == fileName }
        override fun insert(record: ImageRecord) {
            state = state.copy(images = state.images + (record.id to record))
        }
        override fun isPubliclyReferenced(id: String) = id in publicImageIds

        private fun referenced(): Set<String> = buildSet {
            state.profile?.let { addAll(listOfNotNull(it.profileImageId, it.coverImageId)) }
            state.links.forEach { it.second.iconId?.let(::add) }
            state.projects.forEach { (_, f) -> addAll(listOfNotNull(f.thumbnailId)); addAll(f.imageIds) }
        }

        override fun unreferencedBefore(cutoff: Long) =
            state.images.values.filter { it.uploadedAt < cutoff && it.id !in referenced() }

        override fun deleteIfUnreferenced(id: String, cutoff: Long): Boolean {
            val record = state.images[id] ?: return false
            if (record.uploadedAt >= cutoff || id in referenced()) return false
            state = state.copy(images = state.images - id)
            return true
        }
    }

    inner class FakeProfiles : ProfileRepository {
        override fun get(): ProfileDto? = state.profile?.let {
            ProfileDto(it.name, it.description, image(it.profileImageId), image(it.coverImageId))
        }
        override fun save(fields: ProfileFields) {
            state = state.copy(profile = fields)
        }
    }

    inner class FakeLinks : LinkRepository {
        private fun dto(id: Long, f: LinkFields) = SocialLinkDto(id, f.name, f.address, f.isPublic, image(f.iconId))
        override fun list(publicOnly: Boolean) = state.links.filter { !publicOnly || it.second.isPublic }.map { dto(it.first, it.second) }
        override fun get(id: Long) = state.links.firstOrNull { it.first == id }?.let { dto(it.first, it.second) }
        override fun create(fields: LinkFields): SocialLinkDto {
            val id = state.nextId
            state = state.copy(links = state.links + (id to fields), nextId = id + 1)
            return get(id)!!
        }
        override fun update(id: Long, fields: LinkFields): SocialLinkDto {
            state = state.copy(links = state.links.map { if (it.first == id) id to fields else it })
            return get(id)!!
        }
        override fun delete(id: Long): Boolean {
            val had = state.links.any { it.first == id }
            state = state.copy(links = state.links.filter { it.first != id })
            return had
        }
        override fun ids() = state.links.map { it.first }.toSet()
        override fun reorder(ids: List<Long>) {
            state = state.copy(links = ids.map { id -> state.links.first { it.first == id } })
        }
    }

    inner class FakeProjects : ProjectRepository {
        private fun summary(id: Long, f: ProjectFields) = ProjectSummaryDto(id, f.title, f.createdAt, f.isPublic, f.summary, image(f.thumbnailId))
        private fun full(id: Long, f: ProjectFields) =
            summary(id, f).let { ProjectDto(id, f.title, f.createdAt, f.isPublic, f.summary, it.thumbnail, f.imageIds.mapNotNull(::image)) }
        override fun list(publicOnly: Boolean) =
            state.projects.filter { !publicOnly || it.second.isPublic }.map { summary(it.first, it.second) }
        override fun get(id: Long, publicOnly: Boolean) =
            state.projects.firstOrNull { it.first == id && (!publicOnly || it.second.isPublic) }?.let { full(it.first, it.second) }
        override fun create(fields: ProjectFields): ProjectDto {
            val id = state.nextId
            state = state.copy(projects = state.projects + (id to fields), nextId = id + 1)
            return get(id, publicOnly = false)!!
        }
        override fun update(id: Long, fields: ProjectFields): ProjectDto {
            state = state.copy(projects = state.projects.map { if (it.first == id) id to fields else it })
            return get(id, publicOnly = false)!!
        }
        override fun delete(id: Long): Boolean {
            val had = state.projects.any { it.first == id }
            state = state.copy(projects = state.projects.filter { it.first != id })
            return had
        }
        override fun ids() = state.projects.map { it.first }.toSet()
        override fun reorder(ids: List<Long>) {
            state = state.copy(projects = ids.map { id -> state.projects.first { it.first == id } })
        }
    }
}
