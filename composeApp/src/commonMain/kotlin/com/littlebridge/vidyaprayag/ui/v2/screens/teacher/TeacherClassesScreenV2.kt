package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ClassAssessmentDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ClassDetailData
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ClassHomeworkDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.RosterStudentDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherClassSummaryDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.WeeklyPeriodDto
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherClassesViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * CLASSES tab — the teacher's roster plane.
 *
 *   list → tap a class → rich composite detail (next class, weekly timetable,
 *   attendance summary, scheduled tests, active homework, full roster) → tap a
 *   student → scoped read-only profile (attendance / performance / flags / parent
 *   contact).
 *
 * Built ground-up from the Parents-Portal vocabulary: lavender canvas, white
 * rounded TCards with hairline borders, Canvas TRings, brand violet reserved for
 * active/brand moments. No old teacher layout is mimicked.
 *
 * Self-contained: detail and student-profile are rendered in-tab (the shell does
 * not need to know about them) so the dock/header stay put.
 */
@Composable
fun TeacherClassesScreenV2(
    modifier: Modifier = Modifier,
    viewModel: TeacherClassesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    // Student-profile drill-down lives here (over the class detail).
    var openStudentId by remember { mutableStateOf<String?>(null) }

    AnimatedContent(
        targetState = Triple(state.openAssignmentId, openStudentId, state.classes.size),
        transitionSpec = { (fadeIn() togetherWith fadeOut()) },
        label = "classes-nav",
        modifier = modifier.fillMaxSize(),
    ) { (assignmentId, studentId, _) ->
        when {
            studentId != null -> TeacherStudentProfilePane(
                studentId = studentId,
                onBack = { openStudentId = null },
            )
            assignmentId != null -> ClassDetailPane(
                state = state,
                onBack = { viewModel.closeClass() },
                onRetry = { viewModel.retryDetail() },
                onOpenStudent = { openStudentId = it },
            )
            else -> ClassListPane(
                state = state,
                onSearch = viewModel::setSearch,
                onCycleFilter = viewModel::cycleFilter,
                onOpenClass = viewModel::openClass,
                onRefresh = viewModel::refresh,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LIST
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClassListPane(
    state: com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherClassesState,
    onSearch: (String) -> Unit,
    onCycleFilter: () -> Unit,
    onOpenClass: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val c = VTheme.colors
    when {
        state.isLoading && state.classes.isEmpty() -> TeacherCenterState { TeacherSpinner() }
        state.error != null && state.classes.isEmpty() -> TeacherCenterState {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Couldn't load your classes", style = VTheme.type.bodyStrong.colored(c.navyDeep))
                Spacer(Modifier.height(4.dp))
                Text(state.error, style = VTheme.type.caption.colored(c.ink3))
                Spacer(Modifier.height(14.dp))
                VButton("Try again", onClick = onRefresh, size = VButtonSize.Sm, tone = VButtonTone.Lavender)
            }
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text("Classes", style = VTheme.type.h1.colored(c.navyDeep))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${state.classes.size} ${if (state.classes.size == 1) "class" else "classes"} you teach",
                        style = VTheme.type.body.colored(c.ink3),
                    )
                    Spacer(Modifier.height(12.dp))
                    VInput(
                        value = state.search,
                        onValueChange = onSearch,
                        placeholder = "Search class, section or subject",
                        leadingIcon = VIcons.Search,
                    )
                    Spacer(Modifier.height(10.dp))
                    FilterChipRow(filter = state.classTeacherFilter, onCycle = onCycleFilter)
                }
            }
            if (state.visibleClasses.isEmpty()) {
                item {
                    TCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("No classes match", style = VTheme.type.bodyStrong.colored(c.navyDeep))
                            Spacer(Modifier.height(4.dp))
                            Text("Try a different search or filter.", style = VTheme.type.caption.colored(c.ink3))
                        }
                    }
                }
            } else {
                items(state.visibleClasses, key = { it.assignmentId }) { cls ->
                    ClassCard(cls = cls, onClick = { onOpenClass(cls.assignmentId) })
                }
            }
        }
    }
}

