/*
 * File: AdminEventRegistrationScreenV2.kt
 * Module: ui.v2.screens.school
 *
 * Admin Event Registration management screen — registration list, slot management,
 * config, and event cancellation. Wired to AdminEventRegistrationViewModel.
 */
package com.littlebridge.enrollplus.ui.v2.screens.school

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.event.domain.model.AdminEventDto
import com.littlebridge.enrollplus.feature.event.domain.model.AdminRegistrationDto
import com.littlebridge.enrollplus.feature.event.domain.model.AutoGenerateSlotsRequest
import com.littlebridge.enrollplus.feature.event.domain.model.UpdateRegistrationConfigRequest
import com.littlebridge.enrollplus.feature.event.presentation.AdminEventRegistrationState
import com.littlebridge.enrollplus.feature.event.presentation.AdminEventRegistrationViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminEventRegistrationScreenV2(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminEventRegistrationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val c = VTheme.colors
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    var showAllRegistrations by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf("") }
    var slotStart by remember { mutableStateOf("") }
    var slotEnd by remember { mutableStateOf("") }
    var slotCapacity by remember { mutableStateOf("1") }

    val selectedEvent = state.events.find { it.id == selectedEventId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        VBackHeader(
            title = when {
                selectedEventId != null -> "Manage Event"
                showAllRegistrations -> "All Registrations"
                else -> "Event Management"
            },
            onBack = {
                when {
                    selectedEventId != null -> {
                        selectedEventId = null
                        viewModel.clearMessages()
                    }
                    showAllRegistrations -> {
                        showAllRegistrations = false
                        viewModel.loadEvents()
                    }
                    else -> onBack()
                }
            },
        )

        if (state.infoMessage != null) {
            Text(
                text = state.infoMessage!!,
                style = VTheme.type.caption.colored(c.successInk),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage!!,
                style = VTheme.type.caption.colored(c.dangerInk),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        if (selectedEventId != null && selectedEvent != null) {
            LaunchedEffect(selectedEventId) {
                viewModel.loadSlots(selectedEventId!!)
            }
            EventManageContent(
                event = selectedEvent,
                state = state,
                slotStart = slotStart,
                slotEnd = slotEnd,
                slotCapacity = slotCapacity,
                onSlotStartChange = { slotStart = it },
                onSlotEndChange = { slotEnd = it },
                onSlotCapacityChange = { slotCapacity = it },
                onCreateSlot = {
                    viewModel.createSlot(
                        eventId = selectedEventId!!,
                        startTime = slotStart,
                        endTime = slotEnd,
                        capacity = slotCapacity.toIntOrNull() ?: 1,
                    )
                    slotStart = ""
                    slotEnd = ""
                },
                onAutoGenerate = { rangeStart, rangeEnd, duration, capacity, breakAfter, breakDuration ->
                    viewModel.autoGenerateSlots(
                        selectedEventId!!,
                        AutoGenerateSlotsRequest(rangeStart, rangeEnd, duration, capacity, breakAfter, breakDuration),
                    )
                },
                onDeleteSlot = { slotId ->
                    viewModel.deleteSlot(selectedEventId!!, slotId)
                },
                onCancelEvent = {
                    viewModel.cancelEvent(selectedEventId!!)
                    selectedEventId = null
                },
                onExportCsv = {
                    viewModel.exportCsv(selectedEventId!!)
                },
                onViewRegistrations = {
                    viewModel.loadEventRegistrations(selectedEventId!!)
                },
                onToggleRegistration = { enabled ->
                    viewModel.updateRegistrationConfig(
                        selectedEventId!!,
                        UpdateRegistrationConfigRequest(registrationEnabled = enabled),
                    )
                },
            )
        } else if (showAllRegistrations) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    VCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Filters", style = VTheme.type.h3.colored(c.ink))
                            Spacer(Modifier.height(8.dp))
                            VInput(
                                value = statusFilter,
                                onValueChange = { statusFilter = it },
                                label = "Status filter (e.g. REGISTERED, CANCELLED)",
                                placeholder = "All statuses",
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            VButton(
                                text = "Apply Filter",
                                onClick = {
                                    viewModel.loadRegistrations(
                                        status = statusFilter.ifBlank { null },
                                    )
                                },
                                variant = VButtonVariant.Secondary,
                            )
                        }
                    }
                }

                if (state.isLoading && state.registrations.isEmpty()) {
                    item {
                        Text(
                            text = "Loading...",
                            style = VTheme.type.body.colored(c.ink3),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else if (state.registrations.isEmpty()) {
                    item {
                        Text(
                            text = "No registrations found. Tap Apply Filter to load.",
                            style = VTheme.type.body.colored(c.ink3),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    items(state.registrations) { reg ->
                        RegistrationCard(reg = reg)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                VButton(
                    text = "Events",
                    onClick = { viewModel.loadEvents() },
                    variant = VButtonVariant.Primary,
                )
                VButton(
                    text = "All Registrations",
                    onClick = {
                        showAllRegistrations = true
                        viewModel.loadRegistrations()
                    },
                    variant = VButtonVariant.Ghost,
                )
            }

            VStateHost(
                loading = state.isLoading,
                error = state.errorMessage,
                isEmpty = state.events.isEmpty() && !state.isLoading,
                emptyTitle = "No events with registration",
                onRetry = { viewModel.loadEvents() },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.events) { event ->
                        AdminEventCard(
                            event = event,
                            onClick = {
                                selectedEventId = event.id
                                viewModel.clearMessages()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventManageContent(
    event: AdminEventDto,
    state: AdminEventRegistrationState,
    slotStart: String,
    slotEnd: String,
    slotCapacity: String,
    onSlotStartChange: (String) -> Unit,
    onSlotEndChange: (String) -> Unit,
    onSlotCapacityChange: (String) -> Unit,
    onCreateSlot: () -> Unit,
    onAutoGenerate: (String, String, Int, Int, Int, Int) -> Unit,
    onDeleteSlot: (String) -> Unit,
    onCancelEvent: () -> Unit,
    onExportCsv: () -> Unit,
    onViewRegistrations: () -> Unit,
    onToggleRegistration: (Boolean) -> Unit,
) {
    val c = VTheme.colors
    var showAutoGen by remember { mutableStateOf(false) }
    var autoRangeStart by remember { mutableStateOf("09:00") }
    var autoRangeEnd by remember { mutableStateOf("12:00") }
    var autoDuration by remember { mutableStateOf("15") }
    var autoCapacity by remember { mutableStateOf("1") }
    var autoBreakAfter by remember { mutableStateOf("0") }
    var autoBreakDuration by remember { mutableStateOf("5") }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Event info card
        item {
            VCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = event.title, style = VTheme.type.h3.colored(c.ink))
                    Spacer(Modifier.height(4.dp))
                    Text(text = "${event.type} • ${event.startDate}", style = VTheme.type.body.colored(c.ink2))
                    if (event.venue != null) {
                        Text(text = "Venue: ${event.venue}", style = VTheme.type.caption.colored(c.ink3))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        VBadge(
                            text = if (event.registrationEnabled) "Registration open" else "Registration closed",
                            tone = if (event.registrationEnabled) VBadgeTone.Success else VBadgeTone.Neutral,
                        )
                        VBadge(text = "${event.slotCount} slots", tone = VBadgeTone.Accent)
                        VBadge(text = "${event.totalRegistrations} regs", tone = VBadgeTone.Accent)
                    }
                    Spacer(Modifier.height(8.dp))
                    VButton(
                        text = if (event.registrationEnabled) "Close Registration" else "Open Registration",
                        onClick = { onToggleRegistration(!event.registrationEnabled) },
                        variant = VButtonVariant.Secondary,
                        full = true,
                    )
                }
            }
        }

        // Slot management
        item {
            VCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Slot Management", style = VTheme.type.h3.colored(c.ink))
                    Spacer(Modifier.height(8.dp))
                    VInput(
                        value = slotStart,
                        onValueChange = onSlotStartChange,
                        label = "Start time (HH:mm)",
                        placeholder = "09:00",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    VInput(
                        value = slotEnd,
                        onValueChange = onSlotEndChange,
                        label = "End time (HH:mm)",
                        placeholder = "09:15",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    VInput(
                        value = slotCapacity,
                        onValueChange = onSlotCapacityChange,
                        label = "Capacity",
                        placeholder = "1",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    VButton(
                        text = "Create Slot",
                        onClick = onCreateSlot,
                        variant = VButtonVariant.Primary,
                        enabled = slotStart.isNotBlank() && slotEnd.isNotBlank() && !state.isCreating,
                        loading = state.isCreating,
                        full = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    VButton(
                        text = if (showAutoGen) "Hide Auto-Generate" else "Auto-Generate Slots",
                        onClick = { showAutoGen = !showAutoGen },
                        variant = VButtonVariant.Ghost,
                        full = true,
                    )
                    if (showAutoGen) {
                        Spacer(Modifier.height(8.dp))
                        VInput(
                            value = autoRangeStart,
                            onValueChange = { autoRangeStart = it },
                            label = "Range start (HH:mm)",
                            placeholder = "09:00",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        VInput(
                            value = autoRangeEnd,
                            onValueChange = { autoRangeEnd = it },
                            label = "Range end (HH:mm)",
                            placeholder = "12:00",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        VInput(
                            value = autoDuration,
                            onValueChange = { autoDuration = it },
                            label = "Slot duration (minutes)",
                            placeholder = "15",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        VInput(
                            value = autoCapacity,
                            onValueChange = { autoCapacity = it },
                            label = "Capacity per slot",
                            placeholder = "1",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            VInput(
                                value = autoBreakAfter,
                                onValueChange = { autoBreakAfter = it },
                                label = "Break after N slots",
                                placeholder = "0",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f),
                            )
                            VInput(
                                value = autoBreakDuration,
                                onValueChange = { autoBreakDuration = it },
                                label = "Break (min)",
                                placeholder = "5",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        VButton(
                            text = "Generate",
                            onClick = {
                                onAutoGenerate(
                                    autoRangeStart,
                                    autoRangeEnd,
                                    autoDuration.toIntOrNull() ?: 15,
                                    autoCapacity.toIntOrNull() ?: 1,
                                    autoBreakAfter.toIntOrNull() ?: 0,
                                    autoBreakDuration.toIntOrNull() ?: 5,
                                )
                            },
                            variant = VButtonVariant.Primary,
                            enabled = !state.isCreating,
                            loading = state.isCreating,
                            full = true,
                        )
                    }
                    if (state.slots.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        state.slots.forEach { slot ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${slot.startTime}-${slot.endTime} (cap ${slot.capacity})",
                                    style = VTheme.type.caption.colored(c.ink2),
                                )
                                VButton(
                                    text = "Delete",
                                    onClick = { onDeleteSlot(slot.id) },
                                    variant = VButtonVariant.Destructive,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Actions
        item {
            VCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Actions", style = VTheme.type.h3.colored(c.ink))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VButton(
                            text = "View Registrations",
                            onClick = onViewRegistrations,
                            variant = VButtonVariant.Secondary,
                        )
                        VButton(
                            text = "Export CSV",
                            onClick = onExportCsv,
                            variant = VButtonVariant.Secondary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    VButton(
                        text = "Cancel Event",
                        onClick = onCancelEvent,
                        variant = VButtonVariant.Destructive,
                        enabled = !state.isCancelling,
                        loading = state.isCancelling,
                        full = true,
                    )
                    if (state.csvData != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "CSV data ready (${state.csvData!!.length} chars)",
                            style = VTheme.type.caption.colored(c.successInk),
                        )
                    }
                }
            }
        }

        // Registrations for this event
        if (state.registrations.isNotEmpty()) {
            item {
                Text(
                    text = "Registrations (${state.registrations.size})",
                    style = VTheme.type.h3.colored(c.ink),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(state.registrations) { reg ->
                RegistrationCard(reg = reg)
            }
        }
    }
}

@Composable
private fun AdminEventCard(
    event: AdminEventDto,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    VCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = event.title, style = VTheme.type.h3.colored(c.ink))
            Spacer(Modifier.height(4.dp))
            Text(text = "${event.type} • ${event.startDate}", style = VTheme.type.body.colored(c.ink2))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VBadge(
                    text = if (event.registrationEnabled) "Open" else "Closed",
                    tone = if (event.registrationEnabled) VBadgeTone.Success else VBadgeTone.Neutral,
                )
                Text(
                    text = "${event.slotCount} slots • ${event.totalRegistrations} regs",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            }
        }
    }
}

@Composable
private fun RegistrationCard(reg: AdminRegistrationDto) {
    val c = VTheme.colors
    VCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = reg.eventTitle,
                style = VTheme.type.h3.colored(c.ink),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${reg.parentName} • ${reg.parentMobile}",
                style = VTheme.type.body.colored(c.ink2),
            )
            if (reg.studentName != null) {
                Text(
                    text = "Student: ${reg.studentName}",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            }
            if (reg.slotTime != null) {
                Text(
                    text = "Slot: ${reg.slotTime}",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val tone = when (reg.status) {
                    "REGISTERED" -> VBadgeTone.Success
                    "CHECKED_IN" -> VBadgeTone.Accent
                    "CANCELLED" -> VBadgeTone.Danger
                    else -> VBadgeTone.Neutral
                }
                VBadge(text = reg.status, tone = tone)
                Text(
                    text = "${reg.attendeeCount} attending • ${reg.eventDate}",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            }
        }
    }
}
