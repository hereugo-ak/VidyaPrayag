package com.littlebridge.vidyaprayag.ui.v2.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme

/** Deterministic, soft pastel palette — mirrors primitives.tsx `VAvatar` palette. */
private val AvatarPalette = listOf(
    Color(0xFFC8DEFF),
    Color(0xFFA8E6CF),
    Color(0xFFFFD4A3),
    Color(0xFFFFADA8),
    Color(0xFFE0D4FF),
    Color(0xFFD4F3FF),
)

/** Initials from a display name: first letter of the first two words, upper-cased. */
private fun initialsOf(name: String): String =
    name.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

/** Stable background color picked by summing char codes — same name always yields same color. */
private fun avatarColorFor(name: String): Color {
    val hash = name.sumOf { it.code }
    return AvatarPalette[((hash % AvatarPalette.size) + AvatarPalette.size) % AvatarPalette.size]
}

/**
 * VAvatar — circular initials chip with a deterministic per-name color, or an image when [src]
 * is supplied. Translated from primitives.tsx → `VAvatar`.
 */
@Composable
fun VAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    src: String? = null,
    ring: Boolean = false,
) {
    val bg = avatarColorFor(name)
    var mod = modifier
        .size(size)
        .clip(CircleShape)
        .background(bg)
    if (ring) {
        mod = mod.border(BorderStroke(3.dp, VTheme.colors.card), CircleShape)
    }

    Box(mod, contentAlignment = Alignment.Center) {
        if (src != null) {
            AsyncImage(
                model = src,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            Text(
                text = initialsOf(name),
                style = TextStyle(
                    color = Color(0xFF080808),
                    fontSize = (size.value * 0.36f).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VTheme.type.uiFamily,
                ),
            )
        }
    }
}
