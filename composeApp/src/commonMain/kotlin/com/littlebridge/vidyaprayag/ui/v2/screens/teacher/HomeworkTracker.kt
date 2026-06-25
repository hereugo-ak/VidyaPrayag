package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.EnrollCard
import com.littlebridge.vidyaprayag.ui.v2.components.SectionHeader
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

// ─────────────────────────────────────────────────────────────────────────────
// P4-T3 — Homework Tracker
//
// A second Planner view (the host fronts a "Schedule | Homework" pill toggle under
// the week strip — see [PlannerSubToggle]). The Homework view is a LazyColumn
// grouped by class: each HomeworkCard shows the subject + assignment, the due date
// and an "X / Y submitted" progress bar (StatusPresent). Overdue rows turn the bar
// StatusAbsent and add an "OVERDUE" badge. Tap a card → host opens a per-student
// submission sheet.
//
// All visuals via the Enroll.* bridge → VTheme (status colours preserved; no new
// hex; Parents↔Teacher parity per the IMPORTANT NOTE). Items are keyed.
// ─────────────────────────────────────────────────────────────────────────────

/** A homework assignment row. VM-agnostic UI model. */
data class HomeworkAssignment(
    val id: String,
    val className: String,            // group key, e.g. "Class 8B"
    val subject: String,              // "Mathematics"
    val description: String,          // "Exercise 4.2 — quadratic equations"
    val dueLabel: String,             // "Due 26 June"
    val submitted: Int,
    val total: Int,
    val overdue: Boolean,
)

/** The two Planner sub-views the toggle switches between. */
enum class PlannerSubView { Schedule, Homework }

/**
 * PlannerSubToggle — the pill-style "Schedule | Homework" switch under the week
 * strip. Selected segment → primary fill + white; the unselected segment is plain.
 */
@Composable
fun PlannerSubToggle(
    selected: PlannerSubView,
    onSelect: (PlannerSubView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(Enroll.shape.pill)
            .background(Enroll.colors.surfaceSubtle)
            .padding(Enroll.space.xs),
        horizontalArrangement = Arrangement.spacedBy(Enroll.space.xs),
    ) {
        ToggleSegment("Schedule", selected == PlannerSubView.Schedule) { onSelect(PlannerSubView.Schedule) }
        ToggleSegment("Homework", selected == PlannerSubView.Homework) { onSelect(PlannerSubView.Homework) }
    }
}

@Composable
private fun ToggleSegment(label: String, active: Boolean, onClick: () -> Unit) {
    val ix = remember { MutableInteractionSource() }
    Text(
        text = label,
        style = Enroll.type.labelBold.colored(
            if (active) Enroll.colors.onPrimary else Enroll.colors.textSecondary,
        ),
        modifier = Modifier
            .clip(Enroll.shape.pill)
            .background(if (active) Enroll.colors.primary else Color.Transparent)
            .clickable(interactionSource = ix, indication = null, onClick = onClick)
            .padding(horizontal = Enroll.space.xl, vertical = Enroll.space.sm),
    )
}

/**
 * HomeworkTrackerView — the class-grouped homework list.
 *
 * @param assignments   all homework rows (any class); grouped by `className` here.
 * @param onOpen        a card was tapped → host opens the per-student submission sheet.
 */
@Composable
fun HomeworkTrackerView(
    assignments: List<HomeworkAssignment>,
    onOpen: (HomeworkAssignment) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (assignments.isEmpty()) {
        Box(modifier.fillMaxWidth().padding(Enroll.space.xxl), contentAlignment = Alignment.Center) {
            Text(
                text = "No homework assigned yet.",
                style = Enroll.type.bodyMedium.colored(Enroll.colors.textTertiary),
            )
        }
        return
    }

    // Stable class grouping — preserve first-seen order.
    val grouped = assignments.groupBy { it.className }

    LazyColumn(
        modifier = modifier.fillMaxWidth().background(Enroll.colors.surfaceBase),
        contentPadding = PaddingValues(horizontal = Enroll.space.lg, vertical = Enroll.space.md),
        verticalArrangement = Arrangement.spacedBy(Enroll.space.lg),
    ) {
        grouped.forEach { (className, rows) ->
            item(key = "hdr-$className") {
                SectionHeader(title = className)
            }
            items(items = rows, key = { it.id }) { hw ->
                HomeworkCard(hw = hw, onClick = { onOpen(hw) })
            }
        }
    }
}

@Composable
private fun HomeworkCard(hw: HomeworkAssignment, onClick: () -> Unit) {
    EnrollCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = hw.subject,
                style = Enroll.type.labelBold.colored(Enroll.colors.textPrimary),
                modifier = Modifier.weight(1f),
            )
            if (hw.overdue) OverdueBadge()
        }
        Spacer(Modifier.height(Enroll.space.xs))
        Text(
            text = hw.description,
            style = Enroll.type.bodyMedium.colored(Enroll.colors.textSecondary),
            maxLines = 2,
        )
        Spacer(Modifier.height(Enroll.space.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = hw.dueLabel,
                style = Enroll.type.bodySmall.colored(
                    if (hw.overdue) Enroll.colors.statusAbsent else Enroll.colors.textTertiary,
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${hw.submitted} / ${hw.total} submitted",
                style = Enroll.type.bodySmall.colored(Enroll.colors.textSecondary),
            )
        }
        Spacer(Modifier.height(Enroll.space.sm))
        SubmissionBar(
            fraction = if (hw.total > 0) hw.submitted.toFloat() / hw.total else 0f,
            overdue = hw.overdue,
        )
    }
}

/** "OVERDUE" chip — danger soft fill, danger ink. */
@Composable
private fun OverdueBadge() {
    Text(
        text = "OVERDUE",
        style = Enroll.type.labelCaps.colored(Enroll.colors.statusAbsent),
        modifier = Modifier
            .clip(Enroll.shape.chip)
            .background(Enroll.colors.statusAbsentSoft)
            .padding(horizontal = Enroll.space.sm, vertical = Enroll.space.xs),
    )
}

/**
 * A slim submission progress bar — a rounded track with an animated fill. Green
 * (StatusPresent) on track, red (StatusAbsent) when overdue. Built locally so the
 * fill colour is the exact semantic, not a VBadgeTone approximation.
 */
@Composable
private fun SubmissionBar(fraction: Float, overdue: Boolean) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "submissionBar",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(Enroll.shape.pill)
            .background(Enroll.colors.surfaceSubtle),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(6.dp)
                .clip(Enroll.shape.pill)
                .background(if (overdue) Enroll.colors.statusAbsent else Enroll.colors.statusPresent),
        )
    }
}
