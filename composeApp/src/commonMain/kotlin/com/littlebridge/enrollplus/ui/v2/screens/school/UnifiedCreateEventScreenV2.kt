package com.littlebridge.enrollplus.ui.v2.screens.school

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.admin.presentation.UnifiedCreateEventViewModel
import com.littlebridge.enrollplus.feature.admin.presentation.UnifiedCreateEventState
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VDatePicker
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.components.VLabel
import com.littlebridge.enrollplus.ui.v2.components.VScheduleToggle
import com.littlebridge.enrollplus.ui.v2.components.ScheduleSelection
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import com.littlebridge.enrollplus.util.todayIso
import org.koin.compose.viewmodel.koinViewModel

/**
 * UnifiedCreateEventScreenV2 — the single unified 3-step event/announcement
 * creation screen. Replaces both the 7-step CreateEventScreenV2 wizard and
 * the ComposeAnnouncementDialog.
 *
 * All events are announcements under the hood. The "Post as announcement"
 * toggle controls visibility:
 *   ON  → normal announcement (parents see it + syncs to calendar)
 *   OFF → "Calendar Only" announcement (hidden from parent feed, syncs to calendar)
 *
 * Steps:
 *   1. What — type, title, description
 *   2. When — date, schedule toggle
 *   3. Who  — audience, post-as-announcement toggle, publish
 */
@Composable
fun UnifiedCreateEventScreenV2(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnifiedCreateEventViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val c = VTheme.colors

    LaunchedEffect(state.created) {
        if (state.created) {
            onCreated()
            viewModel.reset()
        }
    }

    Column(
        modifier.fillMaxSize().background(c.background).imePadding().navigationBarsPadding(),
    ) {
        VBackHeader(
            title = "Create Event",
            onBack = { if (state.step > 1) viewModel.back() else onBack() },
        )

        StepProgress(current = state.step, total = state.totalSteps)

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state.step) {
                1 -> StepWhat(state, viewModel)
                2 -> StepWhen(state, viewModel)
                3 -> StepWho(state, viewModel)
            }

            state.errorMessage?.let {
                Text(it, style = VTheme.type.caption.colored(c.dangerInk))
            }
        }

        UnifiedFooter(state, viewModel)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Progress + footer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepProgress(current: Int, total: Int) {
    val c = VTheme.colors
    Column(Modifier.fillMaxWidth().background(c.card).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            "Step $current of $total",
            style = VTheme.type.label.colored(c.ink3),
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..total).forEach { i ->
                Box(
                    Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (i <= current) c.tealDeep else c.hairline),
                )
            }
        }
    }
}

