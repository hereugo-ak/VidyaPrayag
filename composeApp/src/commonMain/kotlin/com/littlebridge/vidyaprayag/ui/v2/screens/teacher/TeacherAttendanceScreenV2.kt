package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VDatePicker
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherAttendanceScreenV2 — T-205 (Doc 06 §3, Doc 10 §6.3). REDESIGNED (2026-06) around the
 * real-world flow: **most students are present, so you tap only the exceptions.**
 *
 * The old screen made every student a tall card with four stacked full-width pills, so a
 * 40-student class meant ~40 long cards and a sea of scrolling — slow, hard to scan, easy to
 * lose your place. This rebuild keeps the same ViewModel/contract but reorganises the UX:
 *
 *  - **Present-first**: a prominent hero "Mark everyone present" sets the floor in one tap, then
 *    the teacher only touches the few absent/late/leave students. A running "X unmarked" nudge
 *    makes the remaining work explicit.
 *  - **Compact, scannable rows**: each student is a slim row (avatar + name + roll on the left) with
 *    a single inline **segmented P · A · L · Lv selector** on the right — color + LETTER encoded
 *    (never hue-only, Doc 10 §11), ≥44dp targets (Doc 10 §3). No more giant per-student cards.
 *  - **Sticky live summary**: a single always-visible bar shows P/A/L/Lv counts as tappable FILTER
 *    chips, so the teacher can jump straight to "unmarked" or "absent" in a big class.
 *  - **Search** (client-side, name or roll) for fast lookup in large rosters.
 *  - **Wrong-class guard header** (E15), **enabled correctable date** (fixes F-ATT-2),
 *    **approved-leave defaults + badge** (§3.5), **load-for-EDIT audit** (E3), **holiday warning**
 *    (E1), and a **result-driven sticky Save** (§3.7) are all preserved.
 *  - **Three states** via [VStateHost]; long rosters virtualized via [LazyColumn] (Doc 10 §7).
 *
 * Reached PRE-SCOPED from a Today/Classes CTA via [assignmentId] (Doc 05 binding) — there is no
 * shared class picker (kills F-ATT-1). [scopeHint] shows the guard header instantly while loading;
 * the server load is the source of truth and overrides it.
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

/** Roster filter chips on the summary bar. */
private enum class RosterFilter { ALL, UNMARKED, PRESENT, ABSENT, LATE, LEAVE }

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

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(RosterFilter.ALL) }

    Box(modifier.fillMaxSize().background(c.background)) {
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
            // Apply the search + filter to derive the visible roster (declarative; never mutates VM).
            val visible = remember(state.students, query, filter) {
                state.students.filter { s ->
                    val q = query.trim().lowercase()
                    val matchQ = q.isEmpty() ||
                        s.name.lowercase().contains(q) ||
                        s.rollNo.lowercase().contains(q)
                    val matchF = when (filter) {
                        RosterFilter.ALL -> true
                        RosterFilter.UNMARKED -> s.status == AttendanceStatus.PRESENT && s.source == null
                        RosterFilter.PRESENT -> s.status == AttendanceStatus.PRESENT
                        RosterFilter.ABSENT -> s.status == AttendanceStatus.ABSENT
                        RosterFilter.LATE -> s.status == AttendanceStatus.LATE
                        RosterFilter.LEAVE -> s.status == AttendanceStatus.LEAVE
                    }
                    matchQ && matchF
                }
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp, top = 12.dp, bottom = 150.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // ── Wrong-class guard header (E15) ──────────────────────────
                item(key = "guard") {
                    GuardHeader(scope = headerScope, date = state.date)
                }

                // ── Holiday warning (E1) ────────────────────────────────────
                if (state.isHoliday) {
                    item(key = "holiday") {
                        WarnBanner(
                            text = "This is a holiday${state.holidayName?.let { " ($it)" } ?: ""}. " +
                                "Attendance isn't usually marked today — only mark if there's a special session.",
                        )
                    }
                }

                // ── Date (enabled, correctable — fixes F-ATT-2) ─────────────
                item(key = "date") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        VDatePicker(
                            value = state.date,
                            onValueChange = onChangeDate,
                            label = "Date",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "You can correct attendance up to ${state.backDateWindowDays} days back.",
                            style = VTheme.type.caption.colored(c.ink3),
                        )
                    }
                }

                // ── Last-marked audit (E3) ──────────────────────────────────
                if (state.alreadyMarked && state.lastMarkedBy != null) {
                    item(key = "audit") {
                        AuditPill(
                            text = "Last marked by ${state.lastMarkedBy}" +
                                (state.lastMarkedAt?.let { " · ${prettyStamp(it)}" } ?: ""),
                        )
                    }
                }

                // ── Present-first hero + live summary / filter bar ──────────
                item(key = "summary") {
                    SummaryBoard(
                        state = state,
                        filter = filter,
                        onFilter = { filter = if (filter == it) RosterFilter.ALL else it },
                        onMarkAllPresent = onMarkAllPresent,
                    )
                }

                // ── Search (client-side; appears for any non-trivial roster) ─
                if (state.students.size > 6) {
                    item(key = "search") {
                        VInput(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = "Search name or roll number",
                            leadingIcon = VIcons.Search,
                            modifier = Modifier.fillMaxWidth(),
                            trailing = if (query.isNotEmpty()) {
                                {
                                    val ix = remember { MutableInteractionSource() }
                                    Icon(
                                        VIcons.Close,
                                        contentDescription = "Clear search",
                                        tint = c.ink3,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable(interactionSource = ix, indication = null) { query = "" },
                                    )
                                }
                            } else null,
                        )
                    }
                }

                // ── Roster (compact rows, virtualized) ──────────────────────
                if (visible.isEmpty()) {
                    item(key = "no-match") { NoMatchRow(filter = filter, query = query) }
                } else {
                    items(visible, key = { it.studentId }) { s ->
                        StudentRow(s = s, onSetStatus = onSetStatus)
                    }
                }
            }
        }

        // ── Sticky running counter + result-driven Save (§3.6/§3.7) ──────────
        // Floats over the list so it's always reachable; the list's bottom contentPadding
        // (150dp) clears it.
        SaveBar(
            state = state,
            onSave = onSave,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Wrong-class guard header (E15)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GuardHeader(scope: String, date: String) {
    val c = VTheme.colors
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.accentTint)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.ListChecks, contentDescription = null, tint = c.accentDeep, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    scope.ifBlank { "Attendance" },
                    style = VTheme.type.h3.colored(c.ink).copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.warning.copy(alpha = 0.22f))
            .border(1.dp, c.warning, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(VIcons.AlertTriangle, contentDescription = null, tint = c.warningInk, modifier = Modifier.size(18.dp))
        Text(text, style = VTheme.type.caption.colored(c.warningInk).copy(fontSize = 12.sp))
    }
}

