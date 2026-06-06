package com.littlebridge.vidyaprayag.ui.v2.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.shapeSm

// VTag active palette is fixed in the design (`primitives.tsx`): bg #dcf2ef, fg #006a60,
// border rgba(0,106,96,0.18) — independent of the night/warm tone remap. §2#6 / §13.10.
private val TagActiveBg = Color(0xFFDCF2EF)
private val TagActiveFg = Color(0xFF006A60)
private val TagActiveBorder = Color(0x2E006A60) // rgba(0,106,96,0.18)

/** Semantic tones for [VBadge]. Mirrors primitives.tsx `VBadge` tone union. */
enum class VBadgeTone { Arctic, Success, Warning, Danger, Neutral }

/**
 * VBadge — a pill status chip. Background is a soft tint; foreground is the matching ink.
 * Translated from primitives.tsx → `VBadge`.
 */
@Composable
fun VBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: VBadgeTone = VBadgeTone.Arctic,
) {
    val c = VTheme.colors
    val (bg, fg) = when (tone) {
        VBadgeTone.Arctic -> c.teal.copy(alpha = 0.16f) to c.tealDeep
        VBadgeTone.Success -> c.success.copy(alpha = 0.42f) to c.successInk
        VBadgeTone.Warning -> c.warning.copy(alpha = 0.55f) to c.warningInk
        VBadgeTone.Danger -> c.danger.copy(alpha = 0.55f) to c.dangerInk
        VBadgeTone.Neutral -> c.cream to c.ink2
    }
    Text(
        text = text,
        // React VBadge: 11 / 600 / 0.04em — NOT uppercase, NOT 0.08em tracking. §matrix.
        style = VTheme.type.label.colored(fg).copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.04.em,
        ),
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * VTag — a selectable filter chip (e.g. subject pills). Active state turns teal-tinted.
 * Translated from primitives.tsx → `VTag`.
 */
@Composable
fun VTag(
    text: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
) {
    val c = VTheme.colors
    // §2#6 / §matrix: active bg is the fixed #dcf2ef, fg #006a60, border rgba(0,106,96,.18).
    val bg = if (active) TagActiveBg else c.cream
    val fg = if (active) TagActiveFg else c.ink2
    val borderColor = if (active) TagActiveBorder else c.shadowTint.copy(alpha = 0.04f)

    var mod = modifier
        .clip(VTheme.dimens.shapeSm)
        .background(bg)
        .border(BorderStroke(1.dp, borderColor), VTheme.dimens.shapeSm)

    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        mod = mod.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    }

    Text(
        text = text,
        style = VTheme.type.caption.colored(fg).copy(fontWeight = FontWeight.SemiBold),
        maxLines = 1,
        modifier = mod.padding(contentPadding),
    )
}
