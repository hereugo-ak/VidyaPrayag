/*
 * File: TeacherPtmEventRegistrationScreenV2.kt
 * Module: ui.v2.screens.teacher
 *
 * Teacher PTM Event Registration screen — list PTM events, view slot-wise
 * bookings, and check in parents. Wired to TeacherEventRegistrationViewModel.
 */
package com.littlebridge.enrollplus.ui.v2.screens.teacher

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.event.domain.model.SlotBookingDto
import com.littlebridge.enrollplus.feature.event.domain.model.TeacherPtmEventDto
import com.littlebridge.enrollplus.feature.event.domain.model.TeacherSlotDto
import com.littlebridge.enrollplus.feature.event.presentation.TeacherEventRegistrationViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TeacherPtmEventRegistrationScreenV2(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeacherEventRegistrationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val c = VTheme.colors
    var selectedEventId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        VBackHeader(
            title = if (selectedEventId != null) "PTM Detail" else "PTM Events",
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
            PtmDetailContent(
                eventDetail = state.eventDetail!!,
                slots = state.slots,
                isCheckingIn = state.isCheckingIn,
                onCheckIn = { registrationId ->
                    viewModel.checkinParent(selectedEventId!!, registrationId)
                },
            )
        } else {
            VStateHost(
                loading = state.isLoading,
                error = state.errorMessage,
                isEmpty = state.events.isEmpty() && !state.isLoading,
                emptyTitle = "No PTM events scheduled",
                onRetry = { viewModel.loadPtmEvents() },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.events) { event ->
                        PtmEventCard(
                            event = event,
                            onClick = {
                                selectedEventId = event.id
                                viewModel.loadPtmDetail(event.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PtmDetailContent(
    eventDetail: TeacherPtmEventDto,
    slots: List<TeacherSlotDto>,
    isCheckingIn: Boolean,
    onCheckIn: (String) -> Unit,
) {
    val c = VTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            VCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = eventDetail.title, style = VTheme.type.h3.colored(c.ink))
                    Spacer(Modifier.height(4.dp))
                    Text(text = eventDetail.date, style = VTheme.type.body.colored(c.ink2))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        VBadge(text = "${eventDetail.totalRegistrations} registered", tone = VBadgeTone.Accent)
                        VBadge(text = "${eventDetail.checkedIn} checked in", tone = VBadgeTone.Success)
                    }
                }
            }
        }

        if (slots.isEmpty()) {
            item {
                Text(
                    text = "No slots configured for this event",
                    style = VTheme.type.body.colored(c.ink3),
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            items(slots) { slot ->
                SlotWithBookingsCard(
                    slot = slot,
                    isCheckingIn = isCheckingIn,
                    onCheckIn = onCheckIn,
                )
            }
        }
    }
}

@Composable
private fun SlotWithBookingsCard(
    slot: TeacherSlotDto,
    isCheckingIn: Boolean,
    onCheckIn: (String) -> Unit,
) {
    val c = VTheme.colors
    VCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${slot.startTime} - ${slot.endTime}",
                    style = VTheme.type.h3.colored(c.ink),
                )
                VBadge(
                    text = "${slot.bookedCount}/${slot.capacity}",
                    tone = if (slot.bookedCount >= slot.capacity) VBadgeTone.Danger else VBadgeTone.Neutral,
                )
            }

            if (slot.bookings.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "No bookings for this slot",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            } else {
                Spacer(Modifier.height(8.dp))
                slot.bookings.forEach { booking ->
                    BookingRow(
                        booking = booking,
                        isCheckingIn = isCheckingIn,
                        onCheckIn = { onCheckIn(booking.registrationId) },
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun BookingRow(
    booking: SlotBookingDto,
    isCheckingIn: Boolean,
    onCheckIn: () -> Unit,
) {
    val c = VTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = booking.parentName,
                style = VTheme.type.body.colored(c.ink),
            )
            Text(
                text = "Student: ${booking.studentName} • ${booking.attendeeCount} attendee(s)",
                style = VTheme.type.caption.colored(c.ink3),
            )
            if (booking.parentMobile.isNotBlank()) {
                Text(
                    text = "📞 ${booking.parentMobile}",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            }
        }
        if (booking.status == "CHECKED_IN") {
            VBadge(text = "Checked In", tone = VBadgeTone.Success)
        } else {
            VButton(
                text = "Check In",
                onClick = onCheckIn,
                variant = VButtonVariant.Secondary,
                enabled = !isCheckingIn,
                loading = isCheckingIn,
            )
        }
    }
}

@Composable
private fun PtmEventCard(
    event: TeacherPtmEventDto,
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
                text = event.date,
                style = VTheme.type.body.colored(c.ink2),
            )
            if (event.className.isNotBlank()) {
                Text(
                    text = "Classes: ${event.className}",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VBadge(text = "${event.totalRegistrations} registered", tone = VBadgeTone.Accent)
                VBadge(text = "${event.checkedIn} checked in", tone = VBadgeTone.Success)
            }
        }
    }
}
