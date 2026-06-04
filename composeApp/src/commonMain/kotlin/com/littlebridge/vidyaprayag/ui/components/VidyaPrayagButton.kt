package com.littlebridge.vidyaprayag.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/*
 * These three names are used across the whole app. They now delegate to the
 * premium iOS-style button system (PremiumButton.kt) so every screen gets the
 * same depth/gradient/spring treatment with zero per-screen changes and no
 * duplicated button styles competing with each other.
 */

@Composable
fun VidyaPrayagPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    PremiumButton(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        loading = loading
    )
}

@Composable
fun VidyaPrayagSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    PremiumTonalButton(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled
    )
}

@Composable
fun VidyaPrayagOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    PremiumOutlineButton(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        borderColor = borderColor,
        contentColor = contentColor
    )
}
