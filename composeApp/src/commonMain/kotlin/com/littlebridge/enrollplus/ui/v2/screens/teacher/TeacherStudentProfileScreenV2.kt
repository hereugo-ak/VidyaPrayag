package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.StudentPerformanceDto
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.StudentProfileData
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherStudentProfileViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * Scoped student-profile drill-down (read-only). Loads `GET /teacher/students/{id}`
 * via [TeacherStudentProfileViewModel]; a 403 surfaces as a polite "not your
 * student" wall rather than an error. Attendance / performance / server-computed
 * flags / privacy-gated parent contact, in the Parents-Portal card vocabulary.
 *
 * Each pane gets its OWN VM instance (keyed by studentId via koinViewModel below
 * is shared, so we re-load on studentId change through LaunchedEffect).
 */
@Composable
fun TeacherStudentProfilePane(
    studentId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeacherStudentProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    LaunchedEffect(studentId) { viewModel.load(studentId) }

    val c = VTheme.colors
    Column(modifier.fillMaxSize()) {
        TeacherSubHeader(
            title = state.profile?.name ?: "Student",
            subtitle = state.profile?.let { "${it.className} · ${it.section}" },
            onBack = onBack,
        )
        when {
            state.isLoading -> TeacherCenterState { TeacherSpinner() }
            state.forbidden -> TeacherCenterState {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TIconDisc(VIcons.Lock, c.ink3, c.cream, size = 56.dp, glyph = 26.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Not your student", style = VTheme.type.bodyStrong.colored(c.navyDeep))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "You can only view students in the classes you teach.",
                        style = VTheme.type.caption.colored(c.ink3),
                    )
                }
            }
            state.error != null -> TeacherCenterState {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn't load profile", style = VTheme.type.bodyStrong.colored(c.navyDeep))
                    Spacer(Modifier.height(4.dp))
                    Text(state.error!!, style = VTheme.type.caption.colored(c.ink3))
                    Spacer(Modifier.height(14.dp))
                    VButton("Try again", onClick = { viewModel.retry() }, size = VButtonSize.Sm, tone = VButtonTone.Lavender)
                }
            }
            state.profile != null -> StudentProfileBody(state.profile!!)
            else -> TeacherCenterState { TeacherSpinner() }
        }
    }
}

@Composable
private fun StudentProfileBody(p: StudentProfileData) {
    val c = VTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { IdentityCard(p) }
        item { AttendanceProfileCard(p) }
        if (p.performance.isNotEmpty()) item { PerformanceCard(p.performance) }
        if (p.flags.isNotEmpty()) item { FlagsCard(p.flags) }
        p.parentContact?.let { pc ->
            if (!pc.name.isNullOrBlank() || !pc.phone.isNullOrBlank()) {
                item { ParentContactCard(pc.name, pc.phone) }
            }
        }
    }
}

@Composable
private fun IdentityCard(p: StudentProfileData) {
    val c = VTheme.colors
    TCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(60.dp).clip(CircleShape).background(c.lavenderLight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    p.name.take(1).uppercase(),
                    style = VTheme.type.h2.colored(c.accentDeep),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = VTheme.type.h3.colored(c.navyDeep))
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        append("${p.className} · ${p.section}")
                        p.roll?.let { append(" · Roll $it") }
                    },
                    style = VTheme.type.body.colored(c.ink2),
                )
            }
        }
    }
}

