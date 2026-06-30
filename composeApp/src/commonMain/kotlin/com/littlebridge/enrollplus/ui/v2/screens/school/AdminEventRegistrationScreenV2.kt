/*
 * File: AdminEventRegistrationScreenV2.kt
 * Module: ui.v2.screens.school
 *
 * Admin Event Registration management screen — registration list, slot management,
 * config, and event cancellation. Wired to AdminEventRegistrationViewModel.
 */
package com.littlebridge.enrollplus.ui.v2.screens.school

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.event.domain.model.AdminRegistrationDto
import com.littlebridge.enrollplus.feature.event.presentation.AdminEventRegistrationViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VInput
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
    var showSlotPanel by remember { mutableStateOf(false) }
    var slotEventId by remember { mutableStateOf("") }
    var slotStart by remember { mutableStateOf("") }
    var slotEnd by remember { mutableStateOf("") }
    var slotCapacity by remember { mutableStateOf("1") }
    var cancelEventId by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        VBackHeader(title = "Event Registrations", onBack = onBack)

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

            item {
                VCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Slot Management", style = VTheme.type.h3.colored(c.ink))
                        Spacer(Modifier.height(8.dp))
                        VInput(
                            value = slotEventId,
                            onValueChange = { slotEventId = it },
                            label = "Event ID",
                            placeholder = "UUID",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        VInput(
                            value = slotStart,
                            onValueChange = { slotStart = it },
                            label = "Start time (HH:mm)",
                            placeholder = "09:00",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        VInput(
                            value = slotEnd,
                            onValueChange = { slotEnd = it },
                            label = "End time (HH:mm)",
                            placeholder = "09:15",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        VInput(
                            value = slotCapacity,
                            onValueChange = { slotCapacity = it },
                            label = "Capacity",
                            placeholder = "1",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VButton(
                                text = "Create Slot",
                                onClick = {
                                    viewModel.createSlot(
                                        eventId = slotEventId,
                                        startTime = slotStart,
                                        endTime = slotEnd,
                                        capacity = slotCapacity.toIntOrNull() ?: 1,
                                    )
                                },
                                variant = VButtonVariant.Primary,
                                enabled = slotEventId.isNotBlank() && slotStart.isNotBlank() && slotEnd.isNotBlank() && !state.isCreating,
                                loading = state.isCreating,
                            )
                            VButton(
                                text = "Manage Slots",
                                onClick = { showSlotPanel = !showSlotPanel },
                                variant = VButtonVariant.Ghost,
                            )
                        }
                        if (showSlotPanel && state.slots.isNotEmpty()) {
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
                                        onClick = { viewModel.deleteSlot(slotEventId, slot.id) },
                                        variant = VButtonVariant.Destructive,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                VCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Event Actions", style = VTheme.type.h3.colored(c.ink))
                        Spacer(Modifier.height(8.dp))
                        VInput(
                            value = cancelEventId,
                            onValueChange = { cancelEventId = it },
                            label = "Event ID for cancellation / export",
                            placeholder = "UUID",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VButton(
                                text = "Cancel Event",
                                onClick = { viewModel.cancelEvent(cancelEventId) },
                                variant = VButtonVariant.Destructive,
                                enabled = cancelEventId.isNotBlank() && !state.isCancelling,
                                loading = state.isCancelling,
                            )
                            VButton(
                                text = "Export CSV",
                                onClick = { viewModel.exportCsv(cancelEventId) },
                                variant = VButtonVariant.Secondary,
                                enabled = cancelEventId.isNotBlank(),
                            )
                        }
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

            item {
                Text(
                    text = "Registrations",
                    style = VTheme.type.h3.colored(c.ink),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
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
                        text = "No registrations found",
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
