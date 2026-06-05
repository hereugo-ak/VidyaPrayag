package com.littlebridge.vidyaprayag.ui.v2.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing (base-4) + border radii, from design `theme.css` @theme + rebuild plan §3.5.
 * Single source for all geometry so screens never hardcode dp values.
 */
@Immutable
data class VDimens(
    // Spacing scale
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
    val xxxl: Dp = 64.dp,

    // Screen padding
    val screenPadding: Dp = 16.dp,

    // Radii
    val radiusSm: Dp = 6.dp,    // chip / tag
    val radiusMd: Dp = 10.dp,   // input
    val radiusLg: Dp = 14.dp,   // card
    val radiusXl: Dp = 20.dp,   // sheet / modal
    val radiusPill: Dp = 999.dp,

    // Frame
    val maxContentWidth: Dp = 440.dp,
)

val DefaultVDimens = VDimens()

// Convenient shape helpers (computed from radii)
val VDimens.shapeSm get() = RoundedCornerShape(radiusSm)
val VDimens.shapeMd get() = RoundedCornerShape(radiusMd)
val VDimens.shapeLg get() = RoundedCornerShape(radiusLg)
val VDimens.shapeXl get() = RoundedCornerShape(radiusXl)
val VDimens.shapePill get() = RoundedCornerShape(radiusPill)
