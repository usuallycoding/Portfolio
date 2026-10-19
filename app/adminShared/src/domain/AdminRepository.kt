package com.usuallycoding.portfolio.admin.domain

import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProfileInput
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ProjectInput
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.SocialLinkInput
import kotlinx.coroutines.flow.StateFlow

/**
 * What the owner's app does with the portfolio. Failures are thrown. A call the server rejects as unauthorized
 * ends the login, so [loggedIn] turns false and the app returns to the login screen.
 */
interface AdminRepository {
    val loggedIn: StateFlow<Boolean>

    /** Throws on a wrong password, too many attempts, or no connection. */
    suspend fun login(password: String)
    fun logout()

    /** The address an image is loaded from. */
    fun imageUrl(fileName: String, thumbnail: Boolean): String

    suspend fun profile(): ProfileDto
    suspend fun updateProfile(input: ProfileInput): ProfileDto

    suspend fun links(): List<SocialLinkDto>
    suspend fun createLink(input: SocialLinkInput): SocialLinkDto
    suspend fun updateLink(id: Long, input: SocialLinkInput): SocialLinkDto
    suspend fun deleteLink(id: Long)
    suspend fun reorderLinks(ids: List<Long>): List<SocialLinkDto>

    suspend fun projects(): List<ProjectSummaryDto>
    suspend fun project(id: Long): ProjectDto
    suspend fun createProject(input: ProjectInput): ProjectDto
    suspend fun updateProject(id: Long, input: ProjectInput): ProjectDto
    suspend fun deleteProject(id: Long)
    suspend fun reorderProjects(ids: List<Long>): List<ProjectSummaryDto>

    suspend fun uploadImage(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
        onProgress: (sent: Long, total: Long?) -> Unit = { _, _ -> },
    ): ImageDto
}
