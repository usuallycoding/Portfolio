package com.usuallycoding.portfolio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

val PortfolioLightColors = lightColorScheme()
val PortfolioDarkColors = darkColorScheme()

/**
 * The app's Material theme. With [ThemePreference.System] (the default) it follows the device and switches
 * when the device setting changes; [systemIsDark] is only overridden in tests.
 */
@Composable
fun PortfolioTheme(
    preference: ThemePreference = ThemePreference.System,
    systemIsDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (preference.isDark(systemIsDark)) PortfolioDarkColors else PortfolioLightColors,
        content = content,
    )
}
