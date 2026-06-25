package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.pressScale

/**
 * QuickActionRow — the Home tab's three-up action pills (Loop task P2-T3).
 *
 * The three core teaching jobs, one tap away, never behind a menu: Take Attendance,
 * Add Marks, Message Parent. Rendered as a centered `Row` with `SpaceMD` gaps.
 *
 * Pill style is verbatim from the Design Spec (via the `Enroll.*` bridge → VTheme,
 * so PrimaryIndigo* maps to the portal violet accent — no new colour):
 *   • background `primarySoft`  (loop: PrimaryIndigoSoft)
 *   • icon + text `primaryMid`  (loop: PrimaryIndigoMid)
 *   • `shape.card` corners, `SpaceMD` vertical / `SpaceLG` horizontal padding
 *   • the portal's `pressScale` give on tap (consistent micro-interaction)
 */
@Composable
fun QuickActionRow(
    onTakeAttendance: () -> Unit,
    onAddMarks: () -> Unit,
    onMessageParent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Enroll.space.md),
    ) {
        QuickActionPill(VIcons.Check, "Take Attendance", onTakeAttendance, Modifier.weight(1f))
        QuickActionPill(VIcons.GraduationCap, "Add Marks", onAddMarks, Modifier.weight(1f))
        QuickActionPill(VIcons.Chat, "Message Parent", onMessageParent, Modifier.weight(1f))
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ix = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .pressScale(ix)
            .clip(Enroll.shape.card)
            .background(Enroll.colors.primarySoft)
            .clickable(interactionSource = ix, indication = null, onClick = onClick)
            .padding(horizontal = Enroll.space.lg, vertical = Enroll.space.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Enroll.colors.primaryMid,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(Enroll.space.sm))
        Text(
            text = label,
            style = Enroll.type.labelBold.colored(Enroll.colors.primaryMid),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
