package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.littlebridge.vidyaprayag.ui.v2.components.EnrollCard
import com.littlebridge.vidyaprayag.ui.v2.components.SectionHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

// ─────────────────────────────────────────────────────────────────────────────
// P4-T2 — Daily Plan View
//
// Below the WeekViewHeader: a LazyColumn of PlanDaySection — one section per day,
// each with a "Monday, 23 June" header and the day's PeriodPlanCards. A card shows
// the time range + period number, the class+subject, an inline lesson-topic field
// (BasicTextField, "Tap to add lesson topic…"), a homework row (assigned/submitted),
// and — when an exam falls on that date — an AccentAmber left border + "EXAM" badge.
// Tapping a card opens the full LessonPlanSheet (topic, objective, materials, HW).
//
// All visuals via the Enroll.* bridge → VTheme (violet primary ≈ PrimaryIndigo,
// warning accent ≈ AccentAmber — no new hex, Parents↔Teacher parity per the
// IMPORTANT NOTE). Every LazyColumn item is keyed.
// ─────────────────────────────────────────────────────────────────────────────

/** A single planned period within a day. VM-agnostic UI model. */
data class PlannerPeriod(
    val id: String,
    val periodNumber: Int,
    val timeRange: String,            // "08:45 – 09:30" (host formats via DateUtil)
    val className: String,            // "Class 9B"
    val subject: String,              // "Science"
    val lessonTopic: String,          // "" → placeholder
    val homeworkAssigned: Int,
    val homeworkSubmitted: Int,
    val isExam: Boolean,
)

/** A planning day: its ISO key, the human header, and the day's periods. */
data class PlannerDay(
    val iso: String,                  // "2026-06-23"
    val header: String,               // "Monday, 23 June"
    val periods: List<PlannerPeriod>,
)

/** Detail captured/edited in the LessonPlanSheet. */
data class LessonPlanDraft(
    val periodId: String,
    val topic: String,
    val objective: String,
    val materials: String,
    val homework: String,
)

/**
 * DailyPlanView — the scrollable per-day plan list.
 *
 * @param days            the planning days to render (host builds them for the week).
 * @param listState       hoisted so [WeekViewHeader] can scroll the list to a tapped day.
 * @param onTopicChanged  inline lesson-topic edit committed (host persists).
 * @param onOpenPlan      a card was tapped → open [LessonPlanSheet].
 */
@Composable
fun DailyPlanView(
    days: List<PlannerDay>,
    onTopicChanged: (periodId: String, topic: String) -> Unit,
    onOpenPlan: (PlannerPeriod) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(Enroll.colors.surfaceBase),
        state = listState,
        contentPadding = PaddingValues(
            horizontal = Enroll.space.lg,
            vertical = Enroll.space.md,
        ),
        verticalArrangement = Arrangement.spacedBy(Enroll.space.lg),
    ) {
        items(items = days, key = { it.iso }) { day ->
            PlanDaySection(
                day = day,
                onTopicChanged = onTopicChanged,
                onOpenPlan = onOpenPlan,
            )
        }
    }
}

@Composable
private fun PlanDaySection(
    day: PlannerDay,
    onTopicChanged: (String, String) -> Unit,
    onOpenPlan: (PlannerPeriod) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = day.header,
            style = Enroll.type.headingSmall.colored(Enroll.colors.textPrimary),
        )
        Spacer(Modifier.height(Enroll.space.md))
        if (day.periods.isEmpty()) {
            Text(
                text = "No classes scheduled.",
                style = Enroll.type.bodyMedium.colored(Enroll.colors.textTertiary),
            )
        } else {
            day.periods.forEach { period ->
                PeriodPlanCard(
                    period = period,
                    onTopicChanged = { onTopicChanged(period.id, it) },
                    onOpen = { onOpenPlan(period) },
                )
                Spacer(Modifier.height(Enroll.space.sm))
            }
        }
    }
}

@Composable
private fun PeriodPlanCard(
    period: PlannerPeriod,
    onTopicChanged: (String) -> Unit,
    onOpen: () -> Unit,
) {
    EnrollCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.Top) {
            // Exam marker — an amber left rail when this period carries an exam.
            if (period.isExam) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(56.dp)
                        .clip(Enroll.shape.pill)
                        .background(Enroll.colors.accent),
                )
                Spacer(Modifier.width(Enroll.space.md))
            }
            // Time + period number rail.
            Column(Modifier.width(64.dp)) {
                Text(
                    text = "P${period.periodNumber}",
                    style = Enroll.type.labelCaps.colored(Enroll.colors.textTertiary),
                )
                Spacer(Modifier.height(Enroll.space.xs))
                Text(
                    text = period.timeRange,
                    style = Enroll.type.bodySmall.colored(Enroll.colors.textTertiary),
                )
            }
            Spacer(Modifier.width(Enroll.space.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${period.className} · ${period.subject}",
                        style = Enroll.type.labelBold.colored(Enroll.colors.textPrimary),
                        modifier = Modifier.weight(1f),
                    )
                    if (period.isExam) ExamBadge()
                }
                Spacer(Modifier.height(Enroll.space.xs))
                LessonTopicField(value = period.lessonTopic, onValueChange = onTopicChanged)
                Spacer(Modifier.height(Enroll.space.sm))
                HomeworkRow(assigned = period.homeworkAssigned, submitted = period.homeworkSubmitted)
            }
        }
    }
}

