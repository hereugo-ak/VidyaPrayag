package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.pressScale

// ─────────────────────────────────────────────────────────────────────────────
// P3-T1 — Class + Subject Selector
//
// The gradebook's scope picker: two horizontally scrollable chip rows that replace
// any dropdown/dialog class selection. Row 1 = classes, Row 2 = the subjects taught
// in the selected class. Designed to live in the sticky header zone (NOT inside the
// scrolling marks list) — the host pins it above the LazyColumn.
//
// Selected chip → Enroll.colors.primary fill + white text; unselected →
// surfaceSubtle fill + textSecondary text. All tokens via Enroll.* (the violet
// primary family stands in for the loop's PrimaryIndigo per the IMPORTANT NOTE).
// ─────────────────────────────────────────────────────────────────────────────

/** A class the teacher can grade. `id` is the assignment/class key the VM resolves. */
data class GradebookClassOption(
    val id: String,
    val label: String,            // e.g. "Class 7A"
)

/** A subject within the selected class. */
data class GradebookSubjectOption(
    val id: String,
    val label: String,            // e.g. "Science"
)

/**
 * GradebookSelector — the sticky two-row scope picker.
 *
 * @param classes            all classes the teacher grades
 * @param selectedClassId    currently active class (null = none chosen yet)
 * @param onSelectClass      pick a class → host reloads subjects for it
 * @param subjects           subjects for the selected class (empty hides row 2)
 * @param selectedSubjectId  currently active subject
 * @param onSelectSubject    pick a subject → host reloads the marks grid
 */
@Composable
fun GradebookSelector(
    classes: List<GradebookClassOption>,
    selectedClassId: String?,
    onSelectClass: (String) -> Unit,
    subjects: List<GradebookSubjectOption>,
    selectedSubjectId: String?,
    onSelectSubject: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Enroll.colors.surfaceBase)
            .padding(vertical = Enroll.space.md),
    ) {
        // Row 1 — classes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Enroll.space.sm),
        ) {
            Spacer(Modifier.padding(start = Enroll.space.lg))
            classes.forEach { c ->
                SelectorChip(
                    label = c.label,
                    selected = c.id == selectedClassId,
                    onClick = { onSelectClass(c.id) },
                )
            }
            Spacer(Modifier.padding(end = Enroll.space.lg))
        }

        // Row 2 — subjects (only once a class is chosen and has subjects)
        if (subjects.isNotEmpty()) {
            Spacer(Modifier.height(Enroll.space.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Enroll.space.sm),
            ) {
                Spacer(Modifier.padding(start = Enroll.space.lg))
                subjects.forEach { s ->
                    SelectorChip(
                        label = s.label,
                        selected = s.id == selectedSubjectId,
                        onClick = { onSelectSubject(s.id) },
                    )
                }
                Spacer(Modifier.padding(end = Enroll.space.lg))
            }
        }
    }
}

@Composable
private fun SelectorChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ix = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (selected) Enroll.colors.primary else Enroll.colors.surfaceSubtle,
        label = "chipBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Enroll.colors.onPrimary else Enroll.colors.textSecondary,
        label = "chipFg",
    )
    Text(
        text = label,
        style = Enroll.type.labelBold.colored(fg),
        modifier = Modifier
            .pressScale(ix)
            .clip(Enroll.shape.pill)
            .background(bg)
            .clickable(interactionSource = ix, indication = null, onClick = onClick)
            .padding(horizontal = Enroll.space.lg, vertical = Enroll.space.sm),
    )
}
