package com.usuallycoding.portfolio.client

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ui.PortfolioImage

/** How a project's images are shown: a grid of thumbnails, or one continuous vertical strip. */
enum class GalleryMode(val label: String) { Grid("Grid"), Reader("Reader") }

private val TopBarHeight = 64.dp

/** The width of the back icon button (the standard 48 dp touch target). */
private val BackControlSize = 48.dp

/**
 * The status bar height, kept at the largest value seen. When the system hides the status bar its inset drops to
 * zero; using the stable value for the content's top padding means the page does not shift when that happens.
 */
@Composable
private fun stableStatusBarInset(): Dp {
    val current = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var stable by remember { mutableStateOf(current) }
    if (current > stable) stable = current
    return stable
}

/**
 * A project's page. The top bar is an overlay above the content rather than a slot that reserves space, and the
 * content has a constant top padding, so hiding the bar never moves the content. With [autoHideBars] (phones),
 * scrolling down hides the top bar and, through [onBarsVisibleChange], the system status bar; scrolling up, or being
 * at the top, shows them again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectContent(
    project: ProjectDto,
    imageUrl: ImageUrl,
    onBack: () -> Unit,
    onOpenImage: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    mode: GalleryMode = GalleryMode.Grid,
    onModeChange: (GalleryMode) -> Unit = {},
    showBackControl: Boolean = true,
    autoHideBars: Boolean = false,
    onBarsVisibleChange: (Boolean) -> Unit = {},
) {
    val bars = remember { BarsVisibility() }
    val gridState = rememberLazyGridState()
    val readerState = rememberLazyListState()
    val contentTop = TopBarHeight + stableStatusBarInset()

    if (autoHideBars) {
        LaunchedEffect(mode) {
            snapshotFlow {
                when (mode) {
                    GalleryMode.Grid -> gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                    GalleryMode.Reader -> readerState.firstVisibleItemIndex == 0 && readerState.firstVisibleItemScrollOffset == 0
                }
            }.collect { bars.reportAtTop(it) }
        }
    }
    val barsShown = !autoHideBars || bars.visible
    if (autoHideBars) {
        LaunchedEffect(barsShown) { onBarsVisibleChange(barsShown) }
        // Never leave the system bars hidden behind: restore them when this page goes away.
        DisposableEffect(Unit) { onDispose { onBarsVisibleChange(true) } }
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(if (autoHideBars) Modifier.nestedScroll(bars.connection) else Modifier),
    ) {
        // On wide screens the header and the grid sit in a centered column; the scroll areas stay full width.
        val column = contentColumnWidth(maxWidth)
        when (mode) {
            GalleryMode.Grid -> ProjectGrid(project, imageUrl, onOpenImage, mode, onModeChange, gridState, contentTop, (maxWidth - column) / 2)
            GalleryMode.Reader -> ProjectReader(project, imageUrl, mode, onModeChange, readerState, contentTop, column)
        }
        AnimatedVisibility(
            visible = barsShown,
            modifier = Modifier.align(Alignment.TopStart),
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            CenterAlignedTopAppBar(
                title = { Text(project.title, maxLines = 1) },
                // The bar centres the title in the space between its two sides, so both sides always get a slot of the
                // same size: the back control (or an empty slot where there is none, as on the web) and an empty
                // one opposite. Otherwise a long title sits off centre by half the control's width.
                navigationIcon = {
                    if (showBackControl) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Spacer(Modifier.size(BackControlSize))
                    }
                },
                actions = { Spacer(Modifier.size(BackControlSize)) },
            )
        }
    }
}

@Composable
private fun ProjectHeader(project: ProjectDto, mode: GalleryMode, onModeChange: (GalleryMode) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // The block is centered in the column, but the date and summary text is left-aligned for readability.
        Text(
            formatDate(project.createdAt),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        if (project.summary.isNotBlank()) {
            LongText(
                project.summary,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        GalleryModeSwitch(mode, onModeChange, Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun GalleryModeSwitch(mode: GalleryMode, onModeChange: (GalleryMode) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier) {
        GalleryMode.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = entry == mode,
                onClick = { onModeChange(entry) },
                shape = SegmentedButtonDefaults.itemShape(index, GalleryMode.entries.size),
            ) { Text(entry.label) }
        }
    }
}

@Composable
private fun ProjectGrid(
    project: ProjectDto,
    imageUrl: ImageUrl,
    onOpenImage: (index: Int) -> Unit,
    mode: GalleryMode,
    onModeChange: (GalleryMode) -> Unit,
    state: LazyGridState,
    topPadding: Dp,
    sideMargin: Dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        state = state,
        modifier = Modifier.fillMaxWidth().testTag("grid"),
        contentPadding = PaddingValues(start = sideMargin + 16.dp, end = sideMargin + 16.dp, bottom = 16.dp, top = topPadding + 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { ProjectHeader(project, mode, onModeChange) }
        if (project.images.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { NoImagesMessage() }
        }
        itemsIndexed(project.images, key = { _, image -> image.id }) { index, image ->
            PortfolioImage(
                url = imageUrl(image, true),
                contentDescription = "${project.title} image ${index + 1}",
                modifier = Modifier.aspectRatio(1f).clickable { onOpenImage(index) },
            )
        }
    }
}

/**
 * The images as one continuous strip, each at the full width, touching the next with no gap and no cropping.
 * Each image's height comes from its stored proportions, so the page does not jump as images load.
 */
@Composable
private fun ProjectReader(
    project: ProjectDto,
    imageUrl: ImageUrl,
    mode: GalleryMode,
    onModeChange: (GalleryMode) -> Unit,
    state: LazyListState,
    topPadding: Dp,
    column: Dp,
) {
    // The images always span the full screen width; only the header above them sits in the centered column.
    LazyColumn(
        Modifier.fillMaxWidth().testTag("reader"),
        state = state,
        contentPadding = PaddingValues(top = topPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { Box(Modifier.width(column).padding(16.dp)) { ProjectHeader(project, mode, onModeChange) } }
        if (project.images.isEmpty()) {
            item { Box(Modifier.width(column).padding(horizontal = 16.dp)) { NoImagesMessage() } }
        }
        itemsIndexed(project.images, key = { _, image -> image.id }) { index, image ->
            PortfolioImage(
                url = imageUrl(image, false),
                contentDescription = "${project.title} image ${index + 1}",
                modifier = Modifier.fillMaxWidth().aspectRatio(image.width.toFloat() / image.height.coerceAtLeast(1)),
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

@Composable
private fun NoImagesMessage() {
    Text(
        "No images yet.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Full-screen image viewer showing originals. Swipe, or press the left and right arrow keys, to move between
 * images; the arrow keys stop at the first and last image. The viewer takes keyboard focus when it opens, so the
 * keys work without clicking first.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageViewer(
    project: ProjectDto,
    startIndex: Int,
    imageUrl: ImageUrl,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, (project.images.size - 1).coerceAtLeast(0))) {
        project.images.size
    }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    /** Moves by [delta] images from where the pager is heading, so quick repeated presses keep going. */
    fun step(delta: Int): Boolean {
        val target = pager.targetPage + delta
        if (target !in 0 until project.images.size) return false
        scope.launch { pager.animateScrollToPage(target) }
        return true
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionRight -> step(+1)
                    Key.DirectionLeft -> step(-1)
                    else -> false
                }
            }
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            PortfolioImage(
                url = imageUrl(project.images[page], false),
                contentDescription = "${project.title} image ${page + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            "${pager.currentPage + 1} / ${project.images.size}",
            color = Color.White,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        )
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}
