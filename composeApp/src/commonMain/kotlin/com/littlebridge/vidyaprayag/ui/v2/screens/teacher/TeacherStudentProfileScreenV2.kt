package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.ParentContactDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.StudentAttendanceDayDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.StudentPerformanceDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.StudentProfileData
import com.littlebridge.vidyaprayag.feature.teacher.presentation.StudentProfileState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherStudentProfileViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatarSize
import com.littlebridge.vidyaprayag.ui.v2.components.VBackHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * TeacherStudentProfileScreenV2 (T-505) — the read-only student drill-down reached
 * from a class roster row. Wired to [TeacherStudentProfileViewModel] →
 * `GET /teacher/students/{id}` (server enforces "you teach this student" — 403
 * renders a dedicated forbidden state, never a raw error).
 *
 * Sections (Doc 09 §4): header · attendance (rate + recent + trend) · performance
 * (published marks) · flags (server-computed, plain language) · parent contact
 * (privacy-gated — only present when the server returns it).
 */
@Composable
fun TeacherStudentProfileScreenV2(
    studentId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeacherStudentProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    LaunchedEffect(studentId) { viewModel.load(studentId) }
    StudentProfileContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
private fun StudentProfileContent(
    state: StudentProfileState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(modifier.fillMaxSize()) {
        VBackHeader(title = state.profile?.name ?: "Student", onBack = onBack)

        if (state.forbidden) {
            ForbiddenState()
            return@Column
        }

        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = state.profile == null,
            emptyTitle = "Profile unavailable",
            emptyBody = "We couldn't load this student. Try again.",
            emptyIcon = VIcons.User,
            onRetry = onRetry,
        ) {
            val p = state.profile!!
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProfileHeader(p)
                AttendanceSection(p)
                PerformanceSection(p.performance)
                FlagsSection(p.flags)
                p.parentContact?.let { ParentContactSection(it) }
            }
        }
    }
}

@Composable
private fun ProfileHeader(p: StudentProfileData) {
    val c = VTheme.colors
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            VAvatar(name = p.name, src = p.photoUrl, size = VAvatarSize.Large)
            Column(Modifier.weight(1f)) {
                Text(p.name, style = VTheme.type.h3.colored(c.ink).copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
                Text(
                    buildString {
                        append("${p.className} ${p.section}")
                        p.roll?.let { append(" · Roll #$it") }
                    },
                    style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp),
                )
            }
        }
    }
}

@Composable
private fun AttendanceSection(p: StudentProfileData) {
    val c = VTheme.colors
    SectionLabel("Attendance")
    VCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    p.attendance.rate?.let { "${(it * 100).roundToInt()}%" } ?: "—",
                    style = VTheme.type.data.colored(p.attendance.rate?.let { attendanceTint(it) } ?: c.ink).copy(fontSize = 22.sp),
                )
                Text("30-day rate", style = VTheme.type.label.colored(c.ink3).copy(letterSpacing = TextUnit.Unspecified, fontSize = 10.sp))
            }
            TrendBadge(p.attendance.trend)
        }
        if (p.attendance.recent.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                p.attendance.recent.take(8).forEach { day -> AttendanceDot(day) }
            }
        }
    }
}

@Composable
private fun TrendBadge(trend: String) {
    when (trend) {
        "improving" -> VBadge(text = "Improving", tone = VBadgeTone.Success, leadingIcon = VIcons.TrendingUp)
        "declining" -> VBadge(text = "Declining", tone = VBadgeTone.Danger, leadingIcon = VIcons.AlertTriangle)
        "flat" -> VBadge(text = "Steady", tone = VBadgeTone.Neutral)
        else -> {} // "none" — no history yet, render nothing
    }
}

@Composable
private fun AttendanceDot(day: StudentAttendanceDayDto) {
    val c = VTheme.colors
    val tone = when (day.status) {
        "present" -> c.successInk
        "absent" -> c.dangerInk
        "late" -> c.warningInk
        else -> c.ink3
    }
    val letter = when (day.status) {
        "present" -> "P"; "absent" -> "A"; "late" -> "L"; "leave" -> "Lv"; else -> "?"
    }
    Box(
        Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(tone.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, style = VTheme.type.label.colored(tone).copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun PerformanceSection(performance: List<StudentPerformanceDto>) {
    val c = VTheme.colors
    SectionLabel("Performance")
    if (performance.isEmpty()) {
        EmptyLine("No marks recorded yet.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            performance.forEach { m ->
                VCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(m.assessmentName, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 14.sp))
                            Text(
                                listOfNotNull(m.subject, m.date).joinToString(" · "),
                                style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
                            )
                        }
                        val marks = m.marks
                        if (m.isAbsent || marks == null) {
                            VBadge(text = "Absent", tone = VBadgeTone.Neutral)
                        } else {
                            Text(
                                "${formatMarksP(marks)}/${m.max}",
                                style = VTheme.type.data.colored(c.ink).copy(fontSize = 16.sp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagsSection(flags: List<String>) {
    if (flags.isEmpty() || (flags.size == 1 && flags.first() == "no_data")) {
        if (flags.firstOrNull() == "no_data") {
            SectionLabel("Signals")
            EmptyLine("Not enough data to flag yet.")
        }
        return
    }
    SectionLabel("Signals")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        flags.forEach { f -> VBadge(text = flagLabelP(f), tone = flagToneP(f)) }
    }
}

@Composable
private fun ParentContactSection(contact: ParentContactDto) {
    val c = VTheme.colors
    SectionLabel("Parent contact")
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(VIcons.Phone, contentDescription = null, tint = c.teal, modifier = Modifier.size(16.dp))
            Column {
                contact.name?.let { Text(it, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 14.sp)) }
                contact.phone?.let { Text(it, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 13.sp)) }
            }
        }
    }
}

@Composable
private fun ForbiddenState() {
    val c = VTheme.colors
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(VIcons.AlertTriangle, contentDescription = null, tint = c.ink3, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text("Not your student", style = VTheme.type.h3.colored(c.ink))
        Spacer(Modifier.height(4.dp))
        Text(
            "You can only view profiles of students in the classes you teach.",
            style = VTheme.type.body.colored(c.ink3),
        )
    }
}

// ── small shared pieces (file-private; sibling screen has its own) ───────────

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

private fun formatMarksP(m: Double): String =
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

private fun flagLabelP(code: String): String = when (code) {
    "low_attendance" -> "Low attendance"
    "recent_absences" -> "Recent absences"
    "failing_trend" -> "Failing trend"
    "dropping" -> "Dropping"
    "no_data" -> "No data"
    else -> code.replace('_', ' ')
}

private fun flagToneP(code: String): VBadgeTone = when (code) {
    "low_attendance", "failing_trend" -> VBadgeTone.Danger
    "recent_absences", "dropping" -> VBadgeTone.Warning
    else -> VBadgeTone.Neutral
}
