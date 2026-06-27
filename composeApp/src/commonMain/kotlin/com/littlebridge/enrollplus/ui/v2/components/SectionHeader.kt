package com.littlebridge.enrollplus.ui.v2.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.littlebridge.enrollplus.ui.v2.theme.Enroll
import com.littlebridge.enrollplus.ui.v2.theme.colored

/**
 * ENROLL+ TEACHER PORTAL — shared section header (Loop task P1-T3).
 *
 * The loop references this constantly — `SectionHeader("TODAY'S SCHEDULE")`,
 * `SectionHeader("NOTIFICATIONS", action = "Mark all read") { … }` — so it needs
 * the ergonomic STRING-based action contract from the spec:
 *
 *   SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null)
 *
 * The portal already has `VSectionHeader` (in screens/Shared.kt) but that takes a
 * `@Composable` action *slot*; this is the loop's terser string variant that owns
 * the action's styling so every call site renders an identical text button. Layout
 * + typography rhythm match `VSectionHeader` exactly (RULE: visual parity).
 *
 * Visual decisions (all justified from Design Spec PART 2, via the `Enroll.*` bridge):
 *   • Title  → `Enroll.type.labelCaps` (11/700 ALL-CAPS, 0.10em) in
 *              `Enroll.colors.textSecondary` — the spec's `LabelCaps` / `TextSecondary`.
 *   • Action → text button in `Enroll.colors.primaryMid` (spec: `PrimaryIndigoMid`),
 *              `labelBold` weight, ripple-free with a subtle pill press surface so it
 *              reads as a tappable affordance, not body copy.
 *   • Spacing → `SpaceBetween` row so the action pins to the trailing edge.
 *
 * No hardcoded hex; honours Light/Night tone automatically.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = Enroll.type.labelCaps.colored(Enroll.colors.textSecondary),
        )
        if (action != null && onAction != null) {
            val interaction = remember { MutableInteractionSource() }
            Text(
                text = action,
                style = Enroll.type.labelBold.colored(Enroll.colors.primaryMid),
                modifier = Modifier
                    .clip(RoundedCornerShape(Enroll.space.sm))
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onAction,
                    )
                    .padding(horizontal = Enroll.space.sm, vertical = Enroll.space.xs),
            )
        }
    }
}
