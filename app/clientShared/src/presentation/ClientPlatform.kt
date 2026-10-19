package com.usuallycoding.portfolio.client.presentation

/**
 * Which kind of device the client runs on. The shells pass this in, so every rule that differs between
 * phones and the web is stated here instead of being scattered through the screens.
 */
enum class ClientPlatform(
    /** Pulling down on the profile or a project reloads it. */
    val pullToRefresh: Boolean,
    /** Scrolling down a project hides the status bar and top bar; scrolling up shows them. */
    val autoHideBars: Boolean,
    /** The project page has its own back control. On the web the browser's back is used instead. */
    val showsBackControl: Boolean,
    /** The system back gesture or button is handled by the app (not the browser). */
    val handlesSystemBack: Boolean,
) {
    Mobile(pullToRefresh = true, autoHideBars = true, showsBackControl = true, handlesSystemBack = true),
    Web(pullToRefresh = false, autoHideBars = false, showsBackControl = false, handlesSystemBack = false),
}
