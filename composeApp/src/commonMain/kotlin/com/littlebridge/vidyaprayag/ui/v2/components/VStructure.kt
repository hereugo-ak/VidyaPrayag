package com.littlebridge.vidyaprayag.ui.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * VScreenScaffold — the phone-frame root for every screen.
 *
 * Translated from primitives.tsx → `PhoneFrame`. Centers a max-440dp column on wide displays
 * (desktop/web), paints the portal background, and stacks an optional [topBar] + scrolling
 * [content] + optional [bottomBar]. The content area fills remaining height.
 */
@Composable
fun VScreenScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val c = VTheme.colors
    Box(
        modifier.fillMaxSize().background(c.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = VTheme.dimens.maxContentWidth)
                .background(c.background),
        ) {
            topBar?.invoke()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                content(PaddingValues(VTheme.dimens.screenPadding))
            }
            bottomBar?.invoke()
        }
    }
}

/**
 * VEmptyState — a centered icon-in-circle + title + body + optional action for empty lists.
 * Translated from primitives.tsx → `VEmptyState`.
 */
@Composable
fun VEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    body: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val c = VTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(c.ink.copy(alpha = if (c.isNight) 0.06f else 0.04f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = c.ink3, modifier = Modifier.size(28.dp))
            }
        }
        Text(title, style = VTheme.type.h3.colored(c.ink), textAlign = TextAlign.Center)
        if (body != null) {
            Text(
                body,
                style = VTheme.type.caption.colored(c.ink2),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp),
            )
        }
        if (action != null) {
            Box(Modifier.padding(top = 8.dp)) { action() }
        }
    }
}

/**
 * VComingSoon — a "PREVIEW" card for not-yet-shipped features, with an optional preview slot
 * and a "Notify me" affordance. Translated from primitives.tsx → `VComingSoon`.
 */
@Composable
fun VComingSoon(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    preview: (@Composable () -> Unit)? = null,
    onNotifyMe: (() -> Unit)? = null,
) {
    val c = VTheme.colors
    VCard(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VBadge(text = "● PREVIEW", tone = VBadgeTone.Arctic)
            Text(title, style = VTheme.type.h3.colored(c.ink), textAlign = TextAlign.Center)
            Text(description, style = VTheme.type.caption.colored(c.ink2), textAlign = TextAlign.Center)
            preview?.invoke()
            val interaction = remember { MutableInteractionSource() }
            Row(
                Modifier
                    .padding(top = 8.dp)
                    .clip(CircleShape)
                    .background(c.cream)
                    .clickable(interactionSource = interaction, indication = null, enabled = onNotifyMe != null) { onNotifyMe?.invoke() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VStatusDot(color = c.teal, size = 6.dp)
                Text(
                    "Notify me when ready",
                    style = VTheme.type.caption.colored(c.ink2),
                )
            }
        }
    }
}
