package com.usuallycoding.portfolio.client

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * Decides when the system status bar and the top bar are shown while reading. Scrolling down by more than
 * [hideAfter] pixels hides them, scrolling up by more than [showAfter] shows them again, and they are always
 * shown at the very top of the page. Small back-and-forth movements do not flip it.
 */
class BarsVisibility(
    private val hideAfter: Float = 48f,
    private val showAfter: Float = 12f,
) {
    private var hidden by mutableStateOf(false)
    private var atTop by mutableStateOf(true)
    private var downDistance = 0f
    private var upDistance = 0f

    /** Whether the bars should be on screen right now. */
    val visible: Boolean get() = atTop || !hidden

    /** [deltaY] is the content's movement: negative when the page scrolls down (content moves up), positive when it scrolls up. */
    fun onScroll(deltaY: Float) {
        when {
            deltaY < 0f -> {
                upDistance = 0f
                downDistance += -deltaY
                if (downDistance > hideAfter) hidden = true
            }
            deltaY > 0f -> {
                downDistance = 0f
                upDistance += deltaY
                if (upDistance > showAfter) hidden = false
            }
        }
    }

    /** Reports whether the page is scrolled to its very top, where the bars are always shown. */
    fun reportAtTop(value: Boolean) {
        atTop = value
        if (value) {
            hidden = false
            downDistance = 0f
            upDistance = 0f
        }
    }

    /** Observes scrolling of the content below it without taking part in it. */
    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            onScroll(available.y)
            return Offset.Zero
        }
    }
}
