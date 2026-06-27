package com.littlebridge.enrollplus.ui.v2.screens.teacher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.transport.domain.model.MarkPickupDropRequest
import com.littlebridge.enrollplus.feature.transport.domain.model.TransportAttendance
import com.littlebridge.enrollplus.feature.transport.presentation.TransportViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.screens.VSectionHeader
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TransportAttendanceScreenV2(
    routeId: String,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TransportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val c = VTheme.colors
    var today by remember { mutableStateOf("") }
    var selectedRouteId by remember { mutableStateOf(routeId) }

    LaunchedEffect(Unit) {
        if (today.isEmpty()) today = com.littlebridge.enrollplus.util.todayIso()
        if (selectedRouteId.isEmpty()) {
            viewModel.loadRoutes(all = true)
        }
    }

    LaunchedEffect(selectedRouteId, today) {
        if (selectedRouteId.isNotEmpty() && today.isNotEmpty()) {
            viewModel.loadAttendance(selectedRouteId, today)
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        VBackHeader(title = "Transport Attendance", onBack = onBack)

        if (selectedRouteId.isEmpty()) {
            // ── Route picker ───────────────────────────────────────────────
            VStateHost(
                loading = state.isLoading && state.routes.isEmpty(),
                error = state.error,
                isEmpty = state.routes.isEmpty() && !state.isLoading,
                emptyTitle = "No routes found",
                emptyBody = "No transport routes have been created yet.",
                onRetry = { viewModel.loadRoutes(all = true) },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        VSectionHeader(title = "Select a Route")
                    }
                    items(state.routes) { route ->
                        VCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRouteId = route.id },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        route.name,
                                        style = VTheme.type.h3,
                                        color = c.ink,
                                    )
                                    Text(
                                        "${route.stops.size} stops",
                                        style = VTheme.type.caption,
                                        color = c.ink2,
                                    )
                                }
                                VBadge(
                                    text = if (route.isActive) "Active" else "Inactive",
                                    tone = if (route.isActive) VBadgeTone.Success else VBadgeTone.Neutral,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ── Attendance list ────────────────────────────────────────────
            VStateHost(
                loading = state.isLoading,
                error = state.error,
                isEmpty = state.attendance.isEmpty(),
                emptyTitle = "No students assigned",
                emptyBody = "No students are assigned to this route today.",
                onRetry = { viewModel.loadAttendance(selectedRouteId, today) },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        VSectionHeader(title = "Students ($today)")
                    }
                    items(state.attendance) { record ->
                        AttendanceCard(
                            record = record,
                            onPickup = {
                                viewModel.markPickup(
                                    MarkPickupDropRequest(
                                        studentId = record.studentId,
                                        routeId = record.routeId,
                                        date = record.date,
                                    )
                                )
                            },
                            onDrop = {
                                viewModel.markDrop(
                                    MarkPickupDropRequest(
                                        studentId = record.studentId,
                                        routeId = record.routeId,
                                        date = record.date,
                                    )
                                )
                            },
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AttendanceCard(
    record: TransportAttendance,
    onPickup: () -> Unit,
    onDrop: () -> Unit,
) {
    val c = VTheme.colors
    VCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                record.studentName ?: record.studentId,
                style = VTheme.type.h3,
                color = c.ink,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                record.pickupStatus?.let {
                    VBadge(text = "Pickup: $it", tone = if (it == "picked") VBadgeTone.Success else VBadgeTone.Warning)
                }
                record.dropStatus?.let {
                    VBadge(text = "Drop: $it", tone = if (it == "dropped") VBadgeTone.Success else VBadgeTone.Warning)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VButton(
                text = "Mark Pickup",
                variant = VButtonVariant.Primary,
                size = VButtonSize.Sm,
                onClick = onPickup,
                enabled = record.pickupStatus != "picked",
            )
            VButton(
                text = "Mark Drop",
                variant = VButtonVariant.Secondary,
                size = VButtonSize.Sm,
                onClick = onDrop,
                enabled = record.dropStatus != "dropped",
            )
        }
    }
}
