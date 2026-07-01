/*
 * File: ParentEventRegistrationScreenV2.kt
 * Module: ui.v2.screens.parent
 *
 * Parent Event Registration screen — browse events, view detail with slots,
 * register, cancel, reschedule, and view my registrations.
 * Wired to ParentEventRegistrationViewModel.
 */
package com.littlebridge.enrollplus.ui.v2.screens.parent

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.event.domain.model.EventSlotDto
import com.littlebridge.enrollplus.feature.event.domain.model.ParentEventDto
import com.littlebridge.enrollplus.feature.event.domain.model.RegistrationDto
import com.littlebridge.enrollplus.feature.event.presentation.ParentEventRegistrationViewModel
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
fun ParentEventRegistrationScreenV2(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ParentEventRegistrationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val c = VTheme.colors
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    var showMyRegistrations by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        VBackHeader(
            title = if (selectedEventId != null) "Event Detail" else "Events",
            onBack = {
                if (selectedEventId != null) {
                    selectedEventId = null
                    viewModel.clearMessages()
                } else {
                    onBack()
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

        if (selectedEventId != null && state.eventDetail != null) {
            EventDetailContent(
                eventDetail = state.eventDetail!!,
                isLoading = state.isRegistering || state.isCancelling || state.isRescheduling,
                onRegister = { slotId, attendeeCount ->
                    viewModel.register(
                        eventId = selectedEventId!!,
                        slotId = slotId,
                        studentId = null,
                        attendeeCount = attendeeCount,
                    )
                },
                onCancel = {
                    viewModel.cancelRegistration(selectedEventId!!)
                },
                onReschedule = { newSlotId ->
                    viewModel.reschedule(selectedEventId!!, newSlotId)
                },
            )
        } else if (showMyRegistrations) {
            MyRegistrationsContent(
                registrations = state.myRegistrations,
                isLoading = state.isLoading,
                onBackToList = {
                    showMyRegistrations = false
                    viewModel.loadEvents()
                },
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                VButton(
                    text = "Upcoming Events",
                    onClick = { viewModel.loadEvents() },
                    variant = VButtonVariant.Primary,
                )
                VButton(
                    text = "My Registrations",
                    onClick = {
                        showMyRegistrations = true
                        viewModel.loadMyRegistrations()
                    },
                    variant = VButtonVariant.Ghost,
                )
            }

            VStateHost(
                loading = state.isLoading,
                error = state.errorMessage,
                isEmpty = state.events.isEmpty() && !state.isLoading,
                emptyTitle = "No upcoming events with registration",
                onRetry = { viewModel.loadEvents() },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.events) { event ->
                        EventCard(
                            event = event,
                            onClick = {
                                selectedEventId = event.id
                                viewModel.loadEventDetail(event.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventDetailContent(
    eventDetail: com.littlebridge.enrollplus.feature.event.domain.model.ParentEventDetailResponse,
    isLoading: Boolean,
    onRegister: (String?, Int) -> Unit,
    onCancel: () -> Unit,
    onReschedule: (String) -> Unit,
) {
    val c = VTheme.colors
    val event = eventDetail.event
    var selectedSlotId by remember { mutableStateOf<String?>(null) }
    var attendeeCount by remember { mutableStateOf("1") }
    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Registration") },
            text = { Text("Are you sure you want to cancel your registration for ${event.title}?") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    onCancel()
                }) { Text("Yes, Cancel") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            VCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = event.title, style = VTheme.type.h3.colored(c.ink))
                    Spacer(Modifier.height(4.dp))
                    Text(text = event.startDate, style = VTheme.type.body.colored(c.ink2))
                    if (event.venue != null) {
                        Text(text = "Venue: ${event.venue}", style = VTheme.type.caption.colored(c.ink3))
                    }
                    if (event.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(text = event.description, style = VTheme.type.body.colored(c.ink2))
                    }
                    Spacer(Modifier.height(8.dp))
                    if (event.registrationDeadline != null) {
                        Text(text = "Register by: ${event.registrationDeadline}", style = VTheme.type.caption.colored(c.ink3))
                    }
                    if (event.myRegistrationStatus != null) {
                        VBadge(
                            text = "Registered: ${event.myRegistrationStatus}",
                            tone = VBadgeTone.Success,
                        )
                    } else if (event.registrationEnabled || event.type == "PTM") {
                        VBadge(text = "Registration open", tone = VBadgeTone.Accent)
                    }
                    if (event.conflictingEventTitle != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "⚠ Conflicts with: ${event.conflictingEventTitle}",
                            style = VTheme.type.caption.colored(c.warningInk),
                        )
                    }
                }
            }
        }

        if (eventDetail.slots.isNotEmpty()) {
            item {
                Text(
                    text = "Select a time slot",
                    style = VTheme.type.h3.colored(c.ink),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(eventDetail.slots) { slot ->
                SlotCard(
                    slot = slot,
                    isSelected = selectedSlotId == slot.id,
                    onClick = { selectedSlotId = slot.id },
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            if (event.myRegistrationStatus != null) {
                if (eventDetail.slots.isNotEmpty()) {
                    VButton(
                        text = "Reschedule",
                        onClick = {
                            selectedSlotId?.let { onReschedule(it) }
                        },
                        variant = VButtonVariant.Secondary,
                        enabled = selectedSlotId != null && !isLoading,
                        loading = isLoading,
                        full = true,
                    )
                }
                Spacer(Modifier.height(8.dp))
                VButton(
                    text = "Cancel Registration",
                    onClick = { showCancelDialog = true },
                    variant = VButtonVariant.Destructive,
                    enabled = !isLoading,
                    loading = isLoading,
                    full = true,
                )
            } else {
                if (eventDetail.slots.isEmpty()) {
                    VInput(
                        value = attendeeCount,
                        onValueChange = { attendeeCount = it },
                        label = "Number of attendees",
                        placeholder = "1",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                val canRegister = if (eventDetail.slots.isNotEmpty()) {
                    selectedSlotId != null && !isLoading
                } else {
                    !isLoading
                }
                VButton(
                    text = "Register",
                    onClick = { onRegister(selectedSlotId, attendeeCount.toIntOrNull()?.coerceAtLeast(1) ?: 1) },
                    variant = VButtonVariant.Primary,
                    enabled = canRegister,
                    loading = isLoading,
                    full = true,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SlotCard(
    slot: EventSlotDto,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    VCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "${slot.startTime} - ${slot.endTime}",
                    style = VTheme.type.body.colored(c.ink),
                )
                Text(
                    text = "${slot.bookedCount}/${slot.capacity} booked",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            }
            if (slot.isFull) {
                VBadge(text = "Full", tone = VBadgeTone.Danger)
            } else if (slot.myRegistration) {
                VBadge(text = "Your slot", tone = VBadgeTone.Success)
            } else if (isSelected) {
                VBadge(text = "Selected", tone = VBadgeTone.Accent)
            }
        }
    }
}

@Composable
private fun MyRegistrationsContent(
    registrations: List<RegistrationDto>,
    isLoading: Boolean,
    onBackToList: () -> Unit,
) {
    val c = VTheme.colors
    VStateHost(
        loading = isLoading,
        error = null,
        isEmpty = registrations.isEmpty() && !isLoading,
        emptyTitle = "No registrations yet",
        onRetry = onBackToList,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(registrations) { reg ->
                VCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = reg.eventTitle, style = VTheme.type.h3.colored(c.ink))
                        Spacer(Modifier.height(4.dp))
                        Text(text = reg.eventDate, style = VTheme.type.body.colored(c.ink2))
                        if (reg.slotStartTime != null) {
                            Text(
                                text = "Slot: ${reg.slotStartTime} - ${reg.slotEndTime}",
                                style = VTheme.type.caption.colored(c.ink3),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        val tone = when (reg.status) {
                            "REGISTERED" -> VBadgeTone.Success
                            "CHECKED_IN" -> VBadgeTone.Accent
                            "CANCELLED" -> VBadgeTone.Danger
                            else -> VBadgeTone.Neutral
                        }
                        VBadge(text = reg.status, tone = tone)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: ParentEventDto,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    VCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = event.title,
                style = VTheme.type.h3.colored(c.ink),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = event.startDate,
                style = VTheme.type.body.colored(c.ink2),
            )
            if (event.venue != null) {
                Text(
                    text = "Venue: ${event.venue}",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (event.myRegistrationStatus != null) {
                    VBadge(text = event.myRegistrationStatus!!, tone = VBadgeTone.Success)
                } else if (event.registrationEnabled || event.type == "PTM") {
                    VBadge(text = "Registration open", tone = VBadgeTone.Accent)
                }
                if (event.hasSlots) {
                    Text(
                        text = "Time slots available",
                        style = VTheme.type.caption.colored(c.ink3),
                    )
                }
            }
        }
    }
}
