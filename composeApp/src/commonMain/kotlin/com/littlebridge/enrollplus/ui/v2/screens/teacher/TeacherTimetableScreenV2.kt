package com.littlebridge.enrollplus.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableChangeRequestDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.ResolvedDayDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.ResolvedPeriodDto
import com.littlebridge.enrollplus.feature.teacher.presentation.TeacherTimetableViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VEmptyState
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.components.VTopTabs
import com.littlebridge.enrollplus.ui.v2.screens.VSectionHeader
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

private val WEEKDAY_LABELS = mapOf(
    1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun",
)

private val TIMETABLE_TABS = listOf("This Week", "Change Requests")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherTimetableScreenV2(
    modifier: Modifier = Modifier,
    viewModel: TeacherTimetableViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    var activeTab by remember { mutableStateOf("This Week") }
    var selectedDay by remember { mutableStateOf(1) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var requestKind by remember { mutableStateOf("NEW_PERIOD") }
    var requestPeriod by remember { mutableStateOf<ResolvedPeriodDto?>(null) }

    Column(modifier.fillMaxSize().statusBarsPadding()) {
        VTopTabs(
            tabs = TIMETABLE_TABS,
            selected = activeTab,
            onSelect = { activeTab = it },
        )

        when (activeTab) {
            "This Week" -> {
                // Day selector
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (1..6).forEach { day ->
                        VBadge(
                            text = WEEKDAY_LABELS[day] ?: "",
                            tone = if (selectedDay == day) VBadgeTone.Arctic else VBadgeTone.Neutral,
                            modifier = Modifier.clickable { selectedDay = day },
                        )
                    }
                }

                val dayData = state.week.find { it.weekday == selectedDay }
                val periods = dayData?.periods ?: emptyList()

                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (periods.isEmpty()) {
                        VEmptyState(
                            title = "No periods for ${WEEKDAY_LABELS[selectedDay]}",
                            icon = VIcons.Calendar,
                            body = "Your weekly schedule will appear here.",
                        )
                    } else {
                        periods.forEach { period ->
                            TeacherPeriodCard(
                                period = period,
                                onEdit = {
                                    requestKind = "UPDATE_PERIOD"
                                    requestPeriod = period
                                    showRequestDialog = true
                                },
                                onDelete = {
                                    requestKind = "DELETE_PERIOD"
                                    requestPeriod = period
                                    showRequestDialog = true
                                },
                            )
                        }
                    }

                    VButton(
                        text = "+ Request New Period",
                        onClick = {
                            requestKind = "NEW_PERIOD"
                            requestPeriod = null
                            showRequestDialog = true
                        },
                        full = true,
                        variant = VButtonVariant.Primary,
                        tone = VButtonTone.Teal,
                    )

                    state.infoMessage?.let {
                        Text(it, style = VTheme.type.caption.colored(VTheme.colors.successInk))
                    }
                    state.errorMessage?.let {
                        Text(it, style = VTheme.type.caption.colored(VTheme.colors.dangerInk))
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }

            "Change Requests" -> {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.changeRequests.isEmpty()) {
                        VEmptyState(
                            title = "No change requests",
                            icon = VIcons.Calendar,
                            body = "Your timetable change requests will appear here.",
                        )
                    } else {
                        state.changeRequests.forEach { req ->
                            ChangeRequestItemCard(req)
                        }
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }

    if (showRequestDialog) {
        ChangeRequestDialog(
            state = state,
            kind = requestKind,
            period = requestPeriod,
            isSaving = state.isSaving,
            onSubmit = { assignmentId, periodId, weekday, startTime, endTime, room, reason ->
                viewModel.submitChangeRequest(
                    kind = requestKind,
                    assignmentId = assignmentId,
                    periodId = periodId,
                    weekday = weekday,
                    startTime = startTime,
                    endTime = endTime,
                    room = room,
                    reason = reason,
                )
                showRequestDialog = false
            },
            onDismiss = { showRequestDialog = false },
        )
    }
}

@Composable
private fun TeacherPeriodCard(
    period: ResolvedPeriodDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = VTheme.colors
    VCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Time block
            Column(
                Modifier.width(64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(period.startTime, style = VTheme.type.bodyStrong.colored(c.ink))
                Text("↓", style = VTheme.type.caption.colored(c.ink3))
                Text(period.endTime, style = VTheme.type.body.colored(c.ink2))
            }

            Box(Modifier.width(1.dp).height(40.dp).background(c.hairline))

            // Content
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VBadge(text = period.className, tone = VBadgeTone.Arctic)
                    if (period.section.isNotBlank()) VBadge(text = period.section, tone = VBadgeTone.Accent)
                }
                Spacer(Modifier.height(2.dp))
                Text(period.subject, style = VTheme.type.bodyStrong.colored(c.ink))
                if (period.room.isNotBlank()) {
                    Text("Room ${period.room}", style = VTheme.type.caption.colored(c.ink3))
                }
            }

            // Edit + Delete buttons
            Box(
                Modifier.size(28.dp).clip(CircleShape)
                    .background(c.teal.copy(alpha = 0.1f))
                    .clickable { onEdit() },
                contentAlignment = Alignment.Center,
            ) {
                Text("✎", color = c.tealDeep, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier.size(28.dp).clip(CircleShape)
                    .background(c.dangerInk.copy(alpha = 0.1f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = c.dangerInk, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChangeRequestItemCard(req: TimetableChangeRequestDto) {
    val c = VTheme.colors
    VCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${req.className} · ${req.subject}",
                        style = VTheme.type.bodyStrong.colored(c.ink),
                    )
                    Text(
                        "${WEEKDAY_LABELS[req.weekday] ?: ""} ${req.startTime ?: ""}–${req.endTime ?: ""}",
                        style = VTheme.type.caption.colored(c.ink2),
                    )
                }
                VBadge(
                    text = req.kind.replace("_", " "),
                    tone = VBadgeTone.Accent,
                )
                VBadge(
                    text = req.status,
                    tone = when (req.status) {
                        "PENDING" -> VBadgeTone.Warning
                        "APPROVED" -> VBadgeTone.Success
                        "REJECTED" -> VBadgeTone.Neutral
                        else -> VBadgeTone.Neutral
                    },
                )
            }

            if (req.reason.isNotBlank()) {
                Text("Reason: ${req.reason}", style = VTheme.type.caption.colored(c.ink3))
            }
            if (req.adminNote.isNotBlank()) {
                Text("Admin note: ${req.adminNote}", style = VTheme.type.caption.colored(c.ink3))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChangeRequestDialog(
    state: com.littlebridge.enrollplus.feature.teacher.presentation.TeacherTimetableState,
    kind: String,
    period: ResolvedPeriodDto?,
    isSaving: Boolean,
    onSubmit: (assignmentId: String?, periodId: String?, weekday: Int, startTime: String, endTime: String, room: String, reason: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val assignments = state.assignments
    var selectedAssignmentId by remember { mutableStateOf(period?.assignmentId ?: assignments.firstOrNull()?.assignmentId ?: "") }
    var selectedWeekday by remember { mutableStateOf(1) }
    var startTime by remember { mutableStateOf(period?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(period?.endTime ?: "10:00") }
    var room by remember { mutableStateOf(period?.room ?: "") }
    var reason by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        VCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()).imePadding(),
            ) {
                Text(
                    when (kind) {
                        "NEW_PERIOD" -> "Request New Period"
                        "UPDATE_PERIOD" -> "Request Period Update"
                        "DELETE_PERIOD" -> "Request Period Deletion"
                        else -> "Change Request"
                    },
                    style = VTheme.type.h3, fontWeight = FontWeight.Bold, color = VTheme.colors.ink,
                )
                Text(
                    "This will be sent to your school admin for approval.",
                    style = VTheme.type.caption.colored(VTheme.colors.ink2),
                )

                if (kind == "NEW_PERIOD") {
                    // Assignment selector
                    Text("Class / Subject", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                    if (assignments.isEmpty()) {
                        Text("No assignments found.", style = VTheme.type.caption.colored(VTheme.colors.ink3))
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            assignments.forEach { asg ->
                                VBadge(
                                    text = "${asg.className}-${asg.section} ${asg.subject}",
                                    tone = if (selectedAssignmentId == asg.assignmentId) VBadgeTone.Arctic else VBadgeTone.Neutral,
                                    modifier = Modifier.clickable { selectedAssignmentId = asg.assignmentId },
                                )
                            }
                        }
                    }
                } else if (period != null) {
                    // Show period info for UPDATE/DELETE
                    VCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${period.className}-${period.section} ${period.subject}", style = VTheme.type.bodyStrong.colored(VTheme.colors.ink))
                            Text("${period.startTime}–${period.endTime} · Room ${period.room}", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                        }
                    }
                }

                // Weekday selector
                if (kind != "DELETE_PERIOD") {
                    Text("Day", style = VTheme.type.caption.colored(VTheme.colors.ink2))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..6).forEach { day ->
                            VBadge(
                                text = WEEKDAY_LABELS[day] ?: "",
                                tone = if (selectedWeekday == day) VBadgeTone.Arctic else VBadgeTone.Neutral,
                                modifier = Modifier.clickable { selectedWeekday = day },
                            )
                        }
                    }

                    // Time inputs
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            VInput(value = startTime, onValueChange = { startTime = it }, label = "Start", hint = "HH:mm", placeholder = "09:00")
                        }
                        Box(Modifier.weight(1f)) {
                            VInput(value = endTime, onValueChange = { endTime = it }, label = "End", hint = "HH:mm", placeholder = "10:00")
                        }
                    }
                    VInput(value = room, onValueChange = { room = it }, label = "Room", hint = "e.g. 101", placeholder = "101")
                }

                // Reason (always required)
                VInput(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "Reason",
                    hint = "Why is this change needed?",
                    placeholder = "e.g. Room conflict, schedule swap...",
                )

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(text = "Cancel", onClick = onDismiss, variant = VButtonVariant.Ghost)
                    VButton(
                        text = "Submit Request",
                        onClick = {
                            val r = reason.trim().ifBlank { return@VButton }
                            val aid = selectedAssignmentId.ifBlank { null }
                            val pid = period?.periodId
                            if (kind != "DELETE_PERIOD") {
                                onSubmit(aid, pid, selectedWeekday, startTime.trim(), endTime.trim(), room.trim(), r)
                            } else {
                                onSubmit(aid, pid, selectedWeekday, "", "", "", r)
                            }
                        },
                        variant = VButtonVariant.Primary,
                        tone = VButtonTone.Teal,
                        loading = isSaving,
                    )
                }
            }
        }
    }
}
