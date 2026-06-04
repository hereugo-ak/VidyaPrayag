/*
 * File: NetworkImage.kt  (commonMain)
 * Module: ui.components
 *
 * A single, reusable network-image composable that fixes the recurring UI
 * "glitch" of blank / broken image boxes. The previous code used bare
 * `AsyncImage(...)` calls with NO loading or error slot, so any URL that
 * failed (e.g. the HTTP 403 `lh3.googleusercontent.com/aida...` links or DNS
 * failures seen in the Android logs) rendered as an empty/garbled box.
 *
 * [NetworkImage] always shows:
 *   - a gentle shimmer skeleton while loading, and
 *   - a clean neutral fallback (icon on a surface tint) on error or when the
 *     model is null/blank.
 *
 * Use this everywhere instead of raw AsyncImage for remote images.
 */
package com.littlebridge.vidyaprayag.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Drop-in replacement for [coil3.compose.AsyncImage] with built-in loading and
 * error states so a failed/empty URL never shows as a broken box.
 *
 * @param model        the image source (URL string, etc.). Null/blank → fallback.
 * @param fallbackTint background tint used behind the fallback icon.
 */
@Composable
fun NetworkImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alpha: Float = 1f,
    fallbackTint: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val isBlank = (model as? String)?.isBlank() == true
    if (model == null || isBlank) {
        ImageFallback(modifier = modifier, tint = fallbackTint)
        return
    }

    val request = ImageRequest.Builder(LocalPlatformContext.current)
        .data(model)
        .crossfade(true)
        .build()

    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alpha = alpha,
        loading = {
            Box(modifier = Modifier.fillMaxSize().shimmerPlaceholder(cornerRadius = 0.dp))
        },
        error = {
            ImageFallback(modifier = Modifier.fillMaxSize(), tint = fallbackTint)
        },
        success = {
            SubcomposeAsyncImageContent()
        }
    )
}

/** Neutral fallback surface with a subtle broken-image glyph. */
@Composable
private fun ImageFallback(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Box(
        modifier = modifier.drawBehind { drawRect(tint) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.BrokenImage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(28.dp)
        )
    }
}
