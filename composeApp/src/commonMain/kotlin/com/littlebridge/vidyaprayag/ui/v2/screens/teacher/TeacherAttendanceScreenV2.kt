package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.AttendanceStatus
import com.littlebridge.vidyaprayag.feature.teacher.presentation.StudentAttendance
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherAttendanceState
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherAttendanceViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VDatePicker
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherAttendanceScreenV2 — T-205 (Doc 06 §3, Doc 10 §6.3). The clean rebuild of
 * the attendance plane, reached PRE-SCOPED from a Today/Classes CTA via [assignmentId]
 * (Doc 05 binding) — there is no shared class picker (kills F-ATT-1).
 *
 * Honors the spec end-to-end:
 *  - **Wrong-class guard header** (E15): a prominent "{class} · {subject} · {date}"
 *    band so Aanya always knows which class she's in.
 *  - **Enabled, correctable date** (fixes F-ATT-2): a real [VDatePicker]; the back-date
 *    window the server accepts is advertised; a blocked save returns a clear message.
 *  - **4-state pills** P/A/L/Lv (Doc 06 §3.4, D-ATT-1) at ≥48dp, color + LETTER encoded
 *    (never hue-only — Doc 10 §11), with semantic tokens (success/danger/warning/accentSoft).
 *  - **Leave defaults** (§3.5): approved-leave students arrive pre-set to leave and are
 *    badged; still overridable.
 *  - **Bulk "Mark all present"** (§3.6) preserving manual + leave exceptions.
 *  - **Running counter** (§3.6): "P · A · L · Lv · unmarked", always visible (sticky).
 *  - **Load-for-EDIT audit** (E3): "Last marked by … at …".
 *  - **Holiday warning** (E1) when the date is a published holiday.
 *  - **Result-driven save** (§3.7) via [VButton] loading/success; no auto-publish.
 *  - **Three states** via [VStateHost]; long rosters virtualized via [LazyColumn] (Doc 10 §7).
 *
 * [scopeHint] is the pre-known label from the launching period (so the guard header
 * shows instantly while loading); the server load is the source of truth and overrides it.
 */
@Composable
fun TeacherAttendanceScreenV2(
    assignmentId: String = "",
    date: String = "",
    scopeHint: String = "",
    modifier: Modifier = Modifier,
    viewModel: TeacherAttendanceViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    // Reached pre-scoped: load whenever the assignment changes.
    LaunchedEffect(assignmentId) {
        if (assignmentId.isNotBlank()) viewModel.load(assignmentId, date)
    }

    TeacherAttendanceContent(
        state = state,
        hasScope = assignmentId.isNotBlank(),
        scopeHint = scopeHint,
        onChangeDate = viewModel::changeDate,
        onMarkAllPresent = viewModel::markAllPresent,
        onSetStatus = viewModel::setStatus,
        onSave = viewModel::save,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
private fun TeacherAttendanceContent(
    state: TeacherAttendanceState,
    hasScope: Boolean,
    scopeHint: String,
    onChangeDate: (String) -> Unit,
    onMarkAllPresent: () -> Unit,
    onSetStatus: (String, String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val headerScope = state.scope.ifBlank { scopeHint }

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 12.dp)) {

        // ── Wrong-class guard header (E15) ──────────────────────────────────
        // Always visible, even while loading — Aanya's reassurance she's in the
        // right class. class · subject · date, in calm accent ink.
        if (headerScope.isNotBlank() || state.date.isNotBlank()) {
            GuardHeader(scope = headerScope, date = state.date)
            Spacer(Modifier.height(12.dp))
        }

        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = !hasScope || (state.students.isEmpty() && !state.isHoliday),
            emptyTitle = if (!hasScope) "Choose a class" else "No students enrolled",
            emptyBody = if (!hasScope) "Open attendance from a class on Today to start marking."
            else "No students are enrolled in this class yet.",
            emptyIcon = VIcons.Users,
            onRetry = onRetry,
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── Holiday warning (E1) ────────────────────────────────────
                if (state.isHoliday) {
                    WarnBanner(
                        text = "This is a holiday${state.holidayName?.let { " ($it)" } ?: ""}. " +
                            "Attendance isn't usually marked today — only mark if there's a special session.",
                    )
                }

                // ── Date (enabled, correctable — fixes F-ATT-2) + bulk ──────
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    VDatePicker(
                        value = state.date,
                        onValueChange = onChangeDate,
                        label = "Date",
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "You can correct attendance up to ${state.backDateWindowDays} days back.",
                    style = VTheme.type.caption.colored(c.ink3),
                )

                VButton(
                    text = "Mark all present",
                    onClick = onMarkAllPresent,
                    full = true,
                    variant = VButtonVariant.Secondary,
                    leading = { Icon(VIcons.Check, contentDescription = null, tint = c.successInk, modifier = Modifier.size(18.dp)) },
                )

                // ── Last-marked audit (E3) ──────────────────────────────────
                if (state.alreadyMarked && state.lastMarkedBy != null) {
                    Text(
                        "Last marked by ${state.lastMarkedBy}${state.lastMarkedAt?.let { " · ${prettyStamp(it)}" } ?: ""}",
                        style = VTheme.type.caption.colored(c.ink2),
                    )
                }

                // ── Virtualized roster (Doc 10 §7) ──────────────────────────
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.students, key = { it.studentId }) { s ->
                        StudentRow(s = s, onSetStatus = onSetStatus)
                    }
                    item { Spacer(Modifier.height(140.dp)) } // clear the sticky footer + dock
                }

                // ── Sticky running counter + result-driven Save (§3.6/§3.7) ──
                SaveFooter(state = state, onSave = onSave)

                state.saveError?.let { err ->
                    Text(err, style = VTheme.type.caption.colored(c.danger), modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun GuardHeader(scope: String, date: String) {
    val c = VTheme.colors
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.accentTint).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.Users, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    scope.ifBlank { "Attendance" },
                    style = VTheme.type.h3.colored(c.ink).copy(fontWeight = FontWeight.ExtraBold),
                )
                if (date.isNotBlank()) {
                    Text(prettyStamp(date), style = VTheme.type.caption.colored(c.accentDeep))
                }
            }
        }
    }
}

