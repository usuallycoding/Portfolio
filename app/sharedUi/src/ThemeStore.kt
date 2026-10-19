package com.usuallycoding.portfolio.ui

import com.russhwolf.settings.Settings

/** Remembers the theme choice on this device. Implementations must never throw. */
interface ThemeStore {
    fun load(): ThemePreference
    fun save(preference: ThemePreference)
}

/**
 * Stores the choice with the platform's key-value storage (SharedPreferences, NSUserDefaults, preferences
 * or localStorage). If storage is missing, blocked or fails, loading gives [ThemePreference.System] and
 * saving does nothing, so the app keeps working and follows the device.
 */
class SettingsThemeStore(private val settings: () -> Settings = { Settings() }) : ThemeStore {
    override fun load(): ThemePreference =
        try {
            ThemePreference.parse(settings().getStringOrNull(KEY))
        } catch (e: Throwable) {
            ThemePreference.System
        }

    override fun save(preference: ThemePreference) {
        try {
            settings().putString(KEY, preference.name)
        } catch (e: Throwable) {
            // Not remembered this time; the choice still applies until the app closes.
        }
    }

    private companion object {
        const val KEY = "theme_preference"
    }
}
