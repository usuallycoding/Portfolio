package com.usuallycoding.portfolio.client

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.ui.PortfolioImage
import com.usuallycoding.portfolio.ui.ThemeController
import com.usuallycoding.portfolio.ui.ThemePicker

/** Builds the URL to show an image: the small variant for lists and avatars, the original otherwise. */
typealias ImageUrl = (image: ImageDto?, thumbnail: Boolean) -> String?

private val AvatarSize = 112.dp

/** The cover keeps its 3:1 shape until it would be taller than this; on wider screens it is cropped instead. */
private val CoverMaxHeight = 240.dp

private const val SocialsMore = "Show more socials"
private const val SocialsLess = "Show fewer socials"

/** A link on a wide screen is at most this wide, so one very long address cannot take a whole line. */
private val LinkItemMaxWidth = 320.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeContent(
    data: HomeData,
    imageUrl: ImageUrl,
    onOpenLink: (SocialLinkDto) -> Unit,
    onOpenProject: (ProjectSummaryDto) -> Unit,
    modifier: Modifier = Modifier,
    themeController: ThemeController? = null,
    sections: ExpandedSections = rememberExpandedSections(),
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val wide = isWide(maxWidth)
        val column = contentColumnWidth(maxWidth)
        val columns = projectColumns(column)
        val projectRows = remember(data.projects, columns) { data.projects.chunked(columns) }
        val coverHeight = minOf(maxWidth / 3f, CoverMaxHeight)
        // The list scrolls across the whole width; its items are centered and, on wide screens, limited to the column.
        LazyColumn(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Box(Modifier.fillMaxWidth()) {
                    PortfolioImage(
                        url = imageUrl(data.profile.coverImage, false),
                        contentDescription = "Cover image",
                        modifier = Modifier.fillMaxWidth().height(coverHeight),
                    )
                    PortfolioImage(
                        url = imageUrl(data.profile.profileImage, true),
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = AvatarSize / 2)
                            .size(AvatarSize)
                            .clip(CircleShape),
                    )
                    if (themeController != null) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        ) { ThemePicker(themeController) }
                    }
                }
                Spacer(Modifier.height(AvatarSize / 2 + 12.dp))
            }
            item {
                Column(Modifier.width(column).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        data.profile.name,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (data.profile.description.isNotBlank()) {
                        // The block is centered in the column, but its lines are left-aligned: centered paragraphs are tiring to read.
                        LongText(
                            data.profile.description,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }
            }
            if (data.links.isNotEmpty()) {
                item(key = "socials-title") { SectionTitle("Socials", Modifier.width(column)) }
                if (wide) {
                    // On wide screens the links share one centered, wrapping row at their natural width. After five
                    // lines the flow itself shows the expand icon (or the show-less icon when everything is open).
                    item(key = "socials-flow") {
                        FlowRow(
                            Modifier.width(column).padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxLines = if (sections.socials) Int.MAX_VALUE else CollapsedSocialRows,
                            overflow = FlowRowOverflow.expandOrCollapseIndicator(
                                expandIndicator = {
                                    ExpandToggle(false, SocialsMore, SocialsLess, onClick = { sections.socials = true })
                                },
                                collapseIndicator = {
                                    ExpandToggle(true, SocialsMore, SocialsLess, onClick = { sections.socials = false })
                                },
                                minRowsToShowCollapse = CollapsedSocialRows + 1,
                            ),
                        ) {
                            data.links.forEach { link -> LinkItem(link, imageUrl, onClick = { onOpenLink(link) }) }
                        }
                    }
                } else {
                    // On smaller screens each link is a row: show the first five, with the toggle centered below.
                    items(visibleRows(data.links, sections.socials, CollapsedSocialRows), key = { "link-${it.id}" }) { link ->
                        LinkRow(link, imageUrl, onClick = { onOpenLink(link) })
                    }
                    if (isCollapsible(data.links.size, CollapsedSocialRows)) {
                        item(key = "socials-toggle") {
                            CenteredExpandToggle(
                                expanded = sections.socials,
                                moreLabel = SocialsMore,
                                lessLabel = SocialsLess,
                                onClick = { sections.socials = !sections.socials },
                            )
                        }
                    }
                }
            }
            item(key = "projects-title") {
                SectionTitle("Projects", Modifier.width(column))
                if (data.projects.isEmpty()) {
                    Text(
                        "No projects yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(column).padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            // Projects are laid out in rows of equal-width cards inside the column; the card count follows its width.
            items(visibleRows(projectRows, sections.projects, CollapsedProjectRows), key = { row -> "project-row-${row.first().id}" }) { row ->
                ProjectRow(row, columns, imageUrl, onOpenProject, Modifier.width(column))
            }
            if (isCollapsible(projectRows.size, CollapsedProjectRows)) {
                item(key = "projects-toggle") {
                    CenteredExpandToggle(
                        expanded = sections.projects,
                        moreLabel = "Show more projects",
                        lessLabel = "Show fewer projects",
                        onClick = { sections.projects = !sections.projects },
                        modifier = Modifier.width(column),
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

/** How many project cards fit side by side: as many as fit at the minimum card width, at least one. */
internal fun projectColumns(width: Dp): Int =
    maxOf(1, ((width.value - 2 * ProjectGutter.value + ProjectGap.value) / (ProjectCardMinWidth.value + ProjectGap.value)).toInt())

private val ProjectCardMinWidth = 160.dp
private val ProjectGutter = 16.dp
private val ProjectGap = 12.dp

@Composable
private fun ProjectRow(
    row: List<ProjectSummaryDto>,
    columns: Int,
    imageUrl: ImageUrl,
    onOpenProject: (ProjectSummaryDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    // One width, in whole pixels (rounded down), applied to every card. A fractional width would round up for the
    // first card and leave one pixel less for the next, so neighbouring cards would differ by a pixel.
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val cardWidth = with(density) {
            val available = constraints.maxWidth - (ProjectGutter * 2).roundToPx() - ProjectGap.roundToPx() * (columns - 1)
            (available / columns).toDp()
        }
        Row(
            Modifier.padding(horizontal = ProjectGutter, vertical = ProjectGap / 2).height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(ProjectGap),
        ) {
            row.forEach { project ->
                ProjectCard(project, imageUrl, onClick = { onOpenProject(project) }, modifier = Modifier.width(cardWidth).fillMaxHeight())
            }
        }
    }
}

/**
 * A compact project card. Every card has the same structure and reserves exactly three lines for the summary,
 * so all cards in a row, and across rows, have the same size whatever their content.
 */
@Composable
private fun ProjectCard(project: ProjectSummaryDto, imageUrl: ImageUrl, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.testTag("project-card-${project.id}").clickable(onClick = onClick)) {
        PortfolioImage(
            url = imageUrl(project.thumbnail, true),
            contentDescription = project.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        Column(Modifier.padding(12.dp)) {
            Text(project.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                formatDate(project.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                project.summary,
                style = MaterialTheme.typography.bodySmall,
                minLines = 3,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private val LinkIconSize = 48.dp

/** The icon, name and address of a link; used by both the one-per-row list and the wrapping row. */
@Composable
private fun LinkContent(link: SocialLinkDto, imageUrl: ImageUrl, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        PortfolioImage(
            url = imageUrl(link.icon, true),
            contentDescription = "${link.name} icon",
            modifier = Modifier.size(LinkIconSize).clip(RoundedCornerShape(10.dp)),
        )
        // Natural width, but allowed to shrink so a long address is cut with an ellipsis instead of overflowing.
        Column(Modifier.weight(1f, fill = false)) {
            Text(link.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                link.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A link on a smaller screen: one full-width row, its icon and text aligned to the start. */
@Composable
private fun LinkRow(link: SocialLinkDto, imageUrl: ImageUrl, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().testTag("link-${link.id}").clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        LinkContent(link, imageUrl)
    }
}

/** A link on a wide screen: a compact tappable item at its natural width. */
@Composable
private fun LinkItem(link: SocialLinkDto, imageUrl: ImageUrl, onClick: () -> Unit) {
    LinkContent(
        link,
        imageUrl,
        Modifier.widthIn(max = LinkItemMaxWidth).testTag("link-${link.id}").clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(8.dp),
    )
}
