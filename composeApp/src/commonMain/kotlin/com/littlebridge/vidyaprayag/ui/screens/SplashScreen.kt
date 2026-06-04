package com.littlebridge.vidyaprayag.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect

/**
 * Premium, staged launch animation:
 *  1. a soft navy radial-gradient backdrop fades in,
 *  2. an expanding glow ring + the logo spring-scales up and fades in,
 *  3. the wordmark reveals with an animated letter-spacing settle,
 *  4. the tagline soft-fades beneath it,
 *  5. a gentle perpetual float + breathing glow keep the screen alive.
 *
 * Pure Compose Multiplatform — no extra deps, runs identically everywhere.
 */
@Composable
fun SplashScreen() {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    // Staged one-shot progress drivers.
    val backdrop = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val textProgress = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        backdrop.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(700, delayMillis = 150, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }
    LaunchedEffect(Unit) {
        textProgress.animateTo(1f, tween(700, delayMillis = 450, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        taglineAlpha.animateTo(1f, tween(600, delayMillis = 750, easing = FastOutSlowInEasing))
    }

    // Perpetual ambient motion.
    val infinite = rememberInfiniteTransition(label = "ambient")
    val float by infinite.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "float"
    )
    val glow by infinite.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primary)
            .drawBehind {
                // Soft radial vignette that fades in with `backdrop`.
                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.42f)
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            lerp(primary, secondary, 0.22f).copy(alpha = 0.55f * backdrop.value),
                            primary.copy(alpha = 0f)
                        ),
                        center = center,
                        radius = size.maxDimension * 0.6f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer { translationY = float }
        ) {
            // Logo + expanding glow ring.
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .graphicsLayer { alpha = logoAlpha.value * 0.6f; scaleX = glow; scaleY = glow }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(secondary.copy(alpha = 0.45f), Color.Transparent)
                            )
                        )
                )
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier
                        .size(92.dp)
                        .scale(logoScale.value)
                        .graphicsLayer { alpha = logoAlpha.value }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "VidyaPrayag",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                // Letter spacing settles from wide → tight as it reveals.
                letterSpacing = (10f - 8f * textProgress.value).sp,
                modifier = Modifier.graphicsLayer {
                    alpha = textProgress.value
                    translationY = (1f - textProgress.value) * 14f
                }
            )

            Text(
                text = "Education with Trust",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .height(28.dp)
                    .graphicsLayer { alpha = taglineAlpha.value }
            )
        }
    }
}
