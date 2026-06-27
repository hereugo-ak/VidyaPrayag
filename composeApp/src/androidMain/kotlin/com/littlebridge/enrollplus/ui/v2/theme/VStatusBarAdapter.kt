package com.littlebridge.enrollplus.ui.v2.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation — calls `enableEdgeToEdge` with light or dark
 * `SystemBarStyle` based on the active theme. Uses `DisposableEffect` so the
 * bars are re-applied whenever the theme changes.
 */
@Composable
actual fun VStatusBarAdapter(isDark: Boolean) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return

    DisposableEffect(isDark) {
        activity.enableEdgeToEdge(
            statusBarStyle = if (isDark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                )
            },
            navigationBarStyle = if (isDark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                )
            },
        )
        onDispose { }
    }
}
