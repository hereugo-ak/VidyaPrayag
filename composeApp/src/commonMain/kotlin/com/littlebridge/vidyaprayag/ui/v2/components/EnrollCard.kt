package com.littlebridge.vidyaprayag.ui.v2.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.pressScale

/**
 * ENROLL+ TEACHER PORTAL — shared flat card (Loop task P1-T2).
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * RELATIONSHIP TO [VCard]
 * ─────────────────────────────────────────────────────────────────────────────
 * The portal already has [VCard] — the universal *elevated* surface (navy-tinted
 * soft shadow). [EnrollCard] is the loop's deliberately DIFFERENT contract: a
 * **flat, border-defined** card with NO elevation shadow, and an explicit
 * `scale(0.98f)` press give on tap. It is the surface for the dense, scannable
 * teacher screens (gradebook rows, nudge cards, schedule pills) where stacked
 * soft shadows would muddy the rhythm — the design spec's `ElevationCard = 0.dp`
 * rule ("no shadow — use SurfaceSubtle border instead", PART 2 §SHAPE & ELEVATION).
 *
 * Every visual decision is justified from the Design Spec (PART 2) and resolves
 * through the [Enroll] token bridge → existing [VTheme] (no hardcoded hex, keeps
 * Parents↔Teacher colour parity per the iteration IMPORTANT NOTE):
 *   • fill    → `Enroll.colors.surfaceCard`   (loop: SurfaceCard)
 *   • corners → `Enroll.shape.card`           (loop: ShapeCard = 16dp)
 *   • border  → 1.dp `Enroll.colors.surfaceSubtle` (loop: 1.dp SurfaceSubtle border)
 *   • press   → `Modifier.pressScale(0.98f)`  (loop: subtle scale(0.98f) via interactionSource)
 *   • padding → `Enroll.space.lg` (16dp) default content inset
 *
 * The press scale + ripple-free clickable reuse the portal's existing motion
 * primitive [pressScale] (VMotion §13.3 `active:scale-[0.98]`), so the tactile
 * feel is identical to the rest of the app.
 *
 * USAGE:
 * ```
 * EnrollCard(onClick = { nav.toGradebook() }) {
 *     SectionHeader("ATTENDANCE TODAY")
 *     // … rows …
 * }
 * ```
 *
 * @param onClick when non-null the card becomes tappable: it gets the 0.98f press
 *   scale and a ripple-free `clickable`. Static (display-only) cards pass null and
 *   never animate on touch — they would feel broken if they did.
 * @param tint optional surface tint for semantic cards (e.g. `Enroll.colors.primarySoft`
 *   for info nudges, `Enroll.colors.accentSoft` for pending nudges — loop P2-T5).
 *   Defaults to the neutral card fill.
 */
@Composable
fun EnrollCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tint: Color? = null,
    padding: Dp = Enroll.space.lg,
    shape: RoundedCornerShape = Enroll.shape.card,
    border: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val fill = tint ?: Enroll.colors.surfaceCard
    val borderColor = Enroll.colors.surfaceSubtle

    var base = modifier

    // Press give FIRST in the chain so it wraps the ripple-free clickable visually
    // (VMotion contract: pressScale before clickable). Only navigable cards animate.
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        base = base
            .pressScale(interaction)               // loop: subtle scale(0.98f)
            .clip(shape)
            .background(fill)
        if (border) {
            base = base.border(BorderStroke(1.dp, borderColor), shape)
        }
        base = base.clickable(
            interactionSource = interaction,
            indication = null,                     // flat card — no Material ripple wash
            onClick = onClick,
        )
    } else {
        base = base.clip(shape).background(fill)
        if (border) {
            base = base.border(BorderStroke(1.dp, borderColor), shape)
        }
    }

    Column(
        modifier = base.padding(padding),
        content = content,
    )
}
