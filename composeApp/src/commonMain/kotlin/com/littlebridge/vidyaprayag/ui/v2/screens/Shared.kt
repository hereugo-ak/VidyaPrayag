package com.littlebridge.vidyaprayag.ui.v2.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared helpers for the `ui/v2` screen layer.
 *
 * Kept tiny and dependency-free so every portal screen can compose from the same vocabulary
 * (state collection, section headers, a portal top-bar) without re-importing the same boilerplate.
 */

/** Terse [StateFlow] collection used across all v2 screens (wraps Compose's [collectAsState]). */
@Composable
fun <T> StateFlow<T>.collectAsStateV2(): State<T> = collectAsState()

/** A consistent ALL-CAPS section header + optional trailing action, used inside scroll content. */
@Composable
fun VSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val c = VTheme.colors
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = VTheme.type.label.colored(c.ink3))
        action?.invoke()
    }
}

/** Portal greeting bar: avatar + name + subtitle. Reused by Parent / Teacher / Admin home tabs. */
@Composable
fun VPortalHeader(
    name: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    photoUrl: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = VTheme.colors
    Row(
        modifier.fillMaxWidth().padding(vertical = VTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.md),
    ) {
        VAvatar(name = name.ifBlank { "?" }, src = photoUrl, ring = true)
        Column(Modifier.weight(1f)) {
            Text(subtitle, style = VTheme.type.caption.colored(c.ink3), textAlign = TextAlign.Start)
            Text(name.ifBlank { "—" }, style = VTheme.type.h3.colored(c.ink))
        }
        trailing?.invoke()
    }
}
