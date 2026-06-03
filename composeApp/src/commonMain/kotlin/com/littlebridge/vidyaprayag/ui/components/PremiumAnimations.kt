/*
 * File: PremiumAnimations.kt  (commonMain)
 * Module: ui.components
 *
 * Reusable, dependency-free "premium iOS-style" motion primitives used to make
 * the school onboarding / profile surfaces feel polished (report §8c "premium
 * iOS animations"). Everything here is pure Jetpack-Compose multiplatform —
 * no extra libraries, no platform code — so it runs identically on Android,
 * desktop, web and (future) iOS.
 *
 * Provided helpers:
 *   - Modifier.pressScale()         iOS-like spring press feedback for any tappable
 *   - Modifier.tappableScale()      self-contained tap-with-spring + onClick
 *   - AnimatedEntrance { }          staggered fade + slide-up on first composition
 *   - AnimatedVisibilityFade { }    smooth cross-fade + expand for conditional UI
 *   - Modifier.shimmerPlaceholder() gradient shimmer for loading skeletons
 *   - Modifier.gentleFloat()        subtle perpetual hover for hero elements
 *   - ShimmerBox                    ready-made skeleton row
 *   - PulsingDot                    breathing live/status indicator
 *   - staggerDelay(index)           convenience for list stagger timing
 */
package com.littlebridge.vidyaprayag.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds an iOS-style "press to shrink" spring feedback to any composable. Scales
 * down toward [pressedScale] while held, then springs back on release. Uses an
 * [InteractionSource] so it composes cleanly with `clickable`.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(isPressed) {
        scale.animateTo(
            targetValue = if (isPressed) pressedScale else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * Self-contained tap-with-spring: scales down on press, springs back, and fires
 * [onClick] on tap. Handy when you don't already have an interaction source.
 */
@Composable
fun Modifier.tappableScale(
    pressedScale: Float = 0.95f,
    onClick: () -> Unit
): Modifier {
    val scale = remember { Animatable(1f) }
    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    scale.animateTo(
                        pressedScale,
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
}

/**
 * Wraps content with a one-shot fade + slide-up entrance (iOS "soft reveal").
 * [delayMillis] enables list staggering — pass `index * 60` for a cascade.
 * A subtle scale-from-98% is layered in for a richer, more tactile reveal.
 */
@Composable
fun AnimatedEntrance(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    slideOffsetY: Float = 24f,
    durationMillis: Int = 440,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        )
    }
    Box(
        modifier = modifier.graphicsLayer {
            val p = progress.value
            alpha = p
            translationY = (1f - p) * slideOffsetY
            // Gentle scale-in (0.98 → 1.0) for a soft, premium "settle".
            val s = 0.98f + 0.02f * p
            scaleX = s
            scaleY = s
        }
    ) {
        content()
    }
}

/**
 * Smooth conditional reveal: fade + vertical expand in, fade + shrink out.
 * Use for error banners, inline tips, expandable sections — anything that
 * appears/disappears in response to state. Easier than hand-rolling
 * AnimatedVisibility everywhere and keeps the motion language consistent.
 */
@Composable
fun AnimatedVisibilityFade(
    visible: Boolean,
    modifier: Modifier = Modifier,
    durationMillis: Int = 280,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(durationMillis, easing = FastOutSlowInEasing)) +
            expandVertically(tween(durationMillis, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(durationMillis, easing = FastOutSlowInEasing)) +
            shrinkVertically(tween(durationMillis, easing = FastOutSlowInEasing))
    ) {
        content()
    }
}

/**
 * Gradient shimmer skeleton — drop onto a sized Box while content loads to give
 * the premium "content is coming" feel instead of a bare spinner.
 */
@Composable
fun Modifier.shimmerPlaceholder(
    cornerRadius: Dp = 12.dp,
    baseColor: Color = Color(0xFFE6E8EB),
    highlightColor: Color = Color(0xFFF5F6F7)
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-translate"
    )
    return this
        .clip(RoundedCornerShape(cornerRadius))
        .drawWithContent {
            drawContent()
            val width = size.width
            val brush = Brush.linearGradient(
                colors = listOf(baseColor, highlightColor, baseColor),
                start = Offset(translate * width, 0f),
                end = Offset((translate + 1f) * width, size.height)
            )
            drawRect(brush = brush)
        }
}

/**
 * Subtle perpetual vertical "float" for hero icons / illustrations. Gives an
 * idle screen a living, premium feel without being distracting.
 */
@Composable
fun Modifier.gentleFloat(
    amplitude: Float = 6f,
    durationMillis: Int = 2600
): Modifier {
    val transition = rememberInfiniteTransition(label = "float")
    val offset by transition.animateFloat(
        initialValue = -amplitude,
        targetValue = amplitude,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float-offset"
    )
    return this.graphicsLayer { translationY = offset }
}

/**
 * Convenience for staggered list entrances: returns a delay (ms) that grows
 * with [index] but is capped so long lists don't feel sluggish.
 */
fun staggerDelay(index: Int, step: Int = 60, max: Int = 480): Int =
    (index * step).coerceAtMost(max)

/** A ready-made shimmer block for skeleton rows. */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFFE6E8EB))
            .shimmerPlaceholder(cornerRadius = cornerRadius)
    )
}

/**
 * A gently breathing dot — perfect for "LIVE", "active", or unsaved-changes
 * status indicators. Pulses opacity + scale on an infinite ease loop.
 */
@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF22C55E),
    size: Dp = 10.dp
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse-value"
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                alpha = pulse
                scaleX = pulse
                scaleY = pulse
            }
            .clip(CircleShape)
            .background(color)
    )
}