@Composable
private fun AuditPill(text: String) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.cream)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(VIcons.Clock, contentDescription = null, tint = c.ink3, modifier = Modifier.size(14.dp))
        Text(text, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Present-first hero + live summary / filter board
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SummaryBoard(
    state: TeacherAttendanceState,
    filter: RosterFilter,
    onFilter: (RosterFilter) -> Unit,
    onMarkAllPresent: () -> Unit,
) {
    val c = VTheme.colors
    val unmarked = state.unmarkedCount
    VCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // The present-first hero CTA. The label flips to a calm "All present" confirmation
            // once the floor is set, so the teacher knows the bulk action landed.
            VButton(
                text = if (unmarked == 0 && state.total > 0) "Everyone marked present" else "Mark everyone present",
                onClick = onMarkAllPresent,
                full = true,
                size = VButtonSize.Lg,
                tone = VButtonTone.Lavender,
                leading = {
                    Icon(VIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                },
            )
            Text(
                if (unmarked > 0)
                    "$unmarked still unmarked — tap below, then mark only the exceptions."
                else "Tap a student below to change their status.",
                style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp),
            )

            // Live, tappable status breakdown. Each chip filters the roster (tap again to clear).
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryChip("All", state.total, c.accentDeep, c.accentTint, filter == RosterFilter.ALL, Modifier.weight(1f)) { onFilter(RosterFilter.ALL) }
                SummaryChip("P", state.presentCount, c.successInk, c.success.copy(alpha = 0.5f), filter == RosterFilter.PRESENT, Modifier.weight(1f)) { onFilter(RosterFilter.PRESENT) }
                SummaryChip("A", state.absentCount, c.dangerInk, c.danger.copy(alpha = 0.45f), filter == RosterFilter.ABSENT, Modifier.weight(1f)) { onFilter(RosterFilter.ABSENT) }
                SummaryChip("L", state.lateCount, c.warningInk, c.warning.copy(alpha = 0.5f), filter == RosterFilter.LATE, Modifier.weight(1f)) { onFilter(RosterFilter.LATE) }
                SummaryChip("Lv", state.leaveCount, c.accentDeep, c.accentSoft.copy(alpha = 0.4f), filter == RosterFilter.LEAVE, Modifier.weight(1f)) { onFilter(RosterFilter.LEAVE) }
            }
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    count: Int,
    ink: Color,
    tint: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    val ix = remember { MutableInteractionSource() }
    Column(
        modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) tint else c.cream)
            .border(if (active) 1.5.dp else 1.dp, if (active) ink.copy(alpha = 0.5f) else c.border2, RoundedCornerShape(12.dp))
            .clickable(interactionSource = ix, indication = null, onClick = onClick)
            .semantics { contentDescription = "$label $count" + if (active) ", filter active" else "" }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(count.toString(), style = VTheme.type.data.colored(ink).copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
        Text(label, style = VTheme.type.label.colored(ink.copy(alpha = 0.85f)).copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun NoMatchRow(filter: RosterFilter, query: String) {
    val c = VTheme.colors
    val msg = when {
        query.isNotBlank() -> "No students match \"$query\"."
        filter == RosterFilter.UNMARKED -> "Everyone has been marked."
        filter == RosterFilter.ABSENT -> "No absent students."
        filter == RosterFilter.LATE -> "No students marked late."
        filter == RosterFilter.LEAVE -> "No students on leave."
        filter == RosterFilter.PRESENT -> "No students marked present yet."
        else -> "No students to show."
    }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.cream).padding(vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(msg, style = VTheme.type.body.colored(c.ink3))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Compact student row with an inline segmented P · A · L · Lv selector
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StudentRow(s: StudentAttendance, onSetStatus: (String, String) -> Unit) {
    val c = VTheme.colors
    // A thin status-colored strip down the left edge, matching the current status, so a quick
    // downward glance reads the whole class at once (Doc 10 §11 — color reinforces, the segmented
    // selector below carries the explicit letter cue so it's never hue-only).
    val statusInk = when (s.status) {
        AttendanceStatus.ABSENT -> c.dangerInk
        AttendanceStatus.LATE -> c.warningInk
        AttendanceStatus.LEAVE -> c.accentDeep
        else -> c.successInk
    }
    VCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(statusInk))
            Column(Modifier.weight(1f).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Small avatar for a human touch; the roll is what teachers actually call out.
                    VAvatar(name = s.name, size = 38.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.name,
                            style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 15.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Roll ${s.rollNo.ifBlank { "—" }}", style = VTheme.type.dataSm.colored(c.ink3).copy(fontSize = 12.sp))
                            if (s.isOnApprovedLeave) LeaveBadge()
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Inline segmented selector — one connected control, 4 equal segments, ≥44dp tall,
                // letter + color encoded. The active segment fills with its semantic color.
                SegmentedStatus(
                    status = s.status,
                    onSet = { onSetStatus(s.studentId, it) },
                )
            }
        }
    }
}

@Composable
private fun SegmentedStatus(status: String, onSet: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val c = VTheme.colors
        Segment("P", "Present", status == AttendanceStatus.PRESENT, c.success, c.successInk, Modifier.weight(1f)) { onSet(AttendanceStatus.PRESENT) }
        Segment("A", "Absent", status == AttendanceStatus.ABSENT, c.danger, c.dangerInk, Modifier.weight(1f)) { onSet(AttendanceStatus.ABSENT) }
        Segment("L", "Late", status == AttendanceStatus.LATE, c.warning, c.warningInk, Modifier.weight(1f)) { onSet(AttendanceStatus.LATE) }
        Segment("Lv", "Leave", status == AttendanceStatus.LEAVE, c.accentSoft, c.accentDeep, Modifier.weight(1f)) { onSet(AttendanceStatus.LEAVE) }
    }
}

