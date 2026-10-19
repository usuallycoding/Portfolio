package com.usuallycoding.portfolio.client

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.usuallycoding.portfolio.ImageDto
import io.ktor.client.HttpClient
import com.usuallycoding.portfolio.PortfolioApi
import com.usuallycoding.portfolio.createHttpClient
import com.usuallycoding.portfolio.ui.ProvideImageLoading
import com.usuallycoding.portfolio.ui.ErrorView
import com.usuallycoding.portfolio.ui.Load
import com.usuallycoding.portfolio.ui.LoadingView
import com.usuallycoding.portfolio.ui.PortfolioTheme
import com.usuallycoding.portfolio.ui.SettingsThemeStore
import com.usuallycoding.portfolio.ui.ThemeController
import com.usuallycoding.portfolio.ui.ThemeStore
import com.usuallycoding.portfolio.ui.rememberThemeController
import org.jetbrains.compose.reload.DevelopmentEntryPoint

private const val NO_PROJECT = -1L
private const val NO_IMAGE = -1

/** The read-only portfolio app. [serverUrl] is the base URL of the portfolio server, e.g. `http://10.0.2.2:8090`. */
@Composable
@Preview
@DevelopmentEntryPoint
fun ClientApp(
    serverUrl: String = "http://localhost:8090",
    /** Called with true when the dark theme is showing and false for light, e.g. to match the system bars. */
    onDarkThemeChanged: (Boolean) -> Unit = {},
    themeStore: ThemeStore = remember { SettingsThemeStore() },
    /** Decides platform-specific behavior such as pull to refresh. The Android and iOS shells pass [ClientPlatform.Mobile]. */
    platform: ClientPlatform = ClientPlatform.Web,
    /** Called with false when the system status bar should hide (reading a project) and true when it should show. */
    onSystemBarsVisibleChanged: (Boolean) -> Unit = {},
    /** Ties the current page to an address (the browser's URL on the web). Null on phones, which keep their own navigation. */
    routeHost: RouteHost? = null,
    httpClient: HttpClient = remember { createHttpClient() },
) {
    val api = remember(serverUrl) { PortfolioApi(serverUrl, httpClient) }
    val themeController = rememberThemeController(themeStore)
    val dark = themeController.preference.isDark(isSystemInDarkTheme())
    LaunchedEffect(dark) { onDarkThemeChanged(dark) }

    ProvideImageLoading(httpClient) {
        PortfolioTheme(themeController.preference) {
            Surface(Modifier.fillMaxSize()) {
                ClientNavigation(api, themeController, platform, onSystemBarsVisibleChanged, routeHost)
            }
        }
    }
}

