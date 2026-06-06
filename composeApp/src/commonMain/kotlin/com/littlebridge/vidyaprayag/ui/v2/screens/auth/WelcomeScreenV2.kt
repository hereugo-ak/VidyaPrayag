package com.littlebridge.vidyaprayag.ui.v2.screens.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * WelcomeScreenV2 — premium splash/welcome, a pixel-faithful copy of `Auth.tsx → Splash`.
 *
 * Teal hero (paddingTop 80 / bottom 90, minHeight 440) with radial white glow + two cloud marks,
 * a 160dp glass logo cube carrying the bridge mark (drawn directly — no inner plate) under a halo
 * ring, the 30sp ExtraBold wordmark and tagline. A lavender sheet overlaps the hero by -32dp with a
 * top shadow, holds the social-proof avatar strip ("**240+ schools** · 38k parents"), the
 * "Welcome aboard" heading, and exactly **two** CTAs — soft-mint "Get started" with a trailing
 * arrow and a navy-secondary "I already have an account".
 */
@Composable
fun WelcomeScreenV2(
    onGetStarted: () -> Unit,
    onHaveAccount: () -> Unit,
    modifier: Modifier = Modifier,
    onRegisterSchool: () -> Unit = {},
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .widthIn(max = d.maxContentWidth),
    ) {
        // ── Teal hero ──────────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 440.dp)
                .background(c.teal)
                .drawBehind {
                    // radial white glow @ 50% 30%, fades to transparent ~55%
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.30f),
                            radius = size.maxDimension * 0.55f,
                        ),
                        radius = size.maxDimension * 0.55f,
                        center = Offset(size.width * 0.5f, size.height * 0.30f),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            // cloud marks
            CloudMark(
                Modifier.align(Alignment.TopStart).padding(start = 40.dp, top = 48.dp).size(width = 56.dp, height = 38.dp),
                alpha = 0.30f,
            )
            CloudMark(
                Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 80.dp).size(width = 46.dp, height = 32.dp),
                alpha = 0.25f,
            )

            Column(
                Modifier.padding(top = 80.dp, bottom = 90.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Logo cube — 160dp glass plate + halo ring, bridge drawn directly ──
                Box(
                    Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
                        .drawBehind {
                            // halo: 12dp soft white ring just outside the cube
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.10f),
                                topLeft = Offset(-12.dp.toPx(), -12.dp.toPx()),
                                size = Size(size.width + 24.dp.toPx(), size.height + 24.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx() + 12.dp.toPx(), 28.dp.toPx() + 12.dp.toPx()),
                                style = Stroke(width = 12.dp.toPx()),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BridgeMark(Modifier.size(100.dp), navyDot = c.navy)
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    "VidyaSetu",
                    style = VTheme.type.h1.colored(Color.White).copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bridging gaps for a glorious future",
                    style = VTheme.type.body.colored(Color.White.copy(alpha = 0.92f)).copy(fontSize = 15.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp).padding(horizontal = d.lg),
                )
            }
        }

        // ── Lifted CTA sheet (-32dp overlap, radius 32, top shadow) ──────────────
        Column(
            Modifier
                .fillMaxWidth()
                .offset(y = (-32).dp)
                .drawBehind {
                    // top shadow: 0 -10px 32px rgba(0,0,0,0.06)
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.06f),
                        topLeft = Offset(0f, -10.dp.toPx()),
                        size = Size(size.width, 32.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx(), 32.dp.toPx()),
                    )
                }
                .background(c.background, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .padding(horizontal = 24.dp)
                .padding(top = 40.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── social-proof avatar strip ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box {
                    val avatarColors = listOf(Color(0xFFA8E6CF), Color(0xFFFFD4A3), Color(0xFFC8DEFF))
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        avatarColors.forEach { ac ->
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(ac)
                                    .border(2.dp, c.background, CircleShape),
                            )
                        }
                    }
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.ink)) { append("240+ schools") }
                        withStyle(SpanStyle(color = c.ink2)) { append(" · 38k parents") }
                    },
                    style = VTheme.type.caption.colored(c.ink2),
                )
            }
            Spacer(Modifier.height(24.dp))

            Text("Welcome aboard", style = VTheme.type.h2.colored(c.navy), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in to connect with your child's school, or explore as a new family.",
                style = VTheme.type.body.colored(c.ink2).copy(fontSize = 14.sp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            VButton(
                text = "Get started",
                onClick = onGetStarted,
                full = true,
                size = VButtonSize.Lg,
                tone = VButtonTone.Teal,
                trailing = { Icon(VIcons.ArrowRight, contentDescription = null, modifier = Modifier.size(17.dp)) },
            )
            Spacer(Modifier.height(12.dp))
            VButton(
                text = "I already have an account",
                onClick = onHaveAccount,
                full = true,
                size = VButtonSize.Lg,
                tone = VButtonTone.Navy,
                variant = VButtonVariant.Secondary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = c.ink3)) { append("By continuing you agree to our ") }
                    withStyle(SpanStyle(color = c.tealDeep, fontWeight = FontWeight.SemiBold)) { append("Terms") }
                    withStyle(SpanStyle(color = c.ink3)) { append(" & ") }
                    withStyle(SpanStyle(color = c.tealDeep, fontWeight = FontWeight.SemiBold)) { append("Privacy Policy") }
                },
                style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A single faint stroked cloud, mirroring the splash SVG `M6 24 Q12 14 22 18 Q28 8 38 14 Q46 14 44 24 Z`. */
