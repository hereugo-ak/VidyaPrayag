package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.EnrollCard
import com.littlebridge.vidyaprayag.ui.v2.components.SectionHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherClassAssignment — one row of the teacher's class roster (Loop task P6-T3).
 *
 * A pure UI model the host maps from `TeacherProfileViewModel` (classId stays the
 * stable key for the Gradebook deep link; the strings are display-only). Real EdTech
 * shape — a class+section, the subjects taught in it, and the live headcount.
 */
data class TeacherClassAssignment(
    val classId: String,
    val className: String,   // e.g. "Class 8" / "Grade 5"
    val section: String,     // e.g. "A"
    val subjects: List<String>, // e.g. ["Mathematics", "Science"]
    val studentCount: Int,
)

/**
 * TeacherAssignmentCard — the "MY CLASSES" teaching-assignment card (Loop task P6-T3).
 *
 * A single [EnrollCard] introduced by a `SectionHeader("MY CLASSES")`, listing every
 * class the teacher is assigned to. The spec calls for a non-scrolling `LazyColumn`;
 * because this card lives INSIDE the profile's outer scroll container, a nested
 * lazy list with `userScrollEnabled = false` would still measure with infinite
 * height and crash — so the CMP-safe equivalent is a plain `Column` that lays out
 * every row (the rosters are short, never paginated), which is exactly the
 * "non-scrolling list" the spec intends. Rows are keyed by `classId`.
 *
 * Each [ClassAssignmentRow] shows: class + section (`labelBold`), the subjects taught
 * as comma-style chips (FlowRow of [SubjectChip]s, wrapping on small screens), and
 * the student count with a `GraduationCap` glyph. Tapping a row deep-links into the
 * Gradebook filtered to that class via `onOpenClass(classId)` (host wires the nav arg).
 *
 * All colour/type via `Enroll.*` → VTheme (no new hex). Subject chips reuse the
 * brand `primarySoft` chip fill that the rest of the portal uses for passive tags.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherAssignmentCard(
    assignments: List<TeacherClassAssignment>,
    onOpenClass: (classId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "MY CLASSES")
        Spacer(Modifier.height(Enroll.space.sm))
        EnrollCard(modifier = Modifier.fillMaxWidth(), padding = Enroll.space.xs) {
            assignments.forEachIndexed { index, a ->
                ClassAssignmentRow(
                    assignment = a,
                    onClick = { onOpenClass(a.classId) },
                )
                if (index < assignments.lastIndex) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(horizontal = Enroll.space.md)
                            .background(Enroll.colors.hairline),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassAssignmentRow(
    assignment: TeacherClassAssignment,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val title = "${assignment.className} · ${assignment.section}"
    val countLabel = if (assignment.studentCount == 1) "1 student" else "${assignment.studentCount} students"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Enroll.shape.chip)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .semantics { contentDescription = "$title, $countLabel. Open gradebook." }
            .padding(horizontal = Enroll.space.md, vertical = Enroll.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = Enroll.type.labelBold.colored(Enroll.colors.textPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (assignment.subjects.isNotEmpty()) {
                Spacer(Modifier.height(Enroll.space.sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Enroll.space.xs),
                    verticalArrangement = Arrangement.spacedBy(Enroll.space.xs),
                ) {
                    assignment.subjects.forEach { subject -> SubjectChip(subject) }
                }
            }
            Spacer(Modifier.height(Enroll.space.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    VIcons.GraduationCap,
                    contentDescription = null,
                    tint = Enroll.colors.textTertiary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "  $countLabel",
                    style = Enroll.type.bodySmall.colored(Enroll.colors.textSecondary),
                    maxLines = 1,
                )
            }
        }
        Icon(
            VIcons.ChevronRight,
            contentDescription = null,
            tint = Enroll.colors.textTertiary,
        )
    }
}

/** A passive subject tag — brand soft fill, primary-deep ink. */
@Composable
private fun SubjectChip(subject: String) {
    Text(
        text = subject,
        style = Enroll.type.bodySmall.colored(Enroll.colors.primaryDeep),
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(Enroll.space.sm))
            .background(Enroll.colors.primarySoft)
            .padding(horizontal = Enroll.space.sm, vertical = Enroll.space.xs),
    )
}
