package com.usuallycoding.portfolio.client.data

import com.usuallycoding.portfolio.PortfolioApi
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.client.domain.HomeData
import com.usuallycoding.portfolio.client.domain.PortfolioRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class PortfolioRepositoryImpl(private val api: PortfolioApi) : PortfolioRepository {
    override suspend fun home(): HomeData = coroutineScope {
        val profile = async { api.profile() }
        val links = async { api.links() }
        val projects = async { api.projects() }
        HomeData(profile.await(), links.await(), projects.await())
    }

    override suspend fun project(id: Long): ProjectDto = api.project(id)

    override fun imageUrl(fileName: String, thumbnail: Boolean): String = api.imageUrl(fileName, thumbnail)
}