@Composable
private fun CloudMark(modifier: Modifier, alpha: Float) {
    Canvas(modifier) {
        val sx = size.width / 48f
        val sy = size.height / 32f
        fun x(v: Float) = v * sx
        fun y(v: Float) = v * sy
        val p = Path().apply {
            moveTo(x(6f), y(24f))
            quadraticTo(x(12f), y(14f), x(22f), y(18f))
            quadraticTo(x(28f), y(8f), x(38f), y(14f))
            quadraticTo(x(46f), y(14f), x(44f), y(24f))
            close()
        }
        drawPath(p, color = Color.White.copy(alpha = alpha), style = Stroke(width = 1.5f * sx))
    }
}

/**
 * BridgeMark — the splash hero's bridge logo, drawn directly (no plate) on a 56-unit viewBox:
 * white arc + deck + 3 cables, two white pillar caps and a navy center node at (28,22).
 */
@Composable
private fun BridgeMark(modifier: Modifier, navyDot: Color) {
    Canvas(modifier) {
        val s = size.width / 56f
        fun x(v: Float) = v * s
        fun y(v: Float) = v * s
        // arc M12 32 Q28 12 44 32
        val arc = Path().apply {
            moveTo(x(12f), y(32f))
            quadraticTo(x(28f), y(12f), x(44f), y(32f))
        }
        drawPath(arc, color = Color.White, style = Stroke(width = 3f * s, cap = StrokeCap.Round))
        // deck M10 40 H46
        drawLine(Color.White, Offset(x(10f), y(40f)), Offset(x(46f), y(40f)), strokeWidth = 3.5f * s, cap = StrokeCap.Round)
        // cables 18/28/38
        val cable = Color.White.copy(alpha = 0.78f)
        drawLine(cable, Offset(x(18f), y(32f)), Offset(x(18f), y(40f)), strokeWidth = 1.6f * s, cap = StrokeCap.Round)
        drawLine(cable, Offset(x(28f), y(22f)), Offset(x(28f), y(40f)), strokeWidth = 1.6f * s, cap = StrokeCap.Round)
        drawLine(cable, Offset(x(38f), y(32f)), Offset(x(38f), y(40f)), strokeWidth = 1.6f * s, cap = StrokeCap.Round)
        // pillar caps + navy center node
        drawCircle(Color.White, radius = 2.6f * s, center = Offset(x(12f), y(32f)))
        drawCircle(Color.White, radius = 2.6f * s, center = Offset(x(44f), y(32f)))
        drawCircle(navyDot, radius = 2.4f * s, center = Offset(x(28f), y(22f)))
    }
}