@Composable
private fun WarnBanner(text: String) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.warning.copy(alpha = 0.22f))
            .border(1.dp, c.warning, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(VIcons.AlertTriangle, contentDescription = null, tint = c.warningInk, modifier = Modifier.size(18.dp))
        Text(text, style = VTheme.type.caption.colored(c.warningInk).copy(fontSize = 12.sp))
    }
}

@Composable
private fun StudentRow(s: StudentAttendance, onSetStatus: (String, String) -> Unit) {
    val c = VTheme.colors
    VCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VAvatar(name = s.name, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    // bodyLarge equivalent: primary roster name at 16sp (Doc 10 §2, 58yo floor).
                    Text(s.name, style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 16.sp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Roll ${s.rollNo.ifBlank { "—" }}", style = VTheme.type.dataSm.colored(c.ink3).copy(fontSize = 12.sp))
                        if (s.isOnApprovedLeave) {
                            LeaveBadge()
                        }
                    }
                }
            }
            // The four status pills — ≥48dp tall, equal width, letter + color encoded.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill("P", "Present", s.status == AttendanceStatus.PRESENT, c.success, c.successInk, Modifier.weight(1f)) {
                    onSetStatus(s.studentId, AttendanceStatus.PRESENT)
                }
                StatusPill("A", "Absent", s.status == AttendanceStatus.ABSENT, c.danger, c.dangerInk, Modifier.weight(1f)) {
                    onSetStatus(s.studentId, AttendanceStatus.ABSENT)
                }
                StatusPill("L", "Late", s.status == AttendanceStatus.LATE, c.warning, c.warningInk, Modifier.weight(1f)) {
                    onSetStatus(s.studentId, AttendanceStatus.LATE)
                }
                StatusPill("Lv", "Leave", s.status == AttendanceStatus.LEAVE, c.accentSoft, c.accentDeep, Modifier.weight(1f)) {
                    onSetStatus(s.studentId, AttendanceStatus.LEAVE)
                }
            }
        }
    }
}

@Composable
private fun LeaveBadge() {
    val c = VTheme.colors
    Box(
        Modifier.clip(RoundedCornerShape(999.dp)).background(c.accentSoft.copy(alpha = 0.22f)).padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text("On approved leave", style = VTheme.type.label.colored(c.accentDeep).copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun StatusPill(
    letter: String,
    label: String,
    active: Boolean,
    fill: Color,
    ink: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .heightIn(min = 48.dp) // Doc 10 §3 — hard floor for Mr. Rao.
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) fill else c.cream)
            .border(1.dp, if (active) fill else c.border2, RoundedCornerShape(999.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = if (active) "$label, selected" else label }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter,
            style = VTheme.type.label.colored(if (active) ink else c.ink3)
                .copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
        )
    }
}

@Composable
private fun SaveFooter(state: TeacherAttendanceState, onSave: () -> Unit) {
    val c = VTheme.colors
    VCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                // Running counter — DM Mono tabular figures (Doc 10 §2).
                Text(
                    "${state.presentCount} P · ${state.absentCount} A · ${state.lateCount} L · ${state.leaveCount} Lv",
                    style = VTheme.type.data.colored(c.ink).copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                )
                val unmarked = state.unmarkedCount
                Text(
                    if (unmarked > 0) "$unmarked still on default" else "${state.total} students marked",
                    style = VTheme.type.caption.colored(if (unmarked > 0) c.ink3 else c.successInk),
                )
            }
            VButton(
                text = if (state.saveSuccess) "Saved" else "Save",
                onClick = onSave,
                size = VButtonSize.Lg,
                tone = VButtonTone.Lavender,
                loading = state.isSaving,
                success = state.saveSuccess,
                enabled = !state.isSaving && !state.saveSuccess && state.total > 0,
                successLabel = "Saved",
                modifier = Modifier.widthIn(min = 120.dp),
            )
        }
    }
}

/** Render an ISO date / timestamp into a short human label without pulling a date lib. */
private fun prettyStamp(raw: String): String {
    // Accept "YYYY-MM-DD" or "YYYY-MM-DDTHH:mm" / ISO instants; show date (+time if present).
    val datePart = raw.take(10)
    val parts = datePart.split('-')
    if (parts.size != 3) return raw
    val (y, m, d) = parts
    val month = MONTHS.getOrNull(m.toIntOrNull()?.minus(1) ?: -1) ?: m
    val day = d.toIntOrNull() ?: return raw
    val timePart = raw.drop(11).take(5).takeIf { it.length == 5 && it.contains(':') }
    val base = "$month $day, $y"
    return if (timePart != null) "$base at $timePart" else base
}

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
