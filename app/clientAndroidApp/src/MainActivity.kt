package com.usuallycoding.portfolio

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.usuallycoding.portfolio.client.ClientApp
import com.usuallycoding.portfolio.client.ClientPlatform

/**
 * The dev machine's LAN address, for a physical device. The emulator reaches the host at 10.0.2.2 instead.
 * Plain HTTP hosts must also be listed in res/xml/network_security_config.xml.
 */
private const val SERVER_URL = "http://192.168.1.135:8090"

class MainActivity : ComponentActivity() {
    /**
     * The status and navigation bars pick their icon colors from the system theme, which is wrong when the user
     * overrides the app's theme. Re-apply them to match what the app is actually showing.
     */
    private fun matchSystemBarsToTheme(dark: Boolean) {
        val style = if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    /**
     * Hides or shows the status bar while a project is being read. A swipe from the top edge brings it back
     * temporarily; the layout does not move because the screens use the status bar's full height either way.
     */
    private fun setStatusBarVisible(visible: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (visible) controller.show(WindowInsetsCompat.Type.statusBars()) else controller.hide(WindowInsetsCompat.Type.statusBars())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ClientApp(
                SERVER_URL,
                onDarkThemeChanged = ::matchSystemBarsToTheme,
                platform = ClientPlatform.Mobile,
                onSystemBarsVisibleChanged = ::setStatusBarVisible,
            )
        }
    }
}

@Preview
@Composable
fun ClientAppAndroidPreview() {
    ClientApp(SERVER_URL)
}