@Composable
private fun FilterChipRow(filter: Boolean?, onCycle: () -> Unit) {
    val c = VTheme.colors
    val (label, active) = when (filter) {
        null -> "All classes" to false
        true -> "Class teacher" to true
        false -> "Subject only" to true
    }
    val ix = remember { MutableInteractionSource() }
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) c.accentTint else c.cream)
            .border(1.dp, if (active) c.accent.copy(alpha = 0.35f) else c.hairline, RoundedCornerShape(999.dp))
            .clickable(interactionSource = ix, indication = null, onClick = onCycle)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(VIcons.Filter, contentDescription = null, tint = if (active) c.accentDeep else c.ink3, modifier = Modifier.size(15.dp))
        Text(
            label,
            style = VTheme.type.label.colored(if (active) c.accentDeep else c.ink2).copy(fontWeight = FontWeight.Bold),
        )
        Text("· tap to switch", style = VTheme.type.label.colored(c.ink3).copy(fontSize = 9.sp))
    }
}

@Composable
private fun ClassCard(cls: TeacherClassSummaryDto, onClick: () -> Unit) {
    val c = VTheme.colors
    val subjectColor = teacherSubjectColor(c, cls.subject)
    TCard(onClick = onClick) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TIconDisc(
                    icon = VIcons.School,
                    tint = subjectColor,
                    bg = subjectColor.copy(alpha = 0.12f),
                    size = 46.dp,
                    glyph = 22.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${cls.className} · ${cls.section}",
                            style = VTheme.type.h3.colored(c.navyDeep),
                        )
                        if (cls.isClassTeacher) {
                            TPill("CLASS TEACHER", c.accentTint, c.accentDeep)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(cls.subject, style = VTheme.type.body.colored(subjectColor).copy(fontWeight = FontWeight.SemiBold))
                }
                Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip(icon = VIcons.Users, text = "${cls.studentCount} students")
                if (cls.todayAttendanceMarked) {
                    MetaChip(icon = VIcons.Check, text = "Attendance done", tint = c.successInk, bg = c.success.copy(alpha = 0.14f))
                } else {
                    MetaChip(icon = VIcons.ClipboardList, text = "Mark attendance", tint = c.warningInk, bg = c.warning.copy(alpha = 0.16f))
                }
                if (cls.atRiskCount > 0) {
                    MetaChip(icon = VIcons.AlertTriangle, text = "${cls.atRiskCount} at risk", tint = c.dangerInk, bg = c.danger.copy(alpha = 0.12f))
                }
            }
            cls.nextPeriod?.let { np ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(VIcons.Clock, contentDescription = null, tint = c.ink3, modifier = Modifier.size(13.dp))
                    Text(
                        nextPeriodLabel(np.dayLabel, np.startTime, np.endTime, np.room, np.isToday),
                        style = VTheme.type.caption.colored(c.ink2),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color = VTheme.colors.ink2,
    bg: Color = VTheme.colors.cream,
) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
        Text(text, style = VTheme.type.label.colored(tint).copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp))
    }
}

private fun nextPeriodLabel(day: String, start: String, end: String, room: String, isToday: Boolean): String {
    val when0 = if (isToday) "Today" else day
    val room0 = if (room.isBlank()) "" else " · $room"
    return "Next: $when0 $start–$end$room0"
}

// ─────────────────────────────────────────────────────────────────────────────
// DETAIL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClassDetailPane(
    state: com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherClassesState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenStudent: (String) -> Unit,
) {
    val c = VTheme.colors
    val detail = state.detail
    Column(Modifier.fillMaxSize()) {
        TeacherSubHeader(
            title = detail?.let { "${it.header.className} · ${it.header.section}" } ?: "Class",
            subtitle = detail?.header?.subject,
            onBack = onBack,
        )
        when {
            state.detailLoading -> TeacherCenterState { TeacherSpinner() }
            state.detailError != null -> TeacherCenterState {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn't load this class", style = VTheme.type.bodyStrong.colored(c.navyDeep))
                    Spacer(Modifier.height(4.dp))
                    Text(state.detailError, style = VTheme.type.caption.colored(c.ink3))
                    Spacer(Modifier.height(14.dp))
                    VButton("Try again", onClick = onRetry, size = VButtonSize.Sm, tone = VButtonTone.Lavender)
                }
            }
            detail != null -> ClassDetailBody(detail = detail, onOpenStudent = onOpenStudent)
            else -> TeacherCenterState { TeacherSpinner() }
        }
    }
}

