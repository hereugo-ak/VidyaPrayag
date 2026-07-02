package com.littlebridge.enrollplus.ui.v2.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * VidyaSetu design-system theme provider.
 *
 * Wraps content in four token CompositionLocals ([LocalVColors], [LocalVType],
 * [LocalVDimens], [LocalVThemeDef]) plus a Material 3 [ColorScheme] bridge so
 * that Material components also honour the active theme.
 *
 * Usage:
 *   `VTheme(themeDef = VThemeRegistry.resolve("dark")) { /* screens */ }`
 *
 * Access tokens anywhere via the [VTheme] accessor object:
 *   `VTheme.colors.tealDeep`, `VTheme.type.h1`, `VTheme.dimens.md`, `VTheme.themeDef`
 */

@Composable
fun VTheme(
    themeDef: VThemeDef = VThemeRegistry.defaultTheme,
    typography: VTypography = vidyaSetuTypography(),
    dimens: VDimens = DefaultVDimens,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colorScheme = materialColorSchemeFor(themeDef)
    val scaledTypography = remember(typography, fontScale) {
        typography.scaleBy(fontScale)
    }
    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(
            LocalVColors provides themeDef.colors,
            LocalVType provides scaledTypography,
            LocalVDimens provides dimens,
            LocalVThemeDef provides themeDef,
            LocalFontScale provides fontScale,
            content = content,
        )
    }
}

// ── CompositionLocals ────────────────────────────────────────────────────────

val LocalVColors: ProvidableCompositionLocal<VColors> =
    staticCompositionLocalOf { LightVColors }

val LocalVType: ProvidableCompositionLocal<VTypography> =
    staticCompositionLocalOf { defaultVTypography() }

val LocalVDimens: ProvidableCompositionLocal<VDimens> =
    staticCompositionLocalOf { DefaultVDimens }

val LocalVThemeDef: ProvidableCompositionLocal<VThemeDef> =
    staticCompositionLocalOf { VThemeRegistry.defaultTheme }

// UIX-032: Font scale factor (1.0 = default, 2.0 = 200% for accessibility)
val LocalFontScale: ProvidableCompositionLocal<Float> =
    staticCompositionLocalOf { 1f }

// ── Ergonomic accessor object ────────────────────────────────────────────────

object VTheme {
    val colors: VColors
        @Composable get() = LocalVColors.current
    val type: VTypography
        @Composable get() = LocalVType.current
    val dimens: VDimens
        @Composable get() = LocalVDimens.current
    val themeDef: VThemeDef
        @Composable get() = LocalVThemeDef.current
    val fontScale: Float
        @Composable get() = LocalFontScale.current
}

/** Helper: a TextStyle colored with an explicit color (keeps call sites terse). */
fun TextStyle.colored(color: Color): TextStyle = copy(color = color)

// ── Material 3 bridge ────────────────────────────────────────────────────────

/**
 * Derives a Material 3 [ColorScheme] from a [VThemeDef] so that any Material 3
 * component that reads `MaterialTheme.colorScheme` gets the correct colours.
 *
 * The canonical source of truth remains [VTheme.colors] — this bridge exists
 * solely for Material 3 components (e.g. `Slider`, `Switch`, `DatePicker`) that
 * cannot be easily styled with explicit colour parameters.
 */
@Composable
private fun materialColorSchemeFor(def: VThemeDef): androidx.compose.material3.ColorScheme {
    val c = def.colors
    return if (def.isDark) darkColorScheme(
        primary = c.accent,
        onPrimary = c.card,
        primaryContainer = c.accentDeep,
        onPrimaryContainer = c.card,
        secondary = c.accentSoft,
        onSecondary = c.card,
        background = c.background,
        onBackground = c.ink,
        surface = c.card,
        onSurface = c.ink,
        surfaceVariant = c.cream,
        onSurfaceVariant = c.ink2,
        outline = c.border1,
        outlineVariant = c.hairline,
        error = c.dangerInk,
        onError = c.card,
        errorContainer = c.danger,
        onErrorContainer = c.dangerInk,
    ) else lightColorScheme(
        primary = c.accent,
        onPrimary = c.card,
        primaryContainer = c.accentTint,
        onPrimaryContainer = c.accentDeep,
        secondary = c.accentSoft,
        onSecondary = c.card,
        background = c.background,
        onBackground = c.ink,
        surface = c.card,
        onSurface = c.ink,
        surfaceVariant = c.cream,
        onSurfaceVariant = c.ink2,
        outline = c.border1,
        outlineVariant = c.hairline,
        error = c.dangerInk,
        onError = c.card,
        errorContainer = c.danger,
        onErrorContainer = c.dangerInk,
    )
}
