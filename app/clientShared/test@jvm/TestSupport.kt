package com.usuallycoding.portfolio.client

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText

/** Waits for [text] to appear, syncing with the UI between checks. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.awaitText(text: String, timeoutMs: Long = 10_000) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) {
        check(System.currentTimeMillis() < deadline) { "Timed out waiting for text \"$text\"" }
        waitForIdle()
        Thread.sleep(20)
    }
}