@Composable
private fun ClassDetailBody(detail: ClassDetailData, onOpenStudent: (String) -> Unit) {
    val c = VTheme.colors
    val subjectColor = teacherSubjectColor(c, detail.header.subject)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Next class + attendance hero
        item { NextClassCard(detail, subjectColor) }
        item { AttendanceSnapshotCard(detail) }

        // Weekly timetable
        if (detail.weeklyTimetable.isNotEmpty()) {
            item { TimetableCard(detail.weeklyTimetable) }
        }

        // Scheduled tests
        if (detail.assessmentSchedule.isNotEmpty()) {
            item { AssessmentScheduleCard(detail.assessmentSchedule) }
        }

        // Active homework
        if (detail.activeHomework.isNotEmpty()) {
            item { ActiveHomeworkCard(detail.activeHomework) }
        }

        // Roster
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Students", style = VTheme.type.h3.colored(c.navyDeep))
                Text("${detail.roster.size}", style = VTheme.type.bodyStrong.colored(c.ink3))
            }
        }
        items(detail.roster, key = { it.studentId }) { student ->
            RosterRow(student = student, onClick = { onOpenStudent(student.studentId) })
        }
        if (detail.roster.isEmpty()) {
            item {
                TCard {
                    Text("No students enrolled yet.", style = VTheme.type.body.colored(c.ink3), modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun NextClassCard(detail: ClassDetailData, subjectColor: Color) {
    val c = VTheme.colors
    TCard {
        Column {
            TEyebrow("NEXT CLASS", dot = subjectColor)
            Spacer(Modifier.height(10.dp))
            val np = detail.nextPeriod
            if (np == null) {
                Text("No upcoming period scheduled.", style = VTheme.type.body.colored(c.ink2))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TIconDisc(VIcons.Clock, subjectColor, subjectColor.copy(alpha = 0.12f), size = 44.dp, glyph = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (np.isToday) "Today" else np.dayLabel,
                            style = VTheme.type.h3.colored(c.navyDeep),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            buildString {
                                append("${np.startTime}–${np.endTime}")
                                if (np.room.isNotBlank()) append(" · ${np.room}")
                            },
                            style = VTheme.type.body.colored(c.ink2),
                        )
                    }
                    if (np.isToday) TPill("TODAY", c.accentTint, c.accentDeep)
                }
            }
        }
    }
}

@Composable
private fun AttendanceSnapshotCard(detail: ClassDetailData) {
    val c = VTheme.colors
    val a = detail.attendanceSummary
    val total = (a.presentToday + a.absentToday + a.lateToday + a.leaveToday).coerceAtLeast(1)
    val presentPct = ((a.presentToday + a.lateToday).toFloat() / total * 100f).toInt()
    TCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TEyebrow("ATTENDANCE TODAY")
                if (a.todayMarked) TPill("MARKED", c.success.copy(alpha = 0.16f), c.successInk)
                else TPill("NOT MARKED", c.warning.copy(alpha = 0.18f), c.warningInk)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TRing(
                    percent = if (a.todayMarked) presentPct else 0,
                    accent = c.success,
                    modifier = Modifier.size(72.dp),
                    label = if (a.todayMarked) "$presentPct%" else "—",
                    labelSize = 16.sp,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        TMetricTile("${a.presentToday}", "Present", c.success, Modifier.weight(1f))
                        TMetricTile("${a.absentToday}", "Absent", c.danger, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        TMetricTile("${a.lateToday}", "Late", c.warning, Modifier.weight(1f))
                        TMetricTile("${a.leaveToday}", "Leave", c.accent, Modifier.weight(1f))
                    }
                }
            }
            if (a.weekRate != null || a.monthRate != null) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    a.weekRate?.let {
                        TMetricTile("${(it * 100).toInt()}%", "This week", c.tealDeep, Modifier.weight(1f))
                    }
                    a.monthRate?.let {
                        TMetricTile("${(it * 100).toInt()}%", "This month", c.navy, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableCard(periods: List<WeeklyPeriodDto>) {
    val c = VTheme.colors
    TCard {
        Column {
            TEyebrow("WEEKLY TIMETABLE")
            Spacer(Modifier.height(10.dp))
            periods.forEachIndexed { i, p ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (p.isToday) c.accentTint else c.cream)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        p.dayLabel,
                        style = VTheme.type.bodyStrong.colored(if (p.isToday) c.accentDeep else c.navyDeep),
                        modifier = Modifier.width(48.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${p.startTime}–${p.endTime}",
                        style = VTheme.type.body.colored(c.ink2),
                        modifier = Modifier.weight(1f),
                    )
                    if (p.room.isNotBlank()) {
                        Text(p.room, style = VTheme.type.caption.colored(c.ink3))
                    }
                    if (p.isToday) {
                        Spacer(Modifier.width(8.dp))
                        TPill("TODAY", c.accent.copy(alpha = 0.18f), c.accentDeep)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssessmentScheduleCard(items: List<ClassAssessmentDto>) {
    val c = VTheme.colors
    TCard {
        Column {
            TEyebrow("SCHEDULED TESTS")
            Spacer(Modifier.height(10.dp))
            items.forEachIndexed { i, a ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TIconDisc(VIcons.GraduationCap, c.navy, c.navy.copy(alpha = 0.10f), size = 38.dp, glyph = 18.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.name, style = VTheme.type.bodyStrong.colored(c.navyDeep))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            buildString {
                                append(a.type)
                                if (!a.examDate.isNullOrBlank()) append(" · ${prettyDateShort(a.examDate)}")
                            },
                            style = VTheme.type.caption.colored(c.ink3),
                        )
                    }
                    AssessmentStatusPill(a.status)
                }
            }
        }
    }
}

@Composable
private fun AssessmentStatusPill(status: String) {
    val c = VTheme.colors
    val (bg, fg, label) = when (status.lowercase()) {
        "published" -> Triple(c.success.copy(alpha = 0.16f), c.successInk, "PUBLISHED")
        "graded", "completed" -> Triple(c.teal.copy(alpha = 0.18f), c.tealDeep, "GRADED")
        "scheduled", "upcoming" -> Triple(c.accentTint, c.accentDeep, "SCHEDULED")
        else -> Triple(c.cream, c.ink2, status.uppercase())
    }
    TPill(label, bg, fg)
}

@Composable
private fun ActiveHomeworkCard(items: List<ClassHomeworkDto>) {
    val c = VTheme.colors
    TCard {
        Column {
            TEyebrow("ACTIVE HOMEWORK")
            Spacer(Modifier.height(10.dp))
            items.forEachIndexed { i, h ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                val total = (h.submittedCount + h.notSubmittedCount).coerceAtLeast(1)
                val pct = (h.submittedCount.toFloat() / total * 100f).toInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TRing(percent = pct, accent = c.teal, modifier = Modifier.size(42.dp), labelSize = 11.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(h.title, style = VTheme.type.bodyStrong.colored(c.navyDeep))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            buildString {
                                append("${h.submittedCount}/$total turned in")
                                if (!h.dueDate.isNullOrBlank()) append(" · due ${prettyDateShort(h.dueDate)}")
                            },
                            style = VTheme.type.caption.colored(c.ink3),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RosterRow(student: RosterStudentDto, onClick: () -> Unit) {
    val c = VTheme.colors
    val flag = primaryFlag(student.flags)
    TCard(onClick = onClick, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // avatar / roll disc
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(c.lavenderLight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    student.roll?.toString() ?: student.name.take(1).uppercase(),
                    style = VTheme.type.bodyStrong.colored(c.accentDeep),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(student.name, style = VTheme.type.bodyStrong.colored(c.navyDeep))
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    student.attendanceRate?.let {
                        Text("${(it * 100).toInt()}% present", style = VTheme.type.caption.colored(c.ink3))
                    }
                    student.latestMark?.let { m ->
                        Text("${fmt1(m.marks.toFloat())}/${m.max} · ${m.name}", style = VTheme.type.caption.colored(c.ink3))
                    }
                }
            }
            if (flag != null) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(flag))
                Spacer(Modifier.width(6.dp))
            }
            Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
        }
    }
}

/** Map a student's flag codes (Doc 09 §5) to a severity dot colour; null if benign. */
@Composable
private fun primaryFlag(flags: List<String>): Color? {
    val c = VTheme.colors
    return when {
        flags.any { it in DANGER_FLAGS } -> c.danger
        flags.any { it in WARNING_FLAGS } -> c.warning
        else -> null
    }
}

private val DANGER_FLAGS = setOf("low_attendance", "recent_absences", "failing_trend")
private val WARNING_FLAGS = setOf("dropping")
