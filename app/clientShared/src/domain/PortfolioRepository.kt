package com.usuallycoding.portfolio.client.domain

import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.SocialLinkDto

/** Everything the home page shows, loaded together. */
data class HomeData(
    val profile: ProfileDto,
    val links: List<SocialLinkDto>,
    val projects: List<ProjectSummaryDto>,
)

/** What the client app reads from the portfolio. Failures are thrown; callers decide how to show them. */
interface PortfolioRepository {
    suspend fun home(): HomeData
    suspend fun project(id: Long): ProjectDto

    /** The address an image is loaded from. */
    fun imageUrl(fileName: String, thumbnail: Boolean): String
}
