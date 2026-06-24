package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ClassAssessmentDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ClassDetailData
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ClassHomeworkDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.NextPeriodDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.RosterStudentDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherClassSummaryDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.WeeklyPeriodDto
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherClassesState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherClassesViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatarSize
import com.littlebridge.vidyaprayag.ui.v2.components.VBackHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VTag
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * TeacherClassesScreenV2 (T-504) — the rebuilt Classes tab.
 *
 * List → aggregated cards (student count, real class-teacher badge, next period,
 * today's attendance state, at-risk count) from `GET /teacher/classes-v2`; filter
 * chips (All / Class teacher / Subject) + search.
 *
 * Detail → one composite payload (`GET /teacher/classes-v2/{id}`): header, next
 * period, weekly timetable, attendance summary, assessment schedule, active
 * homework, and the REAL roster (F-CLS-5: the `VComingSoon` placeholder is GONE).
 * Each roster row → student profile (T-505) via [onOpenStudent].
 *
 * Flags/at-risk are SERVER-computed (Doc 09 §5) — the UI only paints them, never
 * recomputes, so list/detail/profile can never disagree.
 */
@Composable
fun TeacherClassesScreenV2(
    onOpenStudent: (String) -> Unit = {},
    onMarkAttendance: (assignmentId: String, scope: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: TeacherClassesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    TeacherClassesContent(
        state = state,
        onOpenClass = viewModel::openClass,
        onCloseClass = viewModel::closeClass,
        onRetryList = viewModel::refresh,
        onRetryDetail = viewModel::retryDetail,
        onSearch = viewModel::setSearch,
        onFilter = viewModel::setFilter,
        onOpenStudent = onOpenStudent,
        onMarkAttendance = onMarkAttendance,
        onMessageParents = viewModel::openBroadcast,
        onSendBroadcast = viewModel::sendBroadcast,
        onCloseBroadcast = viewModel::closeBroadcast,
        modifier = modifier,
    )
}

@Composable
private fun TeacherClassesContent(
    state: TeacherClassesState,
    onOpenClass: (String) -> Unit,
    onCloseClass: () -> Unit,
    onRetryList: () -> Unit,
    onRetryDetail: () -> Unit,
    onSearch: (String) -> Unit,
    onFilter: (Boolean?) -> Unit,
    onOpenStudent: (String) -> Unit,
    onMarkAttendance: (assignmentId: String, scope: String) -> Unit,
    onMessageParents: (String) -> Unit,
    onSendBroadcast: (String) -> Unit,
    onCloseBroadcast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors

    // RA-51 broadcast composer overlay.
    if (state.broadcastClassName != null) {
        ClassBroadcastComposer(
            className = state.broadcastClassName!!,
            sending = state.broadcasting,
            error = state.broadcastError,
            sentCount = state.broadcastResultCount,
            onSend = onSendBroadcast,
            onClose = onCloseBroadcast,
            modifier = modifier,
        )
        return
    }

    // Class detail overlay.
    if (state.openAssignmentId != null) {
        val summary = state.classes.firstOrNull { it.assignmentId == state.openAssignmentId }
        ClassDetailScreen(
            title = summary?.let { "${it.className} ${it.section}" } ?: "Class",
            loading = state.detailLoading,
            error = state.detailError,
            detail = state.detail,
            onBack = onCloseClass,
            onRetry = onRetryDetail,
            onOpenStudent = onOpenStudent,
            onMarkAttendance = {
                summary?.let {
                    onMarkAttendance(
                        it.assignmentId,
                        listOfNotNull(
                            "${it.className} ${it.section}".trim().takeIf { s -> s.isNotBlank() },
                            it.subject.takeIf { s -> s.isNotBlank() },
                        ).joinToString(" · "),
                    )
                }
            },
            onMessageParents = { summary?.let { onMessageParents(it.className) } },
            modifier = modifier,
        )
        return
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("My Classes", style = VTheme.type.h1.colored(c.ink))

        // Search + filter chips (only meaningful once classes exist).
        if (!state.isLoading && state.error == null && state.classes.isNotEmpty()) {
            VInput(
                value = state.search,
                onValueChange = onSearch,
                placeholder = "Search class or subject…",
                leadingIcon = VIcons.Search,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VTag(text = "All", active = state.classTeacherFilter == null, onClick = { onFilter(null) })
                VTag(text = "Class teacher", active = state.classTeacherFilter == true, onClick = { onFilter(true) })
                VTag(text = "Subject", active = state.classTeacherFilter == false, onClick = { onFilter(false) })
            }
        }

        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = state.classes.isEmpty(),
            emptyTitle = "No classes assigned to you yet",
            emptyBody = "Classes you teach will appear here once your admin assigns them.",
            emptyIcon = VIcons.Users,
            onRetry = onRetryList,
        ) {
            val visible = state.visibleClasses
            if (visible.isEmpty()) {
                Text("No classes match your filter.", style = VTheme.type.body.colored(c.ink3))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    visible.forEach { cls ->
                        ClassCard(
                            cls = cls,
                            onClick = { onOpenClass(cls.assignmentId) },
                            onMarkAttendance = {
                                onMarkAttendance(
                                    cls.assignmentId,
                                    listOfNotNull(
                                        "${cls.className} ${cls.section}".trim().takeIf { it.isNotBlank() },
                                        cls.subject.takeIf { it.isNotBlank() },
                                    ).joinToString(" · "),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassCard(
    cls: TeacherClassSummaryDto,
    onClick: () -> Unit,
    onMarkAttendance: () -> Unit,
) {
    val c = VTheme.colors
    VCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${cls.className} ${cls.section}",
                        style = VTheme.type.h3.colored(c.ink).copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    )
                    if (cls.isClassTeacher) VBadge(text = "Class teacher", tone = VBadgeTone.Accent)
                }
                Text(cls.subject, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp))
            }
            Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip(VIcons.Users, "${cls.studentCount}", "students")
            if (cls.todayAttendanceMarked) {
                VBadge(text = "Marked today", tone = VBadgeTone.Success, leadingIcon = VIcons.Check)
            } else {
                VBadge(text = "Not marked", tone = VBadgeTone.Warning, leadingIcon = VIcons.AlertTriangle)
            }
            if (cls.atRiskCount > 0) {
                VBadge(text = "${cls.atRiskCount} at risk", tone = VBadgeTone.Danger, leadingIcon = VIcons.AlertCircle)
            }
        }

        cls.nextPeriod?.let { np ->
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(VIcons.Clock, contentDescription = null, tint = c.ink3, modifier = Modifier.size(14.dp))
                Text(nextPeriodLabel(np), style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp))
            }
        }

        // Attendance is always one tap from the class — independent of today's schedule
        // (this is the path that makes attendance reachable on holidays / off-timetable days).
        Spacer(Modifier.height(12.dp))
        VButton(
            text = if (cls.todayAttendanceMarked) "Update attendance" else "Mark attendance",
            onClick = onMarkAttendance,
            full = true,
            size = VButtonSize.Sm,
            variant = if (cls.todayAttendanceMarked) VButtonVariant.Secondary else VButtonVariant.Primary,
            tone = VButtonTone.Lavender,
            leading = {
                Icon(
                    VIcons.Check,
                    contentDescription = null,
                    tint = if (cls.todayAttendanceMarked) c.ink2 else c.card,
                    modifier = Modifier.size(16.dp),
                )
            },
        )
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    val c = VTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = c.ink3, modifier = Modifier.size(14.dp))
        Text(value, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 13.sp))
        Text(label, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
    }
}

