package com.usuallycoding.portfolio.client

import com.usuallycoding.portfolio.PortfolioApi
import com.usuallycoding.portfolio.client.data.PortfolioRepositoryImpl
import com.usuallycoding.portfolio.client.presentation.HomeViewModel
import com.usuallycoding.portfolio.client.presentation.ProjectViewModel

/** Builds view models over the real repository, with only the HTTP layer faked. */
fun HomeViewModel(api: PortfolioApi) = HomeViewModel(PortfolioRepositoryImpl(api))

fun ProjectViewModel(api: PortfolioApi, id: Long) = ProjectViewModel(PortfolioRepositoryImpl(api), id)
