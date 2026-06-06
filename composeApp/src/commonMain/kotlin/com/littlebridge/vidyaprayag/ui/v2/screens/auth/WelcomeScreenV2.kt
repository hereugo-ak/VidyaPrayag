package com.littlebridge.vidyaprayag.ui.v2.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VLogo
import com.littlebridge.vidyaprayag.ui.v2.components.VLogoTone
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * WelcomeScreenV2 — premium splash/welcome, translated from Auth.tsx → `Splash`.
 *
 * A teal hero panel carrying the bridge logo + wordmark + tagline, then a lifted lavender
 * sheet with the two entry CTAs. Pure composition over Phase-1 `V*` primitives.
 */
@Composable
fun WelcomeScreenV2(
    onGetStarted: () -> Unit,
    onHaveAccount: () -> Unit,
    modifier: Modifier = Modifier,
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
                .height(420.dp)
                .background(c.teal),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(d.md),
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(c.card.copy(alpha = 0.16f))
                        .padding(d.xl),
                ) {
                    VLogo(size = 96.dp, tone = VLogoTone.White)
                }
                Text(
                    "VidyaSetu",
                    style = VTheme.type.h1.colored(c.card),
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Bridging gaps for a glorious future",
                    style = VTheme.type.body.colored(c.card.copy(alpha = 0.92f)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp).padding(horizontal = d.lg),
                )
            }
        }

        // ── Lifted CTA sheet ───────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.background, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .padding(horizontal = d.lg, vertical = d.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.md),
        ) {
            Text(
                "240+ schools · 38k parents",
                style = VTheme.type.caption.colored(c.ink2),
                textAlign = TextAlign.Center,
            )
            Text(
                "Welcome aboard",
                style = VTheme.type.h2.colored(c.navy),
                textAlign = TextAlign.Center,
            )
            Text(
                "Sign in to connect with your child's school, or explore as a new family.",
                style = VTheme.type.body.colored(c.ink2),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(d.sm))
            VButton(
                text = "Get started",
                onClick = onGetStarted,
                full = true,
                size = VButtonSize.Lg,
                tone = VButtonTone.Teal,
                soft = false,
            )
            VButton(
                text = "I already have an account",
                onClick = onHaveAccount,
                full = true,
                size = VButtonSize.Lg,
                tone = VButtonTone.Navy,
                variant = VButtonVariant.Secondary,
            )
            Text(
                "By continuing you agree to our Terms & Privacy Policy",
                style = VTheme.type.label.colored(c.ink3),
                textAlign = TextAlign.Center,
            )
        }
    }
}
