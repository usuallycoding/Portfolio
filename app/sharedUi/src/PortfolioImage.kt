package com.usuallycoding.portfolio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.SubcomposeAsyncImage
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient

/** The loader used by every [PortfolioImage] below [ProvideImageLoading]; null means Coil's default. */
private val LocalPortfolioImageLoader = staticCompositionLocalOf<ImageLoader?> { null }

/**
 * Loads every [PortfolioImage] inside [content] through [httpClient]. The loader is provided through the composition
 * rather than Coil's global singleton, because the singleton can only be configured once per process and throws if
 * the app's UI is composed again (for example when Android recreates the activity on rotation).
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProvideImageLoading(httpClient: HttpClient, content: @Composable () -> Unit) {
    val context = LocalPlatformContext.current
    val loader = remember(httpClient, context) {
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(httpClient)) }
            .build()
    }
    CompositionLocalProvider(LocalPortfolioImageLoader provides loader, content = content)
}

@Composable
private fun ImagePlaceholder(modifier: Modifier, contentDescription: String? = null) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
    )
}

/** An image from a URL, or a neutral placeholder when [url] is null or while it loads or fails. */
@Composable
fun PortfolioImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (url == null) {
        ImagePlaceholder(modifier, contentDescription)
        return
    }
    SubcomposeAsyncImage(
        model = url,
        imageLoader = LocalPortfolioImageLoader.current ?: SingletonImageLoader.get(LocalPlatformContext.current),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = { ImagePlaceholder(Modifier.matchParentSize(), contentDescription) },
        error = { ImagePlaceholder(Modifier.matchParentSize(), contentDescription) },
    )
}
