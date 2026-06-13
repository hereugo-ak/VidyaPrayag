package com.littlebridge.vidyaprayag.ui.v2.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.theme.VElevationLevel
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.shapeCard
import com.littlebridge.vidyaprayag.ui.v2.theme.vElevation

/**
 * VCard — the universal elevated surface.
 *
 * Translated from `UI screens/src/app/components/v/primitives.tsx` → `VCard`.
 * Renders the design's `--card` surface with a hairline border and a soft, low-opacity shadow.
 * In Night tone the shadow is suppressed (deep-black surfaces read flat by design).
 */
@Composable
fun VCard(
    modifier: Modifier = Modifier,
    padding: Dp = VTheme.dimens.md,
    shape: RoundedCornerShape = VTheme.dimens.shapeCard,
    background: Color = VTheme.colors.card,
    border: Boolean = true,
    elevated: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = VTheme.colors
    var base = modifier
    if (elevated) {
        base = base.vElevation(VElevationLevel.Card, radius = VTheme.dimens.radiusCard)
    }

    base = base
        .clip(shape)
        .background(background)

    if (border) {
        base = base.border(BorderStroke(1.dp, colors.hairline), shape)
    }

    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        base = base.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
    }

    Column(modifier = base.padding(padding), content = content)
}
