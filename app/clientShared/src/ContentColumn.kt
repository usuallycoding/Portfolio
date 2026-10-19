package com.usuallycoding.portfolio.client

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Screens at least this wide get the centered content column; narrower ones use the full width. */
internal val WideBreakpoint = 840.dp

/** On wide screens the column is this share of the screen width... */
private const val ColumnShare = 0.5f

/** ...but never narrower than this, so it does not jump from full width to a thin strip just above the breakpoint. */
private val ColumnMinWidth = 560.dp

/** Whether [screenWidth] is wide enough for the centered column and the wrapping social links. */
internal fun isWide(screenWidth: Dp): Boolean = screenWidth >= WideBreakpoint

/**
 * The width of the content column for a screen [screenWidth] wide: the full width below [WideBreakpoint],
 * otherwise half the screen but at least 560 dp (and never more than the screen). Everything on the profile
 * and the project page that sits in the column uses this one rule so it always lines up.
 */
internal fun contentColumnWidth(screenWidth: Dp): Dp =
    if (!isWide(screenWidth)) screenWidth else minOf(screenWidth, maxOf(screenWidth * ColumnShare, ColumnMinWidth))
