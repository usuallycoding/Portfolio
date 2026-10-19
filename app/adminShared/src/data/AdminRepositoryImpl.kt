package com.usuallycoding.portfolio.admin.data

import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProfileInput
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ProjectInput
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.SocialLinkInput
import com.usuallycoding.portfolio.admin.domain.AdminRepository
import kotlinx.coroutines.flow.StateFlow

/** Every call goes through [AdminSession.call], so a 401 anywhere ends the login. */
class AdminRepositoryImpl(private val session: AdminSession) : AdminRepository {
    override val loggedIn: StateFlow<Boolean> get() = session.loggedIn

    override suspend fun login(password: String) = session.login(password)
    override fun logout() = session.logout()
    override fun imageUrl(fileName: String, thumbnail: Boolean) = session.imageUrl(fileName, thumbnail)

    override suspend fun profile(): ProfileDto = session.call { adminProfile() }
    override suspend fun updateProfile(input: ProfileInput): ProfileDto = session.call { updateProfile(input) }

    override suspend fun links(): List<SocialLinkDto> = session.call { adminLinks() }
    override suspend fun createLink(input: SocialLinkInput): SocialLinkDto = session.call { createLink(input) }
    override suspend fun updateLink(id: Long, input: SocialLinkInput): SocialLinkDto = session.call { updateLink(id, input) }
    override suspend fun deleteLink(id: Long) = session.call { deleteLink(id) }
    override suspend fun reorderLinks(ids: List<Long>): List<SocialLinkDto> = session.call { reorderLinks(ids) }

    override suspend fun projects(): List<ProjectSummaryDto> = session.call { adminProjects() }
    override suspend fun project(id: Long): ProjectDto = session.call { adminProject(id) }
    override suspend fun createProject(input: ProjectInput): ProjectDto = session.call { createProject(input) }
    override suspend fun updateProject(id: Long, input: ProjectInput): ProjectDto = session.call { updateProject(id, input) }
    override suspend fun deleteProject(id: Long) = session.call { deleteProject(id) }
    override suspend fun reorderProjects(ids: List<Long>): List<ProjectSummaryDto> = session.call { reorderProjects(ids) }

    override suspend fun uploadImage(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
        onProgress: (sent: Long, total: Long?) -> Unit,
    ): ImageDto = session.call { uploadImage(bytes, fileName, contentType, onProgress) }
}
