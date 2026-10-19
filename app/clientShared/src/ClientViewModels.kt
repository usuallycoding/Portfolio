package com.usuallycoding.portfolio.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.portfolio.PortfolioApi
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.ui.Load
import com.usuallycoding.portfolio.ui.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeData(
    val profile: ProfileDto,
    val links: List<SocialLinkDto>,
    val projects: List<ProjectSummaryDto>,
)

private suspend fun <T> attempt(block: suspend () -> T): Load<T> = try {
    Load.Loaded(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Load.Failed(e.userMessage())
}

class HomeViewModel(private val api: PortfolioApi) : ViewModel() {
    private val _state = MutableStateFlow<Load<HomeData>>(Load.Loading)
    val state: StateFlow<Load<HomeData>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _refreshError = MutableStateFlow<String?>(null)
    /** Set when a refresh fails; the content on screen is left as it was. Cleared with [consumeRefreshError]. */
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    init {
        load()
    }

    private suspend fun fetch(): HomeData = coroutineScope {
        val profile = async { api.profile() }
        val links = async { api.links() }
        val projects = async { api.projects() }
        HomeData(profile.await(), links.await(), projects.await())
    }

    /** The initial load, and the full-screen Retry: shows the loading state while it runs. */
    fun load() {
        _state.value = Load.Loading
        viewModelScope.launch { _state.value = attempt { fetch() } }
    }

    /** Reloads in place: what is on screen stays until the new data arrives, and a failure only reports an error. */
    fun refresh() {
        if (_state.value !is Load.Loaded) return load()
        if (_refreshing.value) return
        _refreshing.value = true
        _refreshError.value = null
        viewModelScope.launch {
            try {
                _state.value = Load.Loaded(fetch())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _refreshError.value = e.userMessage()
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun consumeRefreshError() {
        _refreshError.value = null
    }
}

class ProjectViewModel(private val api: PortfolioApi, private val id: Long) : ViewModel() {
    private val _state = MutableStateFlow<Load<ProjectDto>>(Load.Loading)
    val state: StateFlow<Load<ProjectDto>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = Load.Loading
        viewModelScope.launch { _state.value = attempt { api.project(id) } }
    }

    fun refresh() {
        if (_state.value !is Load.Loaded) return load()
        if (_refreshing.value) return
        _refreshing.value = true
        _refreshError.value = null
        viewModelScope.launch {
            try {
                _state.value = Load.Loaded(api.project(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _refreshError.value = e.userMessage()
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun consumeRefreshError() {
        _refreshError.value = null
    }
}
