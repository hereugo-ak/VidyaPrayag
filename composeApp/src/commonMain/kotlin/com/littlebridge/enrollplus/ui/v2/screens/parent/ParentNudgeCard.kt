/*
 * File: ParentNudgeCard.kt
 * Module: ui.v2.screens.parent
 *
 * The gentle, opt-in PEWS nudge card on the parent dashboard — the read-only end
 * of the Sense → Reason → Act → Learn loop. It is deliberately SOFT:
 *
 *   • No "risk" word, no score, no clinical labels (that vocabulary lives only on
 *     the school/teacher side). The copy is supportive and child-first.
 *   • It renders ONLY when the server says show=true (a real concern exists AND
 *     the school turned on parent_share_enabled). Otherwise the card is absent —
 *     the dashboard never nags.
 *   • Actions deep-link into existing parent surfaces (attendance / message
 *     teacher); we never auto-send anything.
 *
 * Honesty (LAW 6): every line is a real value served by GET /api/v1/parent/pews/{childId}.
 */
package com.littlebridge.enrollplus.ui.v2.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsParentActionDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsParentNudgeDto
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored

/**
 * ParentNudgeCard — supportive, label-free entry point. Driven by the
 * server-provided [PewsParentNudgeDto]; the caller only renders this when
 * `nudge.show == true`.
 *
 * @param onAction invoked with a parent action's deep-link target (e.g.
 *        "attendance" / "messages") so the host portal can route to the right tab.
 */
@Composable
fun ParentNudgeCard(
    nudge: PewsParentNudgeDto,
    onAction: (PewsParentActionDto) -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    VCard(modifier = modifier.fillMaxWidth(), padding = 16.dp) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Soft heart chip — warmth, not alarm.
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    VIcons.Heart,
                    contentDescription = null,
                    tint = c.accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    nudge.headline.ifBlank { "A little support for ${nudge.childName}" },
                    style = VTheme.type.h4.colored(c.ink).copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                )
                if (nudge.message.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        nudge.message,
                        style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp, lineHeight = 19.sp),
                    )
                }
            }
        }

        // Action row — deep-links into existing parent surfaces. Never auto-sends.
        if (nudge.actions.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                nudge.actions.take(2).forEachIndexed { index, action ->
                    VButton(
                        text = action.label,
                        onClick = { onAction(action) },
                        // First action is the gentle primary (e.g. View attendance);
                        // the second is a quiet secondary (e.g. Message teacher).
                        variant = if (index == 0) VButtonVariant.Primary else VButtonVariant.Secondary,
                        size = VButtonSize.Sm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Dismiss row — "Got it" lets the parent clear the card after reading.
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            VButton(
                text = "Got it",
                onClick = onDismiss,
                variant = VButtonVariant.Ghost,
                size = VButtonSize.Sm,
            )
        }
    }
}
