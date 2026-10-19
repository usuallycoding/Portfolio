package com.usuallycoding.portfolio.client.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/** The most rows of a long text shown on the page; the rest is in the bottom sheet. */
internal const val MaxTextRows = 10

/**
 * Text limited to [MaxTextRows] rows. Whether it is truncated comes from the real layout, so it follows the width,
 * the font size and the language, and decides whether an ellipsis is shown. The text can always be tapped or clicked
 * to open the full text, whether or not anything is hidden.
 */
@Composable
fun LongText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = Color.Unspecified,
) {
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    var showFullText by remember(text) { mutableStateOf(false) }
    val copyToClipboard = rememberCopyToClipboard()

    val current = layout
    val truncated = current?.hasVisualOverflow == true
    // Compose draws the "…" only when the cut falls inside a line. If the tenth row ends exactly at a line break
    // nothing marks the cut, so the ellipsis is drawn after that row.
    val ellipsisAfterLastRow = truncated && current != null && cutIsAtALineBreak(text, current)

    Box(modifier.clickable(onClickLabel = "Show full text", role = Role.Button) { showFullText = true }) {
        Text(
            text = text,
            style = style,
            color = color,
            textAlign = textAlign,
            maxLines = MaxTextRows,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout = it },
        )
        if (ellipsisAfterLastRow && current != null) {
            Text(
                "…",
                style = style,
                color = color,
                // Purely visual: screen readers already get the whole text from the text above.
                modifier = Modifier
                    .offset { IntOffset(current.getLineRight(MaxTextRows - 1).roundToInt(), current.getLineTop(MaxTextRows - 1).roundToInt()) }
                    .clearAndSetSemantics { testTag = EllipsisTag },
            )
        }
    }

    if (showFullText) FullTextSheet(text, onDismiss = { showFullText = false }, copyToClipboard = copyToClipboard)
}

/** Test tag of the ellipsis drawn when the cut falls at a line break. */
internal const val EllipsisTag = "long-text-ellipsis"

/**
 * Whether the text was cut exactly at a hard line break (the visible rows end where a newline is, or follow one).
 * Then Compose draws no "…", whereas a cut inside a line gets one automatically. (`isLineEllipsized` cannot be used:
 * it reports false on desktop even where the ellipsis is drawn.)
 */
internal fun cutIsAtALineBreak(text: String, layout: TextLayoutResult): Boolean {
    val end = layout.getLineEnd(MaxTextRows - 1)
    return text.getOrNull(end) == '\n' || text.getOrNull(end - 1) == '\n'
}
