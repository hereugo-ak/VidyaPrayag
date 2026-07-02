package com.littlebridge.enrollplus.ui.v2.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored

enum class VSnackbarTone { Success, Error, Info, Warning }

private fun VSnackbarTone.toIcon(): ImageVector = when (this) {
    VSnackbarTone.Success -> VIcons.Check
    VSnackbarTone.Error -> VIcons.AlertCircle
    VSnackbarTone.Warning -> VIcons.AlertTriangle
    VSnackbarTone.Info -> VIcons.BookOpen
}

/**
 * VSnackbar — a bottom-anchored transient message bar with an icon, message,
 * optional action label, and auto-dismiss support.
 *
 * Uses the VTheme ink/card palette. The [visible] flag drives enter/exit
 * animations (slide up + fade). The host is responsible for calling
 * [onDismiss] after a delay (typically 3–4s via LaunchedEffect).
 */
@Composable
fun VSnackbar(
    message: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tone: VSnackbarTone = VSnackbarTone.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = VTheme.colors
    val iconColor = when (tone) {
        VSnackbarTone.Success -> c.successInk
        VSnackbarTone.Error -> c.dangerInk
        VSnackbarTone.Warning -> c.warningInk
        VSnackbarTone.Info -> c.accent
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.ink)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = tone.toIcon(),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = message,
                style = VTheme.type.body.colored(c.cream),
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = {
                    onAction()
                    onDismiss()
                }) {
                    Text(actionLabel, color = c.accent)
                }
            }
        }
    }
}