private fun nextPeriodLabel(np: NextPeriodDto): String {
    val day = if (np.isToday) "Today" else np.dayLabel
    val room = if (np.room.isNotBlank()) " · ${np.room}" else ""
    return "Next: $day ${np.startTime}$room"
}

// ─────────────────────────────────────────────────────────────────────────────
// Class detail
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClassDetailScreen(
    title: String,
    loading: Boolean,
    error: String?,
    detail: ClassDetailData?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenStudent: (String) -> Unit,
    onMarkAttendance: () -> Unit,
    onMessageParents: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier.fillMaxSize()) {
        VBackHeader(title = title, onBack = onBack)
        VStateHost(
            loading = loading,
            error = error,
            isEmpty = detail == null,
            emptyTitle = "Class detail unavailable",
            emptyBody = "We couldn't load this class. Pull to refresh or try again.",
            onRetry = onRetry,
        ) {
            val d = detail!!
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat(d.header.studentCount.toString(), "Students", Modifier.weight(1f))
                    MiniStat(rateText(d.attendanceSummary.monthRate), "Month att.", Modifier.weight(1f))
                    MiniStat(rateText(d.attendanceSummary.weekRate), "Week att.", Modifier.weight(1f))
                }
                if (d.header.isClassTeacher) VBadge(text = "You are the class teacher", tone = VBadgeTone.Accent)

                VButton(
                    text = "Mark attendance",
                    onClick = onMarkAttendance,
                    full = true,
                    variant = VButtonVariant.Primary,
                    tone = VButtonTone.Lavender,
                    leading = {
                        Icon(VIcons.Check, contentDescription = null, tint = VTheme.colors.card, modifier = Modifier.size(18.dp))
                    },
                )

                VButton(
                    text = "Message class parents",
                    onClick = onMessageParents,
                    full = true,
                    variant = VButtonVariant.Secondary,
                )

                d.nextPeriod?.let { np ->
                    SectionLabel("Next period")
                    VCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(VIcons.Clock, contentDescription = null, tint = VTheme.colors.teal, modifier = Modifier.size(16.dp))
                            Text(nextPeriodLabel(np), style = VTheme.type.body.colored(VTheme.colors.ink))
                        }
                    }
                }

                SectionLabel("Today's attendance")
                AttendanceTodayCard(d)

                if (d.weeklyTimetable.isNotEmpty()) {
                    SectionLabel("Weekly timetable")
                    VCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            d.weeklyTimetable.forEach { p -> TimetableRow(p) }
                        }
                    }
                }

                SectionLabel("Assessments")
                if (d.assessmentSchedule.isEmpty()) {
                    EmptyLine("No assessments yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        d.assessmentSchedule.forEach { AssessmentRow(it) }
                    }
                }

                SectionLabel("Active homework")
                if (d.activeHomework.isEmpty()) {
                    EmptyLine("No active homework.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        d.activeHomework.forEach { HomeworkRow(it) }
                    }
                }

                // Roster — the core fix (F-CLS-5).
                SectionLabel("Roster (${d.roster.size})")
                if (d.roster.isEmpty()) {
                    EmptyLine("No students enrolled in this class yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        d.roster.forEach { s -> RosterRow(s, onClick = { onOpenStudent(s.studentId) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceTodayCard(d: ClassDetailData) {
    val c = VTheme.colors
    val a = d.attendanceSummary
    VCard {
        if (!a.todayMarked) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(VIcons.AlertTriangle, contentDescription = null, tint = c.warningInk, modifier = Modifier.size(16.dp))
                Text("Not marked yet today.", style = VTheme.type.body.colored(c.ink))
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CountCell(a.presentToday, "Present", c.successInk)
                CountCell(a.absentToday, "Absent", c.dangerInk)
                CountCell(a.lateToday, "Late", c.warningInk)
                CountCell(a.leaveToday, "Leave", c.ink2)
            }
        }
    }
}

