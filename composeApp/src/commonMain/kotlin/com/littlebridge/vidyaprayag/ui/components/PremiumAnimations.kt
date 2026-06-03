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
 *   - Modifier.pressScale()        iOS-like spring press feedback for any tappable
 *   - AnimatedEntrance { }         staggered fade + slide-up on first composition
 *   - Modifier.shimmerPlaceholder()gradient shimmer for loading skeletons
 *   - rememberStaggerDelay(index)  convenience for list stagger timing
 */
package com.littlebridge.vidyaprayag.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 */
@Composable
fun AnimatedEntrance(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    slideOffsetY: Float = 24f,
    durationMillis: Int = 420,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        visible = true
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
            alpha = progress.value
            translationY = (1f - progress.value) * slideOffsetY
        }
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
