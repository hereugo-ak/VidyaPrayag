package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.pressScale

// ─────────────────────────────────────────────────────────────────────────────
// P3-T5 — Bulk Actions Bar
//
// A contextual action bar that slides up from the bottom (above the FAB) the moment
// one or more student rows are long-pressed/selected. "X selected" on the left,
// Set Mark / Add Remark / Cancel actions on the right. Primary fill, white content.
// Driven by AnimatedVisibility(selectedCount > 0). All tokens via Enroll.*.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * BulkActionsBar — the multi-select action bar.
 *
 * @param selectedCount how many rows are selected (0 = hidden)
 * @param onSetMark     bulk "Set Mark" → host opens a value entry
 * @param onAddRemark   bulk "Add Remark"
 * @param onCancel      clear the selection
 */
@Composable
fun BulkActionsBar(
    selectedCount: Int,
    onSetMark: () -> Unit,
    onAddRemark: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Enroll.space.lg)
                .clip(Enroll.shape.card)
                .background(Enroll.colors.primary)
                .padding(horizontal = Enroll.space.lg, vertical = Enroll.space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$selectedCount selected",
                style = Enroll.type.labelBold.colored(Enroll.colors.onPrimary),
                modifier = Modifier.weight(1f),
            )
            BulkAction(VIcons.Edit3, "Set Mark", onSetMark)
            Spacer(Modifier.size(Enroll.space.lg))
            BulkAction(VIcons.ClipboardList, "Remark", onAddRemark)
            Spacer(Modifier.size(Enroll.space.lg))
            BulkAction(VIcons.Close, "Cancel", onCancel)
        }
    }
}

@Composable
private fun BulkAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    val ix = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .pressScale(ix)
            .clip(Enroll.shape.chip)
            .clickable(interactionSource = ix, indication = null, onClick = onClick)
            .padding(horizontal = Enroll.space.xs, vertical = Enroll.space.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Enroll.colors.onPrimary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(Enroll.space.xs))
        Text(
            text = label,
            style = Enroll.type.bodySmall.colored(Enroll.colors.onPrimary),
        )
    }
}