@Composable
private fun CountCell(value: Int, label: String, tint: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = VTheme.type.data.colored(tint).copy(fontSize = 18.sp))
        Text(label, style = VTheme.type.label.colored(VTheme.colors.ink3).copy(letterSpacing = TextUnit.Unspecified, fontSize = 10.sp))
    }
}

@Composable
private fun TimetableRow(p: WeeklyPeriodDto) {
    val c = VTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                p.dayLabel,
                style = VTheme.type.bodyStrong.colored(if (p.isToday) c.teal else c.ink).copy(fontSize = 13.sp),
            )
            Text("${p.startTime}–${p.endTime}", style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp))
        }
        if (p.room.isNotBlank()) Text(p.room, style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp))
    }
}

@Composable
private fun AssessmentRow(a: ClassAssessmentDto) {
    val c = VTheme.colors
    val tone = when (a.status) {
        "published" -> VBadgeTone.Success
        "marks_pending" -> VBadgeTone.Warning
        else -> VBadgeTone.Neutral
    }
    VCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(a.name, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 14.sp))
                Text(
                    listOfNotNull(a.type, a.examDate).joinToString(" · "),
                    style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
                )
            }
            VBadge(text = a.status.replace('_', ' '), tone = tone)
        }
    }
}

@Composable
private fun HomeworkRow(h: ClassHomeworkDto) {
    val c = VTheme.colors
    VCard {
        Column {
            Text(h.title, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 14.sp))
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                h.dueDate?.let { Text("Due $it", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp)) }
                Text(
                    "${h.submittedCount} submitted · ${h.notSubmittedCount} pending",
                    style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 11.sp),
                )
            }
        }
    }
}

