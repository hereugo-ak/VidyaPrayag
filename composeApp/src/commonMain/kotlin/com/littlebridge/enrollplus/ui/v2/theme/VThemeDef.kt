package com.littlebridge.enrollplus.ui.v2.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * VThemeDef — a self-contained theme definition.
 *
 * Replaces the old VPortalTone enum as the unit of theme selection. Each [VThemeDef] bundles
 * a [VColors] palette with metadata (display name, description, icon, dark flag)
 * so the theme picker UI can render it without any hardcoded references.
 *
 * All themes are registered in [VThemeRegistry] — the single file to modify
 * when adding a new theme.
 */
@Immutable
data class VThemeDef(
    val id: String,
    val displayName: String,
    val description: String,
    val colors: VColors,
    val isDark: Boolean,
    val icon: ImageVector,
)

/**
 * The user's theme *intent* — which theme to use, including system-follow.
 *
 * Stored as a string in preferences ("system" / "light" / "dark" / "custom").
 * When mode is [SYSTEM], the resolved theme depends on [androidx.compose.foundation.isSystemInDarkTheme].
 * When mode is [CUSTOM], the resolved theme is the one whose [VThemeDef.id] matches
 * the stored custom theme id.
 */
enum class VThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    CUSTOM("custom");

    companion object {
        fun fromString(value: String?): VThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}