@Composable
private fun Segment(
    letter: String,
    label: String,
    active: Boolean,
    fill: Color,
    ink: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    val ix = remember { MutableInteractionSource() }
    val bg by animateColorAsState(if (active) fill else c.cream, label = "segBg")
    val borderW by animateDpAsState(if (active) 1.5.dp else 1.dp, label = "segBorder")
    Row(
        modifier
            .heightIn(min = 46.dp) // ≥44dp accessible target (Doc 10 §3).
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(borderW, if (active) fill else c.border2, RoundedCornerShape(12.dp))
            .clickable(interactionSource = ix, indication = null, onClick = onClick)
            .semantics { contentDescription = if (active) "$label, selected" else "Set $label" }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            letter,
            style = VTheme.type.label.colored(if (active) ink else c.ink3)
                .copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
        )
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

// ─────────────────────────────────────────────────────────────────────────────
// Sticky save bar — floats over the list, always reachable (§3.6 / §3.7)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SaveBar(
    state: TeacherAttendanceState,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val unmarked = state.unmarkedCount
    Column(
        modifier
            .fillMaxWidth()
            .background(c.card)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        state.saveError?.let { err ->
            Text(
                err,
                style = VTheme.type.caption.colored(c.danger),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${state.presentCount} P · ${state.absentCount} A · ${state.lateCount} L · ${state.leaveCount} Lv",
                    style = VTheme.type.data.colored(c.ink).copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
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
