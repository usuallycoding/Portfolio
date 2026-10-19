package com.usuallycoding.portfolio.client

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** The social links show this many rows until the visitor expands them. */
internal const val CollapsedSocialRows = 5

/** The project cards are tall, so they fold away sooner: after this many rows. */
internal const val CollapsedProjectRows = 2

/** Whether [rowCount] rows are more than the collapsed view shows ([limit] rows), so an expand control is needed. */
internal fun isCollapsible(rowCount: Int, limit: Int): Boolean = rowCount > limit

/** The rows to show: all of them when expanded or when they fit within [limit], otherwise the first [limit]. */
internal fun <T> visibleRows(rows: List<T>, expanded: Boolean, limit: Int): List<T> =
    if (expanded || !isCollapsible(rows.size, limit)) rows else rows.take(limit)

/**
 * Which profile sections the visitor has expanded. It is held above the profile screen (the profile leaves the
 * composition while a project is open) so it survives opening a project and coming back, and it is saved with the
 * app's state. Every section starts collapsed.
 */
class ExpandedSections(socials: Boolean = false, projects: Boolean = false) {
    var socials by mutableStateOf(socials)
    var projects by mutableStateOf(projects)

    companion object {
        val Saver = listSaver<ExpandedSections, Boolean>(
            save = { listOf(it.socials, it.projects) },
            restore = { ExpandedSections(socials = it[0], projects = it[1]) },
        )
    }
}

@Composable
fun rememberExpandedSections(): ExpandedSections = rememberSaveable(saver = ExpandedSections.Saver) { ExpandedSections() }

/** The small expand / show less control: the standard Material arrow icon, labelled for screen readers. */
@Composable
internal fun ExpandToggle(
    expanded: Boolean,
    moreLabel: String,
    lessLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) lessLabel else moreLabel,
        )
    }
}

/** [ExpandToggle] centered in a full-width row, for lists that are one item per row. */
@Composable
internal fun CenteredExpandToggle(
    expanded: Boolean,
    moreLabel: String,
    lessLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ExpandToggle(expanded, moreLabel, lessLabel, onClick)
    }
}
