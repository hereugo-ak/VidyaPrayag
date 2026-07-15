package com.littlebridge.enrollplus.ui.v2.screens.teacher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.core.locale.StringKeys
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsSignalDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsStudentDto
import com.littlebridge.enrollplus.feature.teacher.presentation.AttendanceStatus
import com.littlebridge.enrollplus.feature.teacher.presentation.StudentAttendance
import com.littlebridge.enrollplus.feature.pews.presentation.TeacherPewsViewModel
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherAttendanceViewModel
import com.littlebridge.enrollplus.ui.v2.components.VAvatar
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VDatePicker
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.tokens.VColors
import com.littlebridge.enrollplus.ui.tokens.VShapes
import com.littlebridge.enrollplus.ui.tokens.VTypography
import com.littlebridge.enrollplus.ui.v2.locale.appString
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import org.koin.compose.viewmodel.koinViewModel
import com.littlebridge.enrollplus.ui.v2.screens.teacher.TeacherSpinner

/**
 * TeacherAttendanceScreenV2 — the scoped attendance plane (Doc 06 §3). Reached PRE-SCOPED with a
 * pre-authorized [assignmentId] from the Update scope gate or a Home/Classes CTA. It loads the typed
 * roster, defaults the date to today (correctable), pre-sets approved-leave students to "leave"
 * (locked), supports the 4-state space (present · absent · late · leave), a bulk "mark all present",
 * a live running counter, and a result-driven Save that NEVER auto-publishes.
 */
@Composable
fun TeacherAttendanceScreenV2(
    assignmentId: String,
    scopeLabel: String,
    modifier: Modifier = Modifier,
    tool: UpdateTool = UpdateTool.Attendance,
    onToolChange: (UpdateTool) -> Unit = {},
    onChangeClass: () -> Unit = {},
    viewModel: TeacherAttendanceViewModel = koinViewModel(),
    pewsViewModel: TeacherPewsViewModel = koinViewModel(),
    onOpenMessages: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateV2()
    var showInsights by remember { mutableStateOf(false) }

    LaunchedEffect(assignmentId) {
        if (assignmentId.isNotBlank() && state.assignmentId != assignmentId) viewModel.load(assignmentId)
    }

    LaunchedEffect(showInsights) {
        if (showInsights) pewsViewModel.load()
    }

    Box(modifier.fillMaxSize().background(VColors.cream)) {
        when {
            state.isLoading && state.students.isEmpty() && !showInsights -> VtCenterState { TeacherSpinner() }
            state.error != null && state.students.isEmpty() && !showInsights -> VtErrorState(
                title = appString(StringKeys.TC_COULDNT_LOAD_ATTENDANCE),
                detail = state.error,
                retryLabel = appString(StringKeys.COMMON_BUTTON_RETRY),
                onRetry = { viewModel.retry() },
            )
            showInsights -> AttendanceInsightsBody(pewsViewModel, state.className, state.section, scopeLabel) { showInsights = false }
            else -> AttendanceBody(
                state.students,
                viewModel,
                scopeLabel,
                tool = tool,
                onToolChange = onToolChange,
                onChangeClass = onChangeClass,
                onShowInsights = { showInsights = true },
                onOpenMessages = onOpenMessages,
            )
        }
    }
}