@Composable
private fun AttendanceProfileCard(p: StudentProfileData) {
    val c = VTheme.colors
    val a = p.attendance
    val pct = a.rate?.let { (it * 100).toInt() }
    TCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TEyebrow("ATTENDANCE")
                TrendPill(a.trend)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TRing(
                    percent = pct ?: 0,
                    accent = attendanceColor(c, pct),
                    modifier = Modifier.size(72.dp),
                    label = pct?.let { "$it%" } ?: "—",
                    labelSize = 16.sp,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (pct == null) "No attendance data yet" else "$pct% present overall",
                        style = VTheme.type.bodyStrong.colored(c.navyDeep),
                    )
                    if (a.recent.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Recent", style = VTheme.type.label.colored(c.ink3).copy(fontSize = 9.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            a.recent.take(8).forEach { day ->
                                Box(
                                    Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)).background(attendanceDayColor(c, day.status)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendPill(trend: String) {
    val c = VTheme.colors
    val (bg, fg, label) = when (trend.lowercase()) {
        "improving" -> Triple(c.success.copy(alpha = 0.16f), c.successInk, "IMPROVING")
        "declining" -> Triple(c.danger.copy(alpha = 0.12f), c.dangerInk, "DECLINING")
        "flat" -> Triple(c.cream, c.ink2, "STEADY")
        else -> return
    }
    TPill(label, bg, fg)
}

private fun attendanceColor(c: com.littlebridge.vidyaprayag.ui.v2.theme.VColors, pct: Int?): Color = when {
    pct == null -> c.ink3
    pct >= 85 -> c.success
    pct >= 70 -> c.warning
    else -> c.danger
}

private fun attendanceDayColor(c: com.littlebridge.vidyaprayag.ui.v2.theme.VColors, status: String): Color = when (status.lowercase()) {
    "present" -> c.success
    "late" -> c.warning
    "leave" -> c.accent
    "absent" -> c.danger
    else -> c.hairline
}

@Composable
private fun PerformanceCard(perf: List<StudentPerformanceDto>) {
    val c = VTheme.colors
    TCard {
        Column {
            TEyebrow("PERFORMANCE")
            Spacer(Modifier.height(10.dp))
            perf.forEachIndexed { i, e ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.assessmentName, style = VTheme.type.bodyStrong.colored(c.navyDeep))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            buildString {
                                append(e.subject)
                                if (!e.date.isNullOrBlank()) append(" · ${prettyDateShort(e.date)}")
                            },
                            style = VTheme.type.caption.colored(c.ink3),
                        )
                    }
                    when {
                        e.isAbsent -> TPill("ABSENT", c.danger.copy(alpha = 0.12f), c.dangerInk)
                        e.marks == null -> TPill("PENDING", c.cream, c.ink2)
                        else -> {
                            val ratio = if (e.max > 0) e.marks!! / e.max else 0.0
                            val tint = when {
                                ratio >= 0.6 -> c.successInk
                                ratio >= 0.4 -> c.warningInk
                                else -> c.dangerInk
                            }
                            Text(
                                "${fmt1(e.marks!!.toFloat())}/${e.max}",
                                style = VTheme.type.bodyStrong.colored(tint),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagsCard(flags: List<String>) {
    val c = VTheme.colors
    TCard {
        Column {
            TEyebrow("FLAGS", dot = c.warning)
            Spacer(Modifier.height(10.dp))
            flags.forEachIndexed { i, code ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                val (tint, bg, text) = flagMeta(c, code)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(VIcons.AlertTriangle, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                    Text(text, style = VTheme.type.body.colored(tint).copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

private fun flagMeta(c: com.littlebridge.vidyaprayag.ui.v2.theme.VColors, code: String): Triple<Color, Color, String> = when (code) {
    "low_attendance" -> Triple(c.dangerInk, c.danger.copy(alpha = 0.10f), "Low attendance")
    "recent_absences" -> Triple(c.dangerInk, c.danger.copy(alpha = 0.10f), "Recent absences")
    "failing_trend" -> Triple(c.dangerInk, c.danger.copy(alpha = 0.10f), "Failing trend")
    "dropping" -> Triple(c.warningInk, c.warning.copy(alpha = 0.14f), "Marks dropping")
    "no_data" -> Triple(c.ink2, c.cream, "Not enough data")
    else -> Triple(c.ink2, c.cream, code.replace('_', ' ').replaceFirstChar { it.uppercase() })
}

@Composable
private fun ParentContactCard(name: String?, phone: String?) {
    val c = VTheme.colors
    TCard {
        Column {
            TEyebrow("PARENT CONTACT")
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TIconDisc(VIcons.User, c.tealDeep, c.teal.copy(alpha = 0.14f), size = 40.dp, glyph = 18.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name ?: "Parent / Guardian", style = VTheme.type.bodyStrong.colored(c.navyDeep))
                    if (!phone.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(VIcons.Phone, contentDescription = null, tint = c.ink3, modifier = Modifier.size(13.dp))
                            Text(phone, style = VTheme.type.body.colored(c.ink2))
                        }
                    }
                }
            }
        }
    }
}
