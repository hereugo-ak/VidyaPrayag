package com.littlebridge.enrollplus.ui.v2.theme

import androidx.compose.runtime.Composable

/**
 * Adapts the platform's system bars (status bar, navigation bar) to match the
 * current theme. On Android this calls `enableEdgeToEdge` with the appropriate
 * `SystemBarStyle` based on whether the active theme is dark. On other platforms
 * (iOS, web, desktop) this is a no-op.
 *
 * Call this inside the [VTheme] content block so it recomposes when the theme
 * changes.
 */
@Composable
expect fun VStatusBarAdapter(isDark: Boolean)
