package com.littlebridge.enrollplus.ui.v2.theme

import androidx.compose.ui.graphics.vector.ImageVector
import com.littlebridge.enrollplus.ui.v2.components.VIcons

/**
 * VThemeRegistry — the single source of truth for all available themes.
 *
 * **Adding a new theme requires modifying ONLY this file.**
 *
 * Steps to add a theme:
 *  1. Define a [VColors] instance with all colour tokens (use [LightVColors] or
 *     [NightVColors] as a base and `.copy()` to override specific tokens).
 *  2. Add a [VThemeDef] entry to the [themes] list below.
 *  3. Done — the theme automatically appears in the theme picker, is selectable
 *     by users, persists correctly, and syncs to the server.
 *
 * No changes needed in VTheme.kt, PreferenceManager.kt, ThemePicker.kt,
 * NavGraphV2.kt, or any screen/component — all read from this registry.
 *
 * Theme IDs are **stable strings** that must never change once released (they
 * are persisted in DataStore and server-side). [displayName] can change freely.
 */
object VThemeRegistry {

    // ── Registered themes ─────────────────────────────────────────────────────
    // To add a new theme, add a VThemeDef entry below. That's the only change needed.

    val themes: List<VThemeDef> = listOf(
        VThemeDef(
            id = "light",
            displayName = "Light",
            description = "Crisp & bright",
            colors = LightVColors,
            isDark = false,
            icon = VIcons.Star,
        ),
        VThemeDef(
            id = "dark",
            displayName = "Dark",
            description = "Easy on the eyes",
            colors = NightVColors,
            isDark = true,
            icon = VIcons.Bookmark,
        ),
        VThemeDef(
            id = "warm",
            displayName = "Warm",
            description = "Cream & lavender",
            colors = LightVColors,
            isDark = false,
            icon = VIcons.Sparkles,
        ),
        // UIX-031: High-contrast accessibility theme
        VThemeDef(
            id = "high_contrast",
            displayName = "High Contrast",
            description = "WCAG AAA accessibility",
            colors = HighContrastVColors,
            isDark = false,
            icon = VIcons.Star,
        ),
        // ── Add new custom themes below this line ──────────────────────────────
        // VThemeDef(
        //     id = "ocean",
        //     displayName = "Ocean Blue",
        //     description = "Cool & calming",
        //     colors = LightVColors.copy(
        //         accent = Color(0xFF006994),
        //         accentSoft = Color(0xFF4DA6CB),
        //         accentDeep = Color(0xFF004D6B),
        //         accentTint = Color(0xFFE8F4FA),
        //     ),
        //     isDark = false,
        //     icon = VIcons.Water,
        // ),
    )

    // ── Lookup helpers ────────────────────────────────────────────────────────

    val byId: Map<String, VThemeDef> = themes.associateBy { it.id }

    val defaultTheme: VThemeDef = byId["light"] ?: themes.first()

    val defaultDarkTheme: VThemeDef = byId["dark"] ?: themes.first { it.isDark }

    /** Resolve a theme by its id string; falls back to [defaultTheme] if not found. */
    fun resolve(themeId: String): VThemeDef =
        byId[themeId] ?: defaultTheme

    /** Resolve the system-follow theme based on whether the OS is in dark mode. */
    fun resolveSystem(isSystemDark: Boolean): VThemeDef =
        if (isSystemDark) defaultDarkTheme else defaultTheme

    // ── Dynamic registration (for server-provided themes, e.g. school branding) ─

    private val dynamicThemes: MutableList<VThemeDef> = mutableListOf()

    /**
     * Register a theme at runtime (e.g. a school-branded theme fetched from the server).
     * The theme becomes immediately available in [allThemes] and the theme picker.
     */
    fun registerDynamic(def: VThemeDef) {
        if (byId[def.id] == null && dynamicThemes.none { it.id == def.id }) {
            dynamicThemes.add(def)
        }
    }

    /** All themes: compile-time [themes] + runtime-registered [dynamicThemes]. */
    val allThemes: List<VThemeDef> get() = themes + dynamicThemes

    /** Lookup including dynamic themes. */
    fun resolveInclusive(themeId: String): VThemeDef =
        allThemes.firstOrNull { it.id == themeId } ?: defaultTheme
}
