package com.littlebridge.vidyaprayag.ui.v2.screens.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VBrandLogo
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.VMotion
import com.littlebridge.vidyaprayag.ui.v2.theme.VPortalTone
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.pressScale
import kotlinx.coroutines.launch

/**
 * CommonLandingScreenV2 — the app's first interactive surface for users with no session (PHASE 3).
 *
 * Rebuilt from scratch in the current design language (V* primitives, VColors tokens, VMotion). The
 * old `CommonLandingScreen` is referenced for **copy only**; its marketplace UI is discarded.
 *
 * Anatomy (premium, single-source brand):
 *  • teal hero — [VBrandLogo] + halo, wordmark, value tagline, radial glow.
 *  • lifted lavender sheet — a trust strip ("240+ schools · 38k parents"), the **two** role-entry
 *    cards (Parent → OTP auth, School / Administration → credential auth), and a privacy footnote.
 *
 * Exactly two entry points. Teachers do NOT get a CTA here — they sign in through the Admin path
 * (their credentials are minted inside a school's admin account; see PHASE 5).
 */
@Composable
fun CommonLandingScreenV2(
    onParent: () -> Unit,
    onAdmin: () -> Unit,
    modifier: Modifier = Modifier,
) = VTheme(tone = VPortalTone.Light) {
    val c = VTheme.colors
    val d = VTheme.dimens

    // Reveal ladder (mirrors the Welcome/Splash motion vocabulary).
    val logoScale = remember { Animatable(0.84f) }
    val logoAlpha = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordY = remember { Animatable(12f) }
    val sheetY = remember { Animatable(64f) }
    val sheetAlpha = remember { Animatable(0f) }
    val proofAlpha = remember { Animatable(0f) }
    val cardsAlpha = remember { Animatable(0f) }
    val cardsY = remember { Animatable(16f) }

    LaunchedEffect(Unit) {
        launch { logoScale.animateTo(1f, VMotion.springSoft) }
        launch { logoAlpha.animateTo(1f, tween(380)) }
        launch { wordAlpha.animateTo(1f, tween(420, delayMillis = 220)) }
        launch { wordY.animateTo(0f, tween(420, delayMillis = 220)) }
        launch { sheetY.animateTo(0f, VMotion.springSheet) }
        launch { sheetAlpha.animateTo(1f, tween(320, delayMillis = 140)) }
        launch { proofAlpha.animateTo(1f, tween(400, delayMillis = 320)) }
        launch { cardsAlpha.animateTo(1f, tween(450, delayMillis = 420)) }
        launch { cardsY.animateTo(0f, tween(450, delayMillis = 420)) }
    }

    val halo = rememberInfiniteTransition(label = "land-halo")
    val haloAlpha by halo.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                0f at 0; 0.6f at 1200; 0f at 2400
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "land-haloAlpha",
    )

    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .widthIn(max = d.maxContentWidth),
    ) {
        // ── Teal hero ────────────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 380.dp)
                .background(c.teal)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.32f),
                            radius = size.maxDimension * 0.55f,
                        ),
                        radius = size.maxDimension * 0.55f,
                        center = Offset(size.width * 0.5f, size.height * 0.32f),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.statusBarsPadding().padding(top = 64.dp, bottom = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VBrandLogo(
                    size = 132.dp,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        }
                        .drawBehind {
                            drawRoundRect(
                                color = Color.White.copy(alpha = (haloAlpha / 0.6f).coerceIn(0f, 1f) * 0.10f),
                                topLeft = Offset(-12.dp.toPx(), -12.dp.toPx()),
                                size = Size(size.width + 24.dp.toPx(), size.height + 24.dp.toPx()),
                                cornerRadius = CornerRadius(40.dp.toPx(), 40.dp.toPx()),
                                style = Stroke(width = 12.dp.toPx()),
                            )
                        },
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    "VidyaSetu",
                    style = VTheme.type.h1.colored(Color.White)
                        .copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = wordAlpha.value
                        translationY = wordY.value * density
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Education with trust. Progress with purpose.",
                    style = VTheme.type.body.colored(Color.White.copy(alpha = 0.92f)).copy(fontSize = 14.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .padding(horizontal = d.lg)
                        .graphicsLayer { alpha = wordAlpha.value },
                )
                Spacer(Modifier.height(18.dp))
                // Premium trust pill — frosted glass on the teal hero, reads as bank-grade reassurance.
                Row(
                    modifier = Modifier
                        .graphicsLayer { alpha = wordAlpha.value }
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(VIcons.ShieldCheck, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(
                        "Bank-grade encryption",
                        style = VTheme.type.caption.colored(Color.White).copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }

        // ── Lifted sheet ──────────────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .offset(y = (-32).dp)
                .graphicsLayer {
                    translationY = sheetY.value * density
                    alpha = sheetAlpha.value
                }
                .drawBehind {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.06f),
                        topLeft = Offset(0f, -10.dp.toPx()),
                        size = Size(size.width, 32.dp.toPx()),
                        cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()),
                    )
                }
                .background(c.background, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // trust strip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.graphicsLayer { alpha = proofAlpha.value },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    listOf(c.success, c.warning, Color(0xFFC8DEFF)).forEach { ac ->
                        Box(
                            Modifier.size(24.dp).clip(CircleShape).background(ac)
                                .border(2.dp, c.background, CircleShape),
                        )
                    }
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.ink)) { append("240+ schools") }
                        withStyle(SpanStyle(color = c.ink2)) { append(" · 38k parents trust us") }
                    },
                    style = VTheme.type.caption.colored(c.ink2),
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "How would you like to continue?",
                style = VTheme.type.h2.colored(c.navy),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Pick your role to sign in to the right experience.",
                style = VTheme.type.body.colored(c.ink2).copy(fontSize = 14.sp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            // ── the two role-entry cards ──
            Column(
                Modifier.fillMaxWidth().graphicsLayer {
                    alpha = cardsAlpha.value
                    translationY = cardsY.value * density
                },
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                RoleCard(
                    icon = VIcons.Users,
                    accent = c.teal,
                    accentInk = c.tealDeep,
                    title = "I'm a Parent",
                    body = "Track attendance, fees, homework and your child's progress — secured by a quick OTP sign-in.",
                    onClick = onParent,
                )
                RoleCard(
                    icon = VIcons.School,
                    accent = c.warmOrange,
                    accentInk = c.warmOrange,
                    title = "School / Administration",
                    body = "For school admins and teachers. Manage classes, staff, records and announcements.",
                    onClick = onAdmin,
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = c.ink3)) { append("By continuing you agree to our ") }
                    withStyle(SpanStyle(color = c.tealDeep, fontWeight = FontWeight.SemiBold)) { append("Terms") }
                    withStyle(SpanStyle(color = c.ink3)) { append(" & ") }
                    withStyle(SpanStyle(color = c.tealDeep, fontWeight = FontWeight.SemiBold)) { append("Privacy Policy") }
                },
                style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** A tappable role-entry card: tinted icon chip + title + supporting copy + chevron, with press-give. */
@Composable
private fun RoleCard(
    icon: ImageVector,
    accent: Color,
    accentInk: Color,
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val interaction = remember { MutableInteractionSource() }
    VCard(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        padding = 0.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Premium accent bar — a thin role-tinted gradient strip seating the card in its colour.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.45f))),
                    ),
            )
            Row(
                Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accentInk, modifier = Modifier.size(26.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = VTheme.type.h3.colored(c.ink))
                    Spacer(Modifier.height(3.dp))
                    Text(body, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp, lineHeight = 16.sp))
                }
                Icon(VIcons.ArrowRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
            }
        }
    }
}