@Composable
private fun UnifiedFooter(
    state: UnifiedCreateEventState,
    viewModel: UnifiedCreateEventViewModel,
) {
    val c = VTheme.colors
    Row(
        Modifier.fillMaxWidth().background(c.card).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.step > 1) {
            VButton(
                text = "Back",
                onClick = viewModel::back,
                variant = VButtonVariant.Secondary,
                tone = VButtonTone.Navy,
                modifier = Modifier.weight(1f),
            )
        }
        if (state.step < state.totalSteps) {
            VButton(
                text = "Continue",
                onClick = viewModel::next,
                tone = VButtonTone.Teal,
                enabled = state.canGoNext,
                modifier = Modifier.weight(1f),
            )
        } else {
            VButton(
                text = if (state.form.isScheduled) "Schedule" else "Publish",
                onClick = viewModel::publish,
                tone = VButtonTone.Teal,
                loading = state.isSaving,
                enabled = state.canGoNext,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 — What (type + title + description)
// ─────────────────────────────────────────────────────────────────────────────

private data class TypeOption(val type: String, val title: String, val subtitle: String)

private val TYPE_OPTIONS = listOf(
    TypeOption("Holidays", "Holiday", "Days off & breaks"),
    TypeOption("PTM", "PTM", "Parent-teacher meetings"),
    TypeOption("Events", "School Event", "Functions, celebrations"),
    TypeOption("Update", "Update", "General notice / info"),
    TypeOption("Reminder", "Reminder", "Gentle nudge to parents"),
)

@Composable
private fun StepWhat(
    state: UnifiedCreateEventState,
    viewModel: UnifiedCreateEventViewModel,
) {
    val c = VTheme.colors
    VLabel("What kind of event?")
    TYPE_OPTIONS.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { opt ->
                val isSel = opt.type == state.form.type
                VCard(
                    modifier = Modifier.weight(1f),
                    background = if (isSel) c.teal.copy(alpha = 0.12f) else c.card,
                    onClick = { viewModel.setType(opt.type) },
                ) {
                    Text(opt.title, style = VTheme.type.bodyStrong.colored(if (isSel) c.tealDeep else c.ink))
                    Spacer(Modifier.height(2.dp))
                    Text(opt.subtitle, style = VTheme.type.caption.colored(c.ink3))
                }
            }
            if (row.size == 1) Box(Modifier.weight(1f)) {}
        }
    }
    VInput(
        value = state.form.title,
        onValueChange = viewModel::setTitle,
        label = "Title",
        placeholder = "e.g. Annual Sports Day",
        leadingIcon = VIcons.Megaphone,
    )
    VInput(
        value = state.form.description,
        onValueChange = viewModel::setDescription,
        label = "Description",
        placeholder = "Add details for parents and staff",
        singleLine = false,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 2 — When (date + schedule)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepWhen(
    state: UnifiedCreateEventState,
    viewModel: UnifiedCreateEventViewModel,
) {
    VLabel("When is it?")
    VDatePicker(
        value = state.form.date,
        onValueChange = viewModel::setDate,
        label = "Event date",
        placeholder = "Select date",
    )

    var scheduleSelection by remember {
        mutableStateOf(
            ScheduleSelection(
                isScheduled = state.form.isScheduled,
                dateIso = state.form.scheduleDate.ifBlank { todayIso() },
                hour = state.form.scheduleHour,
                minute = state.form.scheduleMinute,
            )
        )
    }

    VScheduleToggle(
        selection = scheduleSelection,
        onSelectionChange = {
            scheduleSelection = it
            viewModel.setScheduled(it.isScheduled)
            viewModel.setScheduleDate(it.dateIso)
            viewModel.setScheduleHour(it.hour)
            viewModel.setScheduleMinute(it.minute)
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 3 — Who (audience + post-as-announcement toggle + publish)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepWho(
    state: UnifiedCreateEventState,
    viewModel: UnifiedCreateEventViewModel,
) {
    val c = VTheme.colors
    val audienceOptions = listOf(
        "Everyone" to "ALL_SCHOOL",
        "Class" to "CLASS",
        "Subject" to "SUBJECT",
        "Students" to "STUDENT",
    )

    VLabel("Send to")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        audienceOptions.forEach { (label, value) ->
            FilterChip(label, state.form.audienceType == value) {
                viewModel.setAudienceType(value)
            }
        }
    }

    val needsTargets = state.form.audienceType != "ALL_SCHOOL"
    if (needsTargets) {
        val targetsHint = when (state.form.audienceType) {
            "CLASS" -> "e.g. Grade 4-A, Grade 5-B"
            "SUBJECT" -> "e.g. Mathematics, Science"
            "STUDENT" -> "e.g. DEMO-S001, S-2024-017"
            else -> ""
        }
        VInput(
            value = state.form.audienceTargets,
            onValueChange = viewModel::setAudienceTargets,
            label = "Targets (comma-separated)",
            placeholder = targetsHint,
            leadingIcon = VIcons.ListChecks,
        )
    }

    Spacer(Modifier.height(8.dp))

    // Post as announcement toggle
    VLabel("Announcement visibility")
    TogglePill(
        label = "Post as announcement",
        subtitle = if (state.form.postAsAnnouncement)
            "Parents will see this in their announcement feed"
        else
            "Calendar only — parents won't see this in announcements",
        checked = state.form.postAsAnnouncement,
        onToggle = { viewModel.setPostAsAnnouncement(it) },
    )

    if (state.calendarEligible && state.form.postAsAnnouncement) {
        Spacer(Modifier.height(4.dp))
        Text(
            "This event will also sync to the Academic Calendar automatically.",
            style = VTheme.type.caption.colored(c.tealDeep),
        )
    }
    if (state.calendarEligible && !state.form.postAsAnnouncement) {
        Spacer(Modifier.height(4.dp))
        Text(
            "This event will appear in the Academic Calendar with a \"Calendar Only\" badge.",
            style = VTheme.type.caption.colored(c.ink3),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared UI primitives
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    val c = VTheme.colors
    val (bg, fg) = if (active) c.teal.copy(alpha = 0.16f) to c.tealDeep else c.cream to c.ink2
    Text(
        text = label,
        style = VTheme.type.caption.colored(fg),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun TogglePill(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val c = VTheme.colors
    VCard(onClick = { onToggle(!checked) }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = VTheme.type.bodyStrong.colored(c.ink), modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(width = 44.dp, height = 26.dp).clip(RoundedCornerShape(999.dp))
                        .background(if (checked) c.tealDeep else c.hairline)
                        .clickable { onToggle(!checked) }
                        .padding(3.dp),
                    contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(Color.White))
                }
            }
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = VTheme.type.caption.colored(c.ink3))
            }
        }
    }
}
