package com.usuallycoding.portfolio.server.domain

import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.SocialLinkDto

/**
 * The repositories of one unit of work. They only exist inside [Transactor.transaction], where they share one
 * atomic transaction, so none of them opens or commits anything itself.
 */
interface Repositories {
    val profile: ProfileRepository
    val links: LinkRepository
    val projects: ProjectRepository
    val images: ImageRepository
}

/** Runs [block] as one atomic unit of work: if anything throws, nothing in it is kept. */
interface Transactor {
    suspend fun <T> transaction(block: Repositories.() -> T): T
}

/** Checked values for a profile, ready to store. */
class ProfileFields(val name: String, val description: String, val profileImageId: String?, val coverImageId: String?)

class LinkFields(val name: String, val address: String, val isPublic: Boolean, val iconId: String?)

class ProjectFields(
    val title: String,
    val createdAt: String,
    val isPublic: Boolean,
    val summary: String,
    val thumbnailId: String?,
    val imageIds: List<String>,
)

interface ProfileRepository {
    /** Null until the owner has saved a profile once. */
    fun get(): ProfileDto?
    fun save(fields: ProfileFields)
}

interface LinkRepository {
    fun list(publicOnly: Boolean): List<SocialLinkDto>
    fun get(id: Long): SocialLinkDto?
    fun create(fields: LinkFields): SocialLinkDto
    fun update(id: Long, fields: LinkFields): SocialLinkDto

    /** True if a link was removed. */
    fun delete(id: Long): Boolean
    fun ids(): Set<Long>

    /** Stores the order; [ids] must already be known to be exactly the existing ids. */
    fun reorder(ids: List<Long>)
}

interface ProjectRepository {
    fun list(publicOnly: Boolean): List<ProjectSummaryDto>

    /** Null if the project does not exist or (with [publicOnly]) is private. */
    fun get(id: Long, publicOnly: Boolean): ProjectDto?
    fun create(fields: ProjectFields): ProjectDto
    fun update(id: Long, fields: ProjectFields): ProjectDto

    /** True if a project was removed. */
    fun delete(id: Long): Boolean
    fun ids(): Set<Long>
    fun reorder(ids: List<Long>)
}

interface ImageRepository {
    fun find(id: String): ImageRecord?
    fun findByFileName(fileName: String): ImageRecord?
    fun insert(record: ImageRecord)

    /** True when an anonymous visitor can reach the image through public content. */
    fun isPubliclyReferenced(id: String): Boolean

    /** Images uploaded before [cutoff] (epoch seconds) that nothing refers to. */
    fun unreferencedBefore(cutoff: Long): List<ImageRecord>

    /** Removes the image if it is still old and unreferenced; true if it was removed. */
    fun deleteIfUnreferenced(id: String, cutoff: Long): Boolean
}
