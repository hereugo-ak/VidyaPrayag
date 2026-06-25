package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.EnrollCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

// ─────────────────────────────────────────────────────────────────────────────
// P4-T4 — Smart Planner Nudge
//
// A slim amber card at the top of the week view, shown only when there are
// unplanned periods next week. One "Plan Now" action scrolls to the first
// unplanned period; a dismiss X hides it for the session.
//
// Dismissed state is hoisted (the host owns persistence — DataStore — so the nudge
// stays dismissed across launches). The card uses the EnrollCard amber `accentSoft`
// tint (loop: AccentAmberSoft) + amber ink — the exact "pending" semantic, no new
// hex (IMPORTANT NOTE). All tokens via Enroll.*.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * PlannerNudge — the unplanned-periods amber prompt.
 *
 * @param unplannedCount  number of unplanned periods next week. ≤ 0 → renders nothing.
 * @param dismissed       host-owned dismissed flag (persist via DataStore).
 * @param onPlanNow       "Plan Now" tapped → host scrolls to the first unplanned period.
 * @param onDismiss       X tapped → host persists the dismissed flag.
 *
 * TODO(host): persist [dismissed] in local DataStore so the nudge stays dismissed
 *   across app launches (the loop's stated storage requirement).
 */
@Composable
fun PlannerNudge(
    unplannedCount: Int,
    dismissed: Boolean,
    onPlanNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = unplannedCount > 0 && !dismissed
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        EnrollCard(
            modifier = modifier.fillMaxWidth(),
            tint = Enroll.colors.accentSoft,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(Enroll.shape.pill)
                        .background(Enroll.colors.accentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = VIcons.Calendar,
                        contentDescription = null,
                        tint = Enroll.colors.accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(Enroll.space.md))
                Text(
                    text = nudgeMessage(unplannedCount),
                    style = Enroll.type.bodyMedium.colored(Enroll.colors.textPrimary),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Enroll.space.sm))
                // Dismiss X.
                val dx = remember { MutableInteractionSource() }
                Icon(
                    imageVector = VIcons.Close,
                    contentDescription = "Dismiss",
                    tint = Enroll.colors.textTertiary,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(Enroll.shape.pill)
                        .clickable(interactionSource = dx, indication = null, onClick = onDismiss),
                )
            }
            Spacer(Modifier.size(Enroll.space.md))
            // "Plan Now" action pill.
            val px = remember { MutableInteractionSource() }
            Text(
                text = "Plan Now",
                style = Enroll.type.labelBold.colored(Enroll.colors.onPrimary),
                modifier = Modifier
                    .clip(Enroll.shape.pill)
                    .background(Enroll.colors.primary)
                    .clickable(interactionSource = px, indication = null, onClick = onPlanNow)
                    .padding(horizontal = Enroll.space.lg, vertical = Enroll.space.sm),
            )
        }
    }
}

/** Real EdTech copy, pluralised. */
private fun nudgeMessage(count: Int): String =
    if (count == 1) "You haven't planned 1 period for next week."
    else "You haven't planned $count periods for next week."
