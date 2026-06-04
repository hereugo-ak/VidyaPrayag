/*
 * File: PremiumButton.kt  (commonMain)
 * Module: ui.components
 *
 * Premium, iOS-style button system for VidyaPrayag. Inspired by top-tier
 * dribbble button designs but customized to the brand palette (deep navy
 * primary #031632 + emerald secondary #006C49).
 *
 * Each button renders with REAL depth, not a flat M3 fill:
 *   - a soft vertical gradient body (lighter top → brand bottom),
 *   - a faint top "gloss" highlight (the glassy iOS sheen),
 *   - a layered drop shadow that lifts the button off the surface,
 *   - a tactile spring press (scale + shadow collapse) on touch,
 *   - a smooth disabled state.
 *
 * Use [PremiumButton] (filled), [PremiumTonalButton] (soft emerald),
 * and [PremiumOutlineButton] (glass border) everywhere on the school side
 * instead of bare M3 Buttons so the whole surface feels consistent.
 */
package com.littlebridge.vidyaprayag.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared shape + height tokens so every premium button matches. */
private val ButtonShape = RoundedCornerShape(18.dp)
private val ButtonHeight = 56.dp

/**
 * Filled premium button — the primary CTA. Gradient navy body with a glossy
 * top sheen, lifted shadow, and a spring press.
 */
@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val scale = remember { Animatable(1f) }
    val isInteractive = enabled && !loading

    // Lighter top + brand bottom = a subtle 3D body.
    val topColor = lerp(containerColor, Color.White, 0.16f)
    val bottomColor = lerp(containerColor, Color.Black, 0.08f)
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .heightIn(min = ButtonHeight)
            .height(ButtonHeight)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .shadow(
                elevation = if (isInteractive) 14.dp else 0.dp,
                shape = ButtonShape,
                spotColor = containerColor.copy(alpha = 0.55f),
                ambientColor = containerColor.copy(alpha = 0.35f)
            )
            .clip(ButtonShape)
            .background(
                brush = if (isInteractive) {
                    Brush.verticalGradient(listOf(topColor, containerColor, bottomColor))
                } else {
                    Brush.verticalGradient(listOf(disabledColor, disabledColor))
                }
            )
            // Glassy top sheen — the iOS highlight.
            .background(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (isInteractive) 0.22f else 0f),
                    0.5f to Color.Transparent,
                    1f to Color.Transparent
                ),
                shape = ButtonShape
            )
            .pressable(enabled = isInteractive, scale = scale, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        ButtonContent(
            text = text,
            loading = loading,
            icon = icon,
            contentColor = if (isInteractive) contentColor
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

/**
 * Soft tonal button — emerald container with low elevation. Good for
 * secondary actions that still need presence.
 */
@Composable
fun PremiumTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    val scale = remember { Animatable(1f) }
    val container = MaterialTheme.colorScheme.secondaryContainer
    val content = MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = modifier
            .height(ButtonHeight)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .shadow(if (enabled) 6.dp else 0.dp, ButtonShape, spotColor = container.copy(alpha = 0.5f))
            .clip(ButtonShape)
            .background(
                Brush.verticalGradient(
                    listOf(lerp(container, Color.White, 0.18f), container)
                )
            )
            .pressable(enabled = enabled, scale = scale, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        ButtonContent(text, loading = false, icon = icon, contentColor = content)
    }
}

/**
 * Glass-outline button — transparent body with a hairline brand border.
 * For tertiary actions ("Skip", "Cancel", "Learn more").
 */
@Composable
fun PremiumOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    val scale = remember { Animatable(1f) }
    val border by animateColorAsState(
        if (enabled) borderColor.copy(alpha = 0.45f) else borderColor.copy(alpha = 0.18f)
    )

    Box(
        modifier = modifier
            .height(ButtonHeight)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(ButtonShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            .border(BorderStroke(1.5.dp, border), ButtonShape)
            .pressable(enabled = enabled, scale = scale, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        ButtonContent(
            text,
            loading = false,
            icon = icon,
            contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.4f)
        )
    }
}

/* ------------------------------------------------------------------ */
/* internals                                                          */
/* ------------------------------------------------------------------ */

@Composable
private fun ButtonContent(
    text: String,
    loading: Boolean,
    icon: (@Composable () -> Unit)?,
    contentColor: Color
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        if (loading) {
            ButtonSpinner(color = contentColor)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                if (icon != null) icon()
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Small indeterminate spinner that matches the content color. */
@Composable
private fun ButtonSpinner(color: Color) {
    CircularProgressIndicator(
        color = color,
        strokeWidth = 2.5.dp,
        modifier = Modifier.height(22.dp)
    )
}

/**
 * Shared press handler: spring-scales down to 0.96 while held, springs back on
 * release, and fires [onClick] on tap. Ignored when [enabled] is false.
 */
private fun Modifier.pressable(
    enabled: Boolean,
    scale: Animatable<Float, *>,
    onClick: () -> Unit
): Modifier = this.pointerInput(enabled) {
    if (!enabled) return@pointerInput
    detectTapGestures(
        onPress = {
            scale.animateTo(
                0.96f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
            )
            tryAwaitRelease()
            scale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            )
        },
        onTap = { onClick() }
    )
}