@Composable
private fun AttendanceBody(
    students: List<StudentAttendance>,
    viewModel: TeacherAttendanceViewModel,
    scopeLabel: String,
    tool: UpdateTool,
    onToolChange: (UpdateTool) -> Unit,
    onChangeClass: () -> Unit,
    onShowInsights: () -> Unit,
    onOpenMessages: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateV2()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = TeacherDockClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Scrollable scoped chrome (rail + scope bar) ──
        item {
            ScopedToolHeader(
                tool = tool,
                scopeLabel = scopeLabel,
                onToolChange = onToolChange,
                onChangeClass = onChangeClass,
            )
        }

        // ── Compact date + metrics header ──
        item {
            VtCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            if (state.alreadyMarked && state.lastMarkedBy != null) {
                                Text(
                                    appString(
                                        StringKeys.TC_LAST_MARKED_BY,
                                        "name" to (state.lastMarkedBy ?: ""),
                                        "date" to (state.lastMarkedAt?.let { " · ${prettyDate(it.take(10))}" } ?: ""),
                                    ),
                                    style = VTypography.caption,
                                    color = VColors.ink3,
                                )
                            }
                        }
                        VDatePicker(
                            value = state.date,
                            onValueChange = { viewModel.changeDate(it) },
                            label = appString(StringKeys.SCH_DATE),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (state.isHoliday || state.isCancelled) {
                        Text(
                            if (state.isHoliday) appString(StringKeys.TC_HOLIDAY_NOTICE, "name" to (state.holidayName?.let { " — $it" } ?: "")) else appString(StringKeys.TC_CLASS_CANCELLED_DATE),
                            style = VTypography.caption,
                            color = VColors.gold,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VtCompactMetric(state.presentCount.toString(), appString(StringKeys.ATT_PRESENT), VColors.success, Modifier.weight(1f))
                        VtCompactMetric(state.absentCount.toString(), appString(StringKeys.ATT_ABSENT), VColors.coral, Modifier.weight(1f))
                        VtCompactMetric(state.lateCount.toString(), appString(StringKeys.ATT_LATE), VColors.gold, Modifier.weight(1f))
                        VtCompactMetric(state.leaveCount.toString(), appString(StringKeys.TEACHER_LEAVE), VColors.sky, Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VButton(
                            text = appString(StringKeys.TC_MARK_ALL_PRESENT),
                            onClick = { viewModel.markAllPresent() },
                            modifier = Modifier.weight(1f),
                            variant = VButtonVariant.Secondary,
                            tone = VButtonTone.Mint,
                            size = VButtonSize.Md,
                            leading = { Icon(VIcons.Check, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        )
                        VButton(
                            text = "Insights",
                            onClick = onShowInsights,
                            modifier = Modifier.weight(1f),
                            variant = VButtonVariant.Ghost,
                            size = VButtonSize.Md,
                            leading = { Icon(VIcons.Activity, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        )
                    }
                }
            }
        }

        // Bug 2+3: build a disambiguation map so duplicate display names get a suffix.
        val disambiguatedNames = remember(students) {
            val nameCounts = students.groupingBy { it.name }.eachCount()
            students.associate { s ->
                val base = s.name.takeIf { it.isNotBlank() && it.length > 1 } ?: "Student ${s.studentId.takeLast(4)}"
                val disambiguated = if (nameCounts[s.name] > 1 && base == s.name) {
                    val suffix = s.rollNo.takeIf { it.isNotBlank() } ?: s.studentId.takeLast(4)
                    "$base · #$suffix"
                } else base
                s.studentId to disambiguated
            }
        }
        items(students, key = { it.studentId }) { s ->
            AttendanceStudentRow(s, displayName = disambiguatedNames[s.studentId] ?: s.name, onSetStatus = { status -> viewModel.setStatus(s.studentId, status) })
        }

        // ── Save footer ──
        item {
            Spacer(Modifier.height(4.dp))
            if (state.saveError != null) {
                Text(state.saveError ?: "", style = VTypography.caption, color = VColors.coral)
                Spacer(Modifier.height(8.dp))
            }
            VButton(
                text = if (state.alreadyMarked) appString(StringKeys.TC_UPDATE_ATTENDANCE) else appString(StringKeys.TC_SAVE_ATTENDANCE),
                onClick = { viewModel.save() },
                full = true,
                tone = VButtonTone.Lavender,
                size = VButtonSize.Lg,
                loading = state.isSaving,
                success = state.saveSuccess,
                successLabel = appString(StringKeys.TC_SAVED),
                stateful = true,
                enabled = students.isNotEmpty() && !state.isHoliday,
            )
        }

        // ── Notify absent parents banner (post-save) ──
        if (state.saveSuccess && state.absentCount > 0) {
            item {
                AbsentNotifyBanner(
                    absentCount = state.absentCount,
                    onNotify = onOpenMessages,
                )
            }
        }
    }
}

@Composable
private fun AttendanceStudentRow(s: StudentAttendance, displayName: String, onSetStatus: (String) -> Unit) {
    val locked = s.isOnApprovedLeave
    val displayRoll = s.rollNo.takeIf { it.isNotBlank() } ?: "—"
    VtCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VAvatar(name = displayName, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(displayName, style = VTypography.caption, color = VColors.ink, maxLines = 1)
                    Text(
                        if (locked) appString(StringKeys.TC_ROLL_ON_LEAVE, "no" to displayRoll) else appString(StringKeys.TC_ROLL_NO, "no" to displayRoll),
                        style = VTypography.caption,
                        color = if (locked) VColors.sky else VColors.ink3,
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(appString(StringKeys.TC_P), AttendanceStatus.PRESENT, s.status, VColors.success, locked, onSetStatus, Modifier.weight(1f))
                StatusChip(appString(StringKeys.TC_A), AttendanceStatus.ABSENT, s.status, VColors.coral, locked, onSetStatus, Modifier.weight(1f))
                StatusChip(appString(StringKeys.ATT_LATE), AttendanceStatus.LATE, s.status, VColors.gold, locked, onSetStatus, Modifier.weight(1f))
                StatusChip(appString(StringKeys.TEACHER_LEAVE), AttendanceStatus.LEAVE, s.status, VColors.sky, locked, onSetStatus, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    status: String,
    current: String,
    tint: androidx.compose.ui.graphics.Color,
    locked: Boolean,
    onSet: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = current == status
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .fillMaxWidth()
            .clip(VShapes.md)
            .background(if (active) tint.copy(alpha = 0.16f) else VColors.creamDeep)
            .border(1.dp, if (active) tint.copy(alpha = 0.5f) else VColors.line, VShapes.md)
            .clickable(interactionSource = interaction, indication = null, enabled = !locked) { onSet(status) }
            .heightIn(min = 40.dp)
            .padding(vertical = 10.dp)
            .alpha(if (locked) 0.45f else 1f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = VTypography.caption,
            color = if (active) tint else VColors.ink2,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Absent Notify Banner — post-save CTA to message absent student parents.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AbsentNotifyBanner(
    absentCount: Int,
    onNotify: () -> Unit,
) {
    VtCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VtIconDisc(VIcons.Mail, tint = VColors.coral, bg = VColors.coral.copy(alpha = 0.12f), size = 36.dp, glyph = 18.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        appString(StringKeys.TC_N_STUDENTS_ABSENT, "count" to absentCount.toString()),
                        style = VTypography.caption,
                        color = VColors.ink,
                    )
                    Text(
                        appString(StringKeys.TC_NOTIFY_PARENTS_ABOUT_ABSENCE),
                        style = VTypography.caption,
                        color = VColors.ink3,
                    )
                }
            }
            VButton(
                text = appString(StringKeys.TC_NOTIFY_PARENTS),
                onClick = onNotify,
                full = true,
                variant = VButtonVariant.Secondary,
                tone = VButtonTone.Rose,
                size = VButtonSize.Sm,
                leading = { Icon(VIcons.Send, contentDescription = null, modifier = Modifier.size(14.dp)) },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ATTENDANCE INSIGHTS — PEWS at-risk students for this class
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AttendanceInsightsBody(
    viewModel: TeacherPewsViewModel,
    className: String,
    section: String,
    scopeLabel: String,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateV2()
    val filtered = remember(state.students, className, section) {
        state.students.filter { s ->
            className.isBlank() || s.className.equals(className, ignoreCase = true)
        }.filter { s ->
            section.isBlank() || s.section.equals(section, ignoreCase = true)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 14.dp, bottom = TeacherDockClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header with back button
        item {
            VtCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(34.dp).clip(androidx.compose.foundation.shape.CircleShape).background(VColors.creamDeep)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(VIcons.ArrowLeft, contentDescription = "Back", tint = VColors.ink2, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        VtEyebrow("Attendance Insights", dot = VColors.violet)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            scopeLabel.ifBlank { "$className-$section" },
                            style = VTypography.h3,
                            color = VColors.ink,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        if (state.isLoading && filtered.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { TeacherSpinner() } }
        } else if (state.error != null && filtered.isEmpty()) {
            item {
                VtCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Couldn't load insights", style = VTypography.h3.copy(color = VColors.ink))
                        Spacer(Modifier.height(8.dp))
                        VButton("Retry", onClick = { viewModel.load() }, tone = VButtonTone.Lavender, size = VButtonSize.Sm)
                    }
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                VtCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        VtIconDisc(VIcons.Activity, tint = VColors.violet, bg = VColors.violet.copy(alpha = 0.14f), size = 48.dp, glyph = 24.dp)
                        Spacer(Modifier.height(10.dp))
                        Text("No at-risk students", style = VTypography.h3.copy(color = VColors.ink))
                        Spacer(Modifier.height(4.dp))
                        Text("All students are on track for this class", style = VTypography.caption, color = VColors.ink3)
                        Spacer(Modifier.height(12.dp))
                        VButton("Mark Attendance", onClick = onBack, tone = VButtonTone.Lavender, size = VButtonSize.Sm)
                    }
                }
            }
        } else {
            item { AnalyticsAtRiskHeader(filtered.size) }
            items(filtered, key = { it.studentCode }) { student ->
                PewsStudentRow(student)
            }
        }
    }
}

@Composable
private fun AnalyticsAtRiskHeader(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VtIconDisc(VIcons.TrendingDown, tint = VColors.coral, bg = VColors.coral.copy(alpha = 0.12f), size = 32.dp, glyph = 16.dp)
        Text("At-Risk Students", style = VTypography.body, color = VColors.ink)
        VtPill("$count", bg = VColors.coral.copy(alpha = 0.12f), fg = VColors.coral)
    }
}

@Composable
private fun PewsStudentRow(student: PewsStudentDto) {
    VtCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VAvatar(name = student.name, size = 38.dp)
            Column(Modifier.weight(1f)) {
                Text(student.name, style = VTypography.caption, color = VColors.ink, maxLines = 1)
                val subtitle = buildString {
                    if (student.className.isNotBlank() && student.section.isNotBlank()) {
                        append("${student.className}-${student.section} · ")
                    }
                    append("risk ${student.riskLevel}")
                    student.attendancePct?.let { append(" · att $it%") }
                    student.marksPct?.let { append(" · marks $it%") }
                }
                Text(subtitle, style = VTypography.caption, color = VColors.ink3)
                if (student.signals.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        student.signals.take(2).forEach { signal ->
                            PewsSignalChip(signal)
                        }
                    }
                }
            }
            RiskLevelPill(student.riskLevel)
        }
    }
}

@Composable
private fun RiskLevelPill(level: String) {
    val (bg, fg) = when (level.lowercase()) {
        "high" -> VColors.coral.copy(alpha = 0.14f) to VColors.coral
        "medium" -> VColors.gold.copy(alpha = 0.14f) to VColors.gold
        else -> VColors.sky.copy(alpha = 0.14f) to VColors.sky
    }
    Box(
        Modifier.clip(VShapes.sm).background(bg).padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(level.replaceFirstChar { it.uppercase() }, style = VTypography.caption, color = fg)
    }
}

@Composable
private fun PewsSignalChip(signal: PewsSignalDto) {
    Box(
        Modifier.clip(VShapes.sm).background(VColors.creamDeep).padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(signal.label, style = VTypography.caption, color = VColors.ink2)
    }
}
