package com.usuallycoding.portfolio.ui

/** How the app picks light or dark: follow the device, or force one. */
enum class ThemePreference(val label: String) {
    System("System"),
    Light("Light"),
    Dark("Dark");

    /** Whether the dark theme should be shown, given the device's current setting. */
    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        System -> systemIsDark
        Light -> false
        Dark -> true
    }

    companion object {
        /** Reads a stored value; anything missing or unrecognised means [System]. */
        fun parse(stored: String?): ThemePreference = entries.firstOrNull { it.name == stored } ?: System
    }
}
