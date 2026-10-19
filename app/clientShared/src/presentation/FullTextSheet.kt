package com.usuallycoding.portfolio.client.presentation

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the "Copied" confirmation stays visible. */
internal const val CopiedConfirmationMillis = 2_000L

/**
 * Puts text on the clipboard. This is the only place that touches the clipboard API, so the rest of the app does not
 * care which Compose clipboard API is available on a platform (and tests can substitute a fake clipboard).
 */
@Composable
internal fun rememberCopyToClipboard(): (String) -> Unit {
    @Suppress("DEPRECATION") // The newer suspend API needs platform-specific entries; this one works on every target.
    val clipboard = LocalClipboardManager.current
    return { text -> clipboard.setText(AnnotatedString(text)) }
}

/**
 * A bottom sheet with the whole text, a copy button and a close (X) button; closed by X, a tap outside, Escape, or
 * the system back. [copyToClipboard] is resolved by the caller, not here: the sheet is a separate window with its
 * own platform locals.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullTextSheet(text: String, onDismiss: () -> Unit, copyToClipboard: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        FullTextSheetContent(text, onClose = close, copyToClipboard = copyToClipboard)
    }
}

/**
 * What is inside the sheet: the close and copy buttons above the scrollable full text. Takes keyboard focus when it
 * appears so that Escape closes it (keyboards on the web and desktop).
 */
@Composable
internal fun FullTextSheetContent(text: String, onClose: () -> Unit, copyToClipboard: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    onClose()
                    true
                } else {
                    false
                }
            }
            .focusRequester(focusRequester)
            .focusable()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CopyButton(text, copyToClipboard)
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
        }
        // Long text scrolls inside the sheet instead of growing past the screen.
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
        )
    }
}

/** A copy icon button; after copying it shows a check mark and "Copied" for a moment. */
@Composable
internal fun CopyButton(text: String, copyToClipboard: (String) -> Unit) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(CopiedConfirmationMillis)
            copied = false
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (copied) Text("Copied", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        IconButton(onClick = {
            copyToClipboard(text)
            copied = true
        }) {
            Icon(
                if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                contentDescription = if (copied) "Copied" else "Copy text",
            )
        }
    }
}
