package com.littlebridge.vidyaprayag.ui.v2.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// Public API enums (mirror primitives.tsx Variant / Tone / size unions)
// ─────────────────────────────────────────────────────────────────────────────

enum class VButtonVariant { Primary, Secondary, Ghost, Destructive }
enum class VButtonTone { Navy, Teal, Sky, Peach, Lavender, Sand, Rose, Mint }
enum class VButtonSize { Sm, Md, Lg }

private enum class VButtonPhase { Idle, Loading, Success }

/** One tone's full color set — verbatim from primitives.tsx `tonePalette`. */
private data class TonePal(
    val bg: Color, val fg: Color, val shadow: Color,
    val soft: Color, val softFg: Color, val softBorder: Color, val softShadow: Color,
)

private fun tonePalette(tone: VButtonTone): TonePal = when (tone) {
    VButtonTone.Navy -> TonePal(
        bg = Color(0xFF26234D), fg = Color.White, shadow = Color(0x4D26234D),
        soft = Color(0xFFD8D2F1), softFg = Color(0xFF26234D), softBorder = Color(0x3826234D), softShadow = Color(0x2926234D))
    VButtonTone.Teal -> TonePal(
        bg = Color(0xFF006A60), fg = Color.White, shadow = Color(0x47006A60),
        soft = Color(0xFFB9E6DF), softFg = Color(0xFF005048), softBorder = Color(0x4D006A60), softShadow = Color(0x2E006A60))
    VButtonTone.Sky -> TonePal(
        bg = Color(0xFF3B78E7), fg = Color.White, shadow = Color(0x4D3B78E7),
        soft = Color(0xFFCDDCFF), softFg = Color(0xFF1A3F99), softBorder = Color(0x471F4FB5), softShadow = Color(0x2E1F4FB5))
    VButtonTone.Peach -> TonePal(
        bg = Color(0xFFE08A3C), fg = Color.White, shadow = Color(0x4DE08A3C),
        soft = Color(0xFFFAD0A8), softFg = Color(0xFF7A3F0A), softBorder = Color(0x478A4A10), softShadow = Color(0x2E8A4A10))
    VButtonTone.Lavender -> TonePal(
        bg = Color(0xFF7A6CF0), fg = Color.White, shadow = Color(0x4D7A6CF0),
        soft = Color(0xFFD6CDFF), softFg = Color(0xFF3527A8), softBorder = Color(0x473D2DB5), softShadow = Color(0x2E3D2DB5))
    VButtonTone.Sand -> TonePal(
        bg = Color(0xFFA88B5C), fg = Color.White, shadow = Color(0x47A88B5C),
        soft = Color(0xFFE8D8B6), softFg = Color(0xFF5A4626), softBorder = Color(0x4D6B5230), softShadow = Color(0x2E6B5230))
    VButtonTone.Rose -> TonePal(
        bg = Color(0xFFC14A6A), fg = Color.White, shadow = Color(0x47C14A6A),
        soft = Color(0xFFF6CAD6), softFg = Color(0xFF6E1730), softBorder = Color(0x477E1F3A), softShadow = Color(0x2E7E1F3A))
    VButtonTone.Mint -> TonePal(
        bg = Color(0xFF2F9B7A), fg = Color.White, shadow = Color(0x472F9B7A),
        soft = Color(0xFFBCE5D2), softFg = Color(0xFF0E4D36), softBorder = Color(0x4713573F), softShadow = Color(0x2E13573F))
}

/**
 * VButton — the universal action button.
 *
 * Translated from primitives.tsx → `VButton`. Supports 4 [variant]s × 8 [tone]s × soft/filled,
 * 3 [size]s, full-width, and the signature stateful flow: tapping a [stateful] button transitions
 * idle → loading (spinner) → success (check + [successLabel]) → idle automatically.
 */
@Composable
fun VButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: VButtonVariant = VButtonVariant.Primary,
    tone: VButtonTone = VButtonTone.Navy,
    size: VButtonSize = VButtonSize.Md,
    soft: Boolean = true,
    full: Boolean = false,
    enabled: Boolean = true,
    stateful: Boolean = false,
    loading: Boolean = false,
    successLabel: String = "Done",
    successDurationMs: Long = 1400L,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = VTheme.colors
    val pal = tonePalette(tone)

    val contentPadding: PaddingValues = when (size) {
        VButtonSize.Sm -> PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        VButtonSize.Md -> PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        VButtonSize.Lg -> PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    }
    val radius: Dp = if (size == VButtonSize.Sm) 10.dp else 12.dp
    val shape = RoundedCornerShape(radius)
    val iconSize = if (size == VButtonSize.Sm) 14.dp else 18.dp

    // Phase state machine.
    var phase by remember { mutableStateOf(VButtonPhase.Idle) }
    val activePhase = when {
        stateful -> phase
        loading -> VButtonPhase.Loading
        else -> VButtonPhase.Idle
    }
    LaunchedEffect(phase) {
        if (phase == VButtonPhase.Loading) {
            delay(650)
            phase = VButtonPhase.Success
            delay(successDurationMs)
            phase = VButtonPhase.Idle
        }
    }

    val isBusy = activePhase != VButtonPhase.Idle
    val isInteractive = enabled && !isBusy

    // Resolve fill / fg / border per variant.
    data class Skin(val bg: Color, val fg: Color, val border: Color?)
    val skin: Skin = when (variant) {
        VButtonVariant.Primary ->
            if (soft) Skin(pal.soft, pal.softFg, pal.softBorder)
            else Skin(pal.bg, pal.fg, null)
        VButtonVariant.Secondary -> Skin(c.card, c.ink, c.border2)
        VButtonVariant.Ghost -> Skin(Color.Transparent, if (soft) pal.softFg else c.ink2, null)
        VButtonVariant.Destructive -> Skin(c.dangerInk, Color.White, null)
    }

    var mod = modifier
    if (full) mod = mod.fillMaxWidth()
    if (variant != VButtonVariant.Ghost) {
        mod = mod.shadow(if (soft) 8.dp else 6.dp, shape, ambientColor = pal.softShadow, spotColor = pal.shadow)
    }
    mod = mod.clip(shape).background(skin.bg)
    skin.border?.let { mod = mod.border(BorderStroke(1.dp, it), shape) }

    val interaction = remember { MutableInteractionSource() }
    mod = mod.clickable(
        interactionSource = interaction,
        indication = null,
        enabled = isInteractive,
    ) {
        if (stateful && phase == VButtonPhase.Idle) {
            phase = VButtonPhase.Loading
            onClick()
        } else {
            onClick()
        }
    }

    Box(
        modifier = mod.padding(contentPadding).alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = activePhase,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "vbutton-phase",
        ) { p ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (p) {
                    VButtonPhase.Idle -> {
                        leading?.invoke()
                        Text(text, style = VTheme.type.h4.colored(skin.fg).copy(fontWeight = FontWeight.SemiBold))
                    }
                    VButtonPhase.Loading -> {
                        val transition = rememberInfiniteTransition(label = "spin")
                        val angle by transition.animateFloat(
                            initialValue = 0f, targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
                            label = "angle",
                        )
                        Icon(
                            imageVector = VIcons.Spinner,
                            contentDescription = null,
                            tint = skin.fg,
                            modifier = Modifier.size(iconSize).rotate(angle),
                        )
                    }
                    VButtonPhase.Success -> {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = skin.fg, modifier = Modifier.size(iconSize))
                        Text(successLabel, style = VTheme.type.h4.colored(skin.fg).copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }
    }
}
