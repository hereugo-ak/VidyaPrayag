/*
 * File: ComingSoonPill.kt  (commonMain)
 * Module: ui.components
 *
 * A small, honest "coming soon" status pill that REPLACES the old pattern of
 * permanently-disabled Buttons (onClick = { Unit }, enabled = false) with
 * awkward copy like "Forecast export pending". A greyed-out button that can
 * never be pressed is a UX glitch — it looks tappable but isn't, and it muddies
 * the premium feel.
 *
 * Instead we show a clearly non-interactive pill with a subtle pulsing dot,
 * so the user understands the feature is upcoming without being teased with a
 * dead control. Pure Compose Multiplatform.
 */
package com.littlebridge.vidyaprayag.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Non-interactive "Coming soon" status pill.
 *
 * @param label what's coming (e.g. "Report export", "Reminder delivery").
 * @param onLight set true when placed on a dark/hero surface so the text and
 *                chip tint flip to a light treatment.
 */
@Composable
fun ComingSoonPill(
    label: String = "Coming soon",
    modifier: Modifier = Modifier,
    onLight: Boolean = false
) {
    val accent = if (onLight) Color.White else MaterialTheme.colorScheme.secondary
    val bg = if (onLight) Color.White.copy(alpha = 0.14f)
             else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.30f)
    val textColor = if (onLight) Color.White.copy(alpha = 0.92f) else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PulsingDot(color = accent, size = 8.dp)
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        )
    }
}