@Composable
private fun RosterRow(s: RosterStudentDto, onClick: () -> Unit) {
    val c = VTheme.colors
    VCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VAvatar(name = s.name, src = s.photoUrl, size = VAvatarSize.Small)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 14.sp))
                    s.roll?.let { Text("#$it", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    s.attendanceRate?.let {
                        Text(
                            "${(it * 100).roundToInt()}% att.",
                            style = VTheme.type.caption.colored(attendanceTint(it)).copy(fontSize = 12.sp),
                        )
                    }
                    s.latestMark?.let { m ->
                        Text(
                            "${m.name} ${formatMarks(m.marks)}/${m.max}",
                            style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp),
                        )
                    }
                }
                if (s.flags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        s.flags.take(3).forEach { f -> VBadge(text = flagLabel(f), tone = flagTone(f)) }
                    }
                }
            }
            Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small shared pieces
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = VTheme.type.label.colored(VTheme.colors.ink3).copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
    )
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = VTheme.type.body.colored(VTheme.colors.ink3).copy(fontSize = 13.sp))
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(c.ink.copy(alpha = 0.06f)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = VTheme.type.data.colored(c.ink).copy(fontSize = 18.sp))
        Text(label, style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = TextUnit.Unspecified, fontSize = 10.sp))
    }
}

private fun rateText(rate: Double?): String = if (rate == null) "—" else "${(rate * 100).roundToInt()}%"

private fun formatMarks(m: Double): String =
    if (m == m.toLong().toDouble()) m.toLong().toString() else m.toString()

@Composable
private fun attendanceTint(rate: Double): androidx.compose.ui.graphics.Color {
    val c = VTheme.colors
    return when {
        rate < 0.75 -> c.dangerInk
        rate < 0.85 -> c.warningInk
        else -> c.successInk
    }
}

/** Plain-language label for a server flag code (Doc 09 §5). */
private fun flagLabel(code: String): String = when (code) {
    "low_attendance" -> "Low attendance"
    "recent_absences" -> "Recent absences"
    "failing_trend" -> "Failing trend"
    "dropping" -> "Dropping"
    "no_data" -> "No data"
    else -> code.replace('_', ' ')
}

private fun flagTone(code: String): VBadgeTone = when (code) {
    "low_attendance", "failing_trend" -> VBadgeTone.Danger
    "recent_absences", "dropping" -> VBadgeTone.Warning
    else -> VBadgeTone.Neutral
}

// ─────────────────────────────────────────────────────────────────────────────
// RA-51 — message class parents composer (preserved from the prior screen).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClassBroadcastComposer(
    className: String,
    sending: Boolean,
    error: String?,
    sentCount: Int?,
    onSend: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var body by remember { mutableStateOf("") }

    Column(modifier.fillMaxSize()) {
        VBackHeader(title = "Message $className parents", onBack = onClose)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (sentCount != null) {
                VCard {
                    Text(
                        "Message delivered to $sentCount parent${if (sentCount == 1) "" else "s"}.",
                        style = VTheme.type.bodyStrong.colored(c.ink),
                    )
                    Spacer(Modifier.height(12.dp))
                    VButton(text = "Done", onClick = onClose, full = true)
                }
            } else {
                Text(
                    "Every parent of $className will receive this in their inbox.",
                    style = VTheme.type.caption.colored(c.ink2),
                )
                VInput(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = "Write your message…",
                    enabled = !sending,
                    singleLine = false,
                )
                if (error != null) Text(error, style = VTheme.type.caption.colored(c.dangerInk))
                VButton(
                    text = "Send to parents",
                    onClick = { if (body.isNotBlank()) onSend(body) },
                    full = true,
                    loading = sending,
                    enabled = body.isNotBlank() && !sending,
                )
            }
        }
    }
}
