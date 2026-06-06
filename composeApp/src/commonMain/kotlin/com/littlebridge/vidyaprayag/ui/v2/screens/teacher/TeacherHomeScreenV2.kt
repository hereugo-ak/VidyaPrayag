package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherHomeViewModel
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherPeriod
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherTask
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VEmptyState
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.screens.VPortalHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TeacherHomeScreenV2 — teacher "Home" tab, translated from Teacher.tsx → Home.
 *
 * Today's glance (classes / pending attendance / pending marks / homework due), the period
 * timeline, and the task list. Bound to the existing [TeacherHomeViewModel].
 */
@Composable
fun TeacherHomeScreenV2(
    modifier: Modifier = Modifier,
    viewModel: TeacherHomeViewModel = koinViewModel(),
    onOpenNotifications: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    val state by viewModel.state.collectAsStateV2()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.md, vertical = d.md),
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        VPortalHeader(
            name = state.teacherName,
            subtitle = state.schoolName.ifBlank { "Today" },
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
                    TeacherHeaderIcon(VIcons.Calendar, "Academic calendar", onClick = onOpenCalendar)
                    TeacherHeaderIcon(VIcons.Bell, "Notifications", onClick = onOpenNotifications)
                }
            },
        )

        // Stat glance
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.sm)) {
            StatCard("Classes", state.classesToday, Modifier.weight(1f))
            StatCard("Attendance", state.pendingAttendance, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.sm)) {
            StatCard("Marks due", state.pendingMarks, Modifier.weight(1f))
            StatCard("Homework", state.homeworkDue, Modifier.weight(1f))
        }

        VSectionHeader("TODAY'S PERIODS")
        if (state.periods.isEmpty() && !state.isLoading) {
            VEmptyState(title = "No periods today", icon = VIcons.Calendar, body = "Enjoy the lighter day.")
        } else {
            state.periods.forEach { PeriodRow(it) }
        }

        VSectionHeader("TASKS")
        if (state.tasks.isEmpty() && !state.isLoading) {
            VEmptyState(title = "All caught up", icon = VIcons.Check, body = "No pending tasks.")
        } else {
            state.tasks.forEach { TaskRow(it) }
        }

        if (state.error != null) {
            Text(state.error!!, style = VTheme.type.caption.colored(c.dangerInk))
        }
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun TeacherHeaderIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(c.cream)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = c.ink, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    VCard(modifier = modifier) {
        Text(value.toString(), style = VTheme.type.dataLg.colored(c.tealDeep))
        Text(label, style = VTheme.type.label.colored(c.ink3))
    }
}

@Composable
private fun PeriodRow(p: TeacherPeriod) {
    val c = VTheme.colors
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.md)) {
            VStatusDot(color = if (p.status == "done") c.successInk else c.teal)
            Column(Modifier.weight(1f)) {
                Text("${p.className} · ${p.subject}", style = VTheme.type.h4.colored(c.ink))
                Text("${p.time} · ${p.room}", style = VTheme.type.caption.colored(c.ink3))
            }
            VBadge(text = p.status.uppercase(), tone = if (p.status == "done") VBadgeTone.Success else VBadgeTone.Arctic)
        }
    }
}

@Composable
private fun TaskRow(t: TeacherTask) {
    val c = VTheme.colors
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.md)) {
            VStatusDot(color = if (t.isDone) c.successInk else c.warningInk)
            Column(Modifier.weight(1f)) {
                Text(t.title, style = VTheme.type.h4.colored(c.ink))
                Text("${t.subtitle} · ${t.className}", style = VTheme.type.caption.colored(c.ink3))
            }
            VBadge(text = t.type.uppercase(), tone = VBadgeTone.Neutral)
        }
    }
}