/** "EXAM" chip — amber soft fill, amber ink (semantic exam marker). */
@Composable
private fun ExamBadge() {
    Text(
        text = "EXAM",
        style = Enroll.type.labelCaps.colored(Enroll.colors.accent),
        modifier = Modifier
            .clip(Enroll.shape.chip)
            .background(Enroll.colors.accentSoft)
            .padding(horizontal = Enroll.space.sm, vertical = Enroll.space.xs),
    )
}

/** Inline lesson-topic editor — placeholder when empty, commits on each change. */
@Composable
private fun LessonTopicField(value: String, onValueChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    Box {
        if (text.isEmpty()) {
            Text(
                text = "Tap to add lesson topic…",
                style = Enroll.type.bodyMedium.colored(Enroll.colors.textTertiary),
            )
        }
        BasicTextField(
            value = text,
            onValueChange = { text = it; onValueChange(it) },
            textStyle = Enroll.type.bodyMedium.colored(Enroll.colors.textSecondary),
            singleLine = true,
            cursorBrush = SolidColor(Enroll.colors.primary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** "HW: assigned / submitted" — green count when fully submitted, amber while pending. */
@Composable
private fun HomeworkRow(assigned: Int, submitted: Int) {
    if (assigned <= 0) {
        Text(
            text = "HW: none",
            style = Enroll.type.bodySmall.colored(Enroll.colors.textTertiary),
        )
        return
    }
    val complete = submitted >= assigned
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "HW: ",
            style = Enroll.type.bodySmall.colored(Enroll.colors.textTertiary),
        )
        Text(
            text = "$submitted / $assigned submitted",
            style = Enroll.type.bodySmall.colored(
                if (complete) Enroll.colors.statusPresent else Enroll.colors.accent,
            ),
        )
    }
}

/**
 * LessonPlanSheet — the full per-period plan editor.
 *
 * @param visible   show/hide
 * @param period    the period being planned (header context)
 * @param draft     current draft values (host owns persistence)
 * @param onDismiss close
 * @param onSave    commit the edited [LessonPlanDraft]
 */
@Composable
fun LessonPlanSheet(
    visible: Boolean,
    period: PlannerPeriod?,
    draft: LessonPlanDraft?,
    onDismiss: () -> Unit,
    onSave: (LessonPlanDraft) -> Unit,
) {
    if (!visible || period == null) return

    var topic by remember(draft) { mutableStateOf(draft?.topic ?: period.lessonTopic) }
    var objective by remember(draft) { mutableStateOf(draft?.objective ?: "") }
    var materials by remember(draft) { mutableStateOf(draft?.materials ?: "") }
    var homework by remember(draft) { mutableStateOf(draft?.homework ?: "") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Enroll.shape.sheet)
                .background(Enroll.colors.surfaceBase)
                .padding(horizontal = Enroll.space.lg)
                .padding(bottom = Enroll.space.xxl),
        ) {
            Spacer(Modifier.height(Enroll.space.md))
            SectionHeader(title = "LESSON PLAN")
            Spacer(Modifier.height(Enroll.space.xs))
            Text(
                text = "${period.className} · ${period.subject}  ·  P${period.periodNumber}",
                style = Enroll.type.bodyMedium.colored(Enroll.colors.textSecondary),
            )
            Spacer(Modifier.height(Enroll.space.lg))

            VInput(value = topic, onValueChange = { topic = it }, label = "Topic", placeholder = "e.g. Photosynthesis — light reactions")
            Spacer(Modifier.height(Enroll.space.md))
            VInput(value = objective, onValueChange = { objective = it }, label = "Learning objective", placeholder = "What students should be able to do", singleLine = false)
            Spacer(Modifier.height(Enroll.space.md))
            VInput(value = materials, onValueChange = { materials = it }, label = "Materials needed", placeholder = "e.g. lab kit, worksheet 3")
            Spacer(Modifier.height(Enroll.space.md))
            VInput(value = homework, onValueChange = { homework = it }, label = "Homework", placeholder = "Assignment for this lesson", singleLine = false)
            Spacer(Modifier.height(Enroll.space.lg))

            VButton(
                text = "Save plan",
                onClick = {
                    onSave(LessonPlanDraft(period.id, topic.trim(), objective.trim(), materials.trim(), homework.trim()))
                },
                full = true,
                enabled = topic.isNotBlank(),
            )
        }
    }
}
