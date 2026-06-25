package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.EnrollCard
import com.littlebridge.vidyaprayag.ui.v2.components.SectionHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.pressScale

// ─────────────────────────────────────────────────────────────────────────────
// P2-T6 — Pending Tasks Card
//
// A teacher's running to-do strip on the Home tab: the three most pressing tasks
// with a tap-to-tick checkbox, collapsed by default and expandable via "See All".
// Ticking a task strikes it through and turns the box StatusPresent green — the
// satisfying "one less thing" beat. All colour/type/shape/space via Enroll.*.
// ─────────────────────────────────────────────────────────────────────────────

/** A single teacher to-do. `dueLabel` is a pre-formatted human string (e.g. "Due today"). */
data class TeacherTask(
    val id: String,
    val description: String,
    val dueLabel: String,
    val done: Boolean = false,
)

/** How many tasks the collapsed card shows before "See All". */
private const val COLLAPSED_LIMIT = 3

/**
 * PendingTasksCard — the Home tab "PENDING" list.
 *
 * @param tasks       the teacher's open tasks (VM-owned ordering)
 * @param onToggle    tap a checkbox → mark done / undone (id passed back)
 * @param onSeeAll    open the full task list (only shown when there are extras)
 */
@Composable
fun PendingTasksCard(
    tasks: List<TeacherTask>,
    onToggle: (String) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tasks.isEmpty()) return

    val hasMore = tasks.size > COLLAPSED_LIMIT
    val visible = tasks.take(COLLAPSED_LIMIT)

    EnrollCard(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "PENDING",
            action = if (hasMore) "See All" else null,
            onAction = if (hasMore) onSeeAll else null,
        )
        Spacer(Modifier.height(Enroll.space.md))
        visible.forEachIndexed { index, task ->
            if (index > 0) Spacer(Modifier.height(Enroll.space.sm))
            TaskRow(task = task, onToggle = { onToggle(task.id) })
        }
    }
}

@Composable
private fun TaskRow(task: TeacherTask, onToggle: () -> Unit) {
    val ix = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Enroll.shape.chip)
            .clickable(interactionSource = ix, indication = null, onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskCheckbox(checked = task.done, interactionSource = ix)
        Spacer(Modifier.size(Enroll.space.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.description,
                style = Enroll.type.bodyMedium.colored(
                    if (task.done) Enroll.colors.textTertiary else Enroll.colors.textPrimary,
                ),
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                maxLines = 2,
            )
            AnimatedVisibility(visible = !task.done) {
                Text(
                    text = task.dueLabel,
                    style = Enroll.type.bodySmall.colored(Enroll.colors.textTertiary),
                )
            }
        }
    }
}

/** A tactile custom checkbox — hairline square when open, StatusPresent tick when done. */
@Composable
private fun TaskCheckbox(checked: Boolean, interactionSource: MutableInteractionSource) {
    val shape = RoundedCornerShape(Enroll.space.xs)
    Box(
        modifier = Modifier
            .pressScale(interactionSource)
            .size(22.dp)
            .clip(shape)
            .then(
                if (checked) {
                    Modifier.background(Enroll.colors.statusPresent)
                } else {
                    Modifier.border(1.5.dp, Enroll.colors.border, shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = VIcons.Check,
                contentDescription = null,
                tint = Enroll.colors.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
