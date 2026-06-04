package com.littlebridge.vidyaprayag.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Premium surface card. Same public signature as before (drop-in), now with:
 *   - a soft top→bottom gradient on the surface (barely-there depth),
 *   - a layered, tinted drop shadow (lifts off the background like iOS),
 *   - a hairline highlight border.
 *
 * Keeping the signature identical means every existing screen that already
 * uses VidyaPrayagCard inherits the premium look with zero changes and no
 * competing card components.
 */
@Composable
fun VidyaPrayagCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Int = 8,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    // Gentle vertical gradient: a touch lighter at the top, base at the bottom.
    val top = lerp(backgroundColor, Color.White, 0.04f)
    val bottom = backgroundColor
    val hairline = border ?: BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation.dp,
                shape = shape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(hairline, shape)
    ) {
        content()
    }
}