/** Wraps scrolling content so pulling down reloads it, when [enabled]; otherwise shows the content as it is. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshArea(enabled: Boolean, refreshing: Boolean, onRefresh: () -> Unit, content: @Composable () -> Unit) {
    if (enabled) {
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) { content() }
    } else {
        content()
    }
}

/** Shows a refresh failure briefly with a Retry action, leaving the content behind it untouched. */
@Composable
private fun RefreshErrorHost(error: String?, onRetry: () -> Unit, onShown: () -> Unit, modifier: Modifier = Modifier) {
    val host = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        if (error != null) {
            val result = host.showSnackbar(error, actionLabel = "Retry", duration = SnackbarDuration.Long)
            onShown()
            if (result == SnackbarResult.ActionPerformed) onRetry()
        }
    }
    SnackbarHost(host, modifier.safeDrawingPadding())
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ClientNavigation(
    api: PortfolioApi,
    themeController: ThemeController,
    platform: ClientPlatform,
    onSystemBarsVisibleChanged: (Boolean) -> Unit,
    routeHost: RouteHost?,
) {
    // The first page comes from the address (a deep link or reload on the web); otherwise the profile.
    val initialProject = remember(routeHost) { (routeHost?.current() as? Route.Project)?.id ?: NO_PROJECT }
    var projectId by rememberSaveable { mutableStateOf(initialProject) }
    var imageIndex by rememberSaveable { mutableStateOf(NO_IMAGE) }
    // Kept here, not per project, so it survives refreshes and rotation; always starts as the grid.
    var galleryMode by rememberSaveable { mutableStateOf(GalleryMode.Grid.ordinal) }
    val uriHandler = LocalUriHandler.current
    // Kept here so which sections are expanded survives opening a project and coming back to the profile.
    val sections = rememberExpandedSections()

    // The browser's back, forward or an edited URL change the page from outside the app.
    DisposableEffect(routeHost) {
        val stopListening = routeHost?.onChanged { route ->
            val target = (route as? Route.Project)?.id ?: NO_PROJECT
            if (target != projectId) {
                projectId = target
                imageIndex = NO_IMAGE
                galleryMode = GalleryMode.Grid.ordinal
            }
        }
        onDispose { stopListening?.invoke() }
    }
    val openProject: (Long) -> Unit = { id ->
        galleryMode = GalleryMode.Grid.ordinal // every project opens as the grid
        imageIndex = NO_IMAGE
        routeHost?.push(Route.Project(id))
        projectId = id
    }
    val goHome: () -> Unit = {
        imageIndex = NO_IMAGE
        routeHost?.push(Route.Home)
        projectId = NO_PROJECT
    }

    val imageUrl: ImageUrl = { image: ImageDto?, thumbnail: Boolean ->
        image?.let { api.imageUrl(it.fileName, thumbnail) }
    }

    val home = viewModel { HomeViewModel(api) }
    val homeState by home.state.collectAsState()

    if (projectId == NO_PROJECT) {
        val refreshing by home.refreshing.collectAsState()
        val refreshError by home.refreshError.collectAsState()
        Box(Modifier.fillMaxSize()) {
            when (val state = homeState) {
                Load.Loading -> LoadingView()
                is Load.Failed -> ErrorView(state.message, onRetry = home::load)
                is Load.Loaded -> RefreshArea(platform.pullToRefresh, refreshing, home::refresh) {
                    HomeContent(
                        data = state.value,
                        imageUrl = imageUrl,
                        onOpenLink = { link -> runCatching { uriHandler.openUri(link.address) } },
                        onOpenProject = { openProject(it.id) },
                        themeController = themeController,
                        sections = sections,
                        modifier = Modifier.safeDrawingPadding(),
                    )
                }
            }
            RefreshErrorHost(refreshError, onRetry = home::refresh, onShown = home::consumeRefreshError, Modifier.align(Alignment.BottomCenter))
        }
    } else {
        val id = projectId
        val vm = viewModel(key = "project-$id") { ProjectViewModel(api, id) }
        val state by vm.state.collectAsState()
        val refreshing by vm.refreshing.collectAsState()
        val refreshError by vm.refreshError.collectAsState()

        BackHandler(enabled = platform.handlesSystemBack) {
            if (imageIndex != NO_IMAGE) imageIndex = NO_IMAGE else goHome()
        }
        Box(Modifier.fillMaxSize()) {
            when (val s = state) {
                Load.Loading -> LoadingView()
                is Load.Failed -> ErrorView(s.message, onRetry = vm::load)
                is Load.Loaded -> {
                    RefreshArea(platform.pullToRefresh, refreshing, vm::refresh) {
                        ProjectContent(
                            project = s.value,
                            imageUrl = imageUrl,
                            onBack = goHome,
                            onOpenImage = { imageIndex = it },
                            mode = GalleryMode.entries[galleryMode],
                            onModeChange = { galleryMode = it.ordinal },
                            showBackControl = platform.showsBackControl,
                            autoHideBars = platform.autoHideBars,
                            onBarsVisibleChange = onSystemBarsVisibleChanged,
                        )
                    }
                    if (imageIndex != NO_IMAGE) {
                        ImageViewer(s.value, imageIndex, imageUrl, onClose = { imageIndex = NO_IMAGE })
                    }
                }
            }
            RefreshErrorHost(refreshError, onRetry = vm::refresh, onShown = vm::consumeRefreshError, Modifier.align(Alignment.BottomCenter))
        }
    }
}
