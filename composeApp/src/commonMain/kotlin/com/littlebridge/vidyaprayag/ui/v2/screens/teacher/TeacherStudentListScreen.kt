package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.EnrollCard
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatarSize
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.pressScale

/**
 * TeacherStudentListItem — one student in the roster list (Loop task P7-T5).
 *
 * A pure UI model the host maps from the student repository. `classLabel` is the
 * display class+section (e.g. "8-A") so the flat list still tells the teacher which
 * class each student belongs to.
 */
data class TeacherStudentListItem(
    val studentId: String,
    val name: String,
    val classLabel: String,
    val rollNumber: String? = null,
    val avatarUrl: String? = null,
)

/**
 * TeacherStudentListScreen — the student-list destination (Loop task P7-T5, STUB).
 *
 * The terminal screen for the "Total Students" profile stat deep link
 * ([TeacherDestination.StudentList], produced by `TeacherNavRouter
 * .totalStudentsDestination`). The spec asks for "a student list screen (stub with
 * correct navigation)" — so this is a real, token-correct list UI wired to the right
 * destination, but its DATA is host-supplied and the per-row tap is left as a TODO
 * for the host to wire (e.g. into the student profile) once that screen exists.
 *
 * TODO(host): provide `students` from a `TeacherStudentListViewModel` (roster across
 * the teacher's classes, optionally filtered by `TeacherDestination.StudentList
 * .classId`) and wire `onOpenStudent(studentId)` to the student-detail screen when it
 * lands. Until then the rows render and navigate-back correctly — no dead end.
 *
 * Layout: custom back top-bar (ChatTopBar idiom) + a `LazyColumn` of [StudentRow]s in
 * one [EnrollCard], each with a [VAvatar], name, and class/roll caption. All
 * colour/type via `Enroll.*` → VTheme; no new hex.
 */
@Composable
fun TeacherStudentListScreen(
    students: List<TeacherStudentListItem>,
    onBack: () -> Unit,
    onOpenStudent: (studentId: String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "All Students",
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Enroll.colors.surfaceBase),
    ) {
        StudentListTopBar(title = title, count = students.size, onBack = onBack)

        if (students.isEmpty()) {
            EmptyStudentList()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Enroll.space.screen,
                    vertical = Enroll.space.lg,
                ),
                verticalArrangement = Arrangement.spacedBy(Enroll.space.sm),
            ) {
                items(items = students, key = { it.studentId }) { student ->
                    EnrollCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenStudent(student.studentId) },
                        padding = Enroll.space.md,
                    ) {
                        StudentRow(student)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentRow(student: TeacherStudentListItem) {
    val caption = listOfNotNull(
        student.classLabel.takeIf { it.isNotBlank() },
        student.rollNumber?.let { "Roll $it" },
    ).joinToString("  ·  ")

    Row(verticalAlignment = Alignment.CenterVertically) {
        VAvatar(name = student.name, src = student.avatarUrl, size = VAvatarSize.Medium)
        Spacer(Modifier.width(Enroll.space.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = student.name,
                style = Enroll.type.labelBold.colored(Enroll.colors.textPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (caption.isNotEmpty()) {
                Spacer(Modifier.height(Enroll.space.xs))
                Text(
                    text = caption,
                    style = Enroll.type.bodySmall.colored(Enroll.colors.textSecondary),
                    maxLines = 1,
                )
            }
        }
        Icon(VIcons.ChevronRight, contentDescription = null, tint = Enroll.colors.textTertiary)
    }
}

/** Calm empty state when the roster is unavailable / not yet loaded. */
@Composable
private fun EmptyStudentList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Enroll.space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            VIcons.User,
            contentDescription = null,
            tint = Enroll.colors.textTertiary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(Enroll.space.md))
        Text(
            text = "No students to show yet",
            style = Enroll.type.bodyMedium.colored(Enroll.colors.textSecondary),
        )
    }
}

/** Custom back top-bar with a live student count — same idiom as ChatTopBar. */
@Composable
private fun StudentListTopBar(title: String, count: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Enroll.colors.surfaceCard)
            .statusBarsPadding()
            .padding(horizontal = Enroll.space.md, vertical = Enroll.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ix = remember { MutableInteractionSource() }
        Box(
            Modifier
                .pressScale(ix)
                .size(36.dp)
                .clip(Enroll.shape.pill)
                .clickable(interactionSource = ix, indication = null, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                VIcons.ArrowLeft,
                contentDescription = "Back",
                tint = Enroll.colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(Enroll.space.sm))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = Enroll.type.labelBold.colored(Enroll.colors.textPrimary),
                maxLines = 1,
            )
            if (count > 0) {
                Text(
                    text = if (count == 1) "1 student" else "$count students",
                    style = Enroll.type.bodySmall.colored(Enroll.colors.textSecondary),
                    maxLines = 1,
                )
            }
        }
    }
}
