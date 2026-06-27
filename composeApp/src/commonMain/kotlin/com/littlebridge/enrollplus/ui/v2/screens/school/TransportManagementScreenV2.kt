package com.littlebridge.enrollplus.ui.v2.screens.school

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.littlebridge.enrollplus.feature.transport.domain.model.CreateAssignmentRequest
import com.littlebridge.enrollplus.feature.transport.domain.model.CreateRouteRequest
import com.littlebridge.enrollplus.feature.transport.domain.model.CreateVehicleRequest
import com.littlebridge.enrollplus.feature.transport.domain.model.TransportRoute
import com.littlebridge.enrollplus.feature.transport.domain.model.TransportVehicle
import com.littlebridge.enrollplus.feature.transport.domain.model.TransportAssignment
import com.littlebridge.enrollplus.feature.transport.presentation.TransportViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.screens.VSectionHeader
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TransportManagementScreenV2(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TransportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    var showRouteForm by remember { mutableStateOf(false) }
    var showVehicleForm by remember { mutableStateOf(false) }
    var showAssignmentForm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadRoutes()
        viewModel.loadVehicles()
        viewModel.loadAssignments()
    }

    LaunchedEffect(state.infoMessage) {
        if (state.infoMessage != null) {
            showRouteForm = false
            showVehicleForm = false
            showAssignmentForm = false
            viewModel.clearMessages()
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        VBackHeader(title = "Transport Management", onBack = onBack)

        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = false,
            onRetry = { viewModel.loadRoutes() },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ── Routes section ──────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VSectionHeader(title = "Routes (${state.routes.size})")
                        VButton(
                            text = "+ Add Route",
                            variant = VButtonVariant.Primary,
                            size = VButtonSize.Sm,
                            onClick = { showRouteForm = !showRouteForm },
                        )
                    }
                }
                if (showRouteForm) {
                    item { CreateRouteForm(viewModel = viewModel) }
                }
                items(state.routes) { route ->
                    RouteCard(
                        route = route,
                        onDelete = { viewModel.deleteRoute(route.id) },
                    )
                }

                // ── Vehicles section ────────────────────────────────────────
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VSectionHeader(title = "Vehicles (${state.vehicles.size})")
                        VButton(
                            text = "+ Add Vehicle",
                            variant = VButtonVariant.Primary,
                            size = VButtonSize.Sm,
                            onClick = { showVehicleForm = !showVehicleForm },
                        )
                    }
                }
                if (showVehicleForm) {
                    item { CreateVehicleForm(viewModel = viewModel, routes = state.routes) }
                }
                items(state.vehicles) { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        onDelete = { viewModel.deleteVehicle(vehicle.id) },
                    )
                }

                // ── Assignments section ─────────────────────────────────────
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VSectionHeader(title = "Student Assignments (${state.assignments.size})")
                        VButton(
                            text = "+ Assign",
                            variant = VButtonVariant.Primary,
                            size = VButtonSize.Sm,
                            onClick = { showAssignmentForm = !showAssignmentForm },
                        )
                    }
                }
                if (showAssignmentForm) {
                    item { CreateAssignmentForm(viewModel = viewModel, routes = state.routes, vehicles = state.vehicles) }
                }
                items(state.assignments) { assignment ->
                    AssignmentCard(
                        assignment = assignment,
                        onDeactivate = { viewModel.deactivateAssignment(assignment.id) },
                    )
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun CreateRouteForm(viewModel: TransportViewModel) {
    val c = VTheme.colors
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    VCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("New Route", style = VTheme.type.h3, color = c.ink)
            VInput(
                value = name,
                onValueChange = { name = it },
                label = "Route name",
                placeholder = "e.g. Route A — North Sector",
                modifier = Modifier.fillMaxWidth(),
            )
            VInput(
                value = description,
                onValueChange = { description = it },
                label = "Description (optional)",
                placeholder = "Covers northern residential areas",
                modifier = Modifier.fillMaxWidth(),
            )
            VButton(
                text = "Create Route",
                variant = VButtonVariant.Primary,
                size = VButtonSize.Sm,
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createRoute(CreateRouteRequest(name = name, description = description.ifBlank { null }))
                    }
                },
            )
        }
    }
}

@Composable
private fun CreateVehicleForm(viewModel: TransportViewModel, routes: List<TransportRoute>) {
    val c = VTheme.colors
    var busNumber by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("40") }
    var driverName by remember { mutableStateOf("") }
    var driverPhone by remember { mutableStateOf("") }
    var selectedRouteId by remember { mutableStateOf("") }

    VCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("New Vehicle", style = VTheme.type.h3, color = c.ink)
            VInput(
                value = busNumber,
                onValueChange = { busNumber = it },
                label = "Bus number",
                placeholder = "e.g. KA-01-AB-1234",
                modifier = Modifier.fillMaxWidth(),
            )
            VInput(
                value = capacity,
                onValueChange = { capacity = it },
                label = "Capacity",
                placeholder = "40",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                modifier = Modifier.fillMaxWidth(),
            )
            VInput(
                value = driverName,
                onValueChange = { driverName = it },
                label = "Driver name (optional)",
                modifier = Modifier.fillMaxWidth(),
            )
            VInput(
                value = driverPhone,
                onValueChange = { driverPhone = it },
                label = "Driver phone (optional)",
                modifier = Modifier.fillMaxWidth(),
            )
            if (routes.isNotEmpty()) {
                Text("Assign to route (optional):", style = VTheme.type.caption, color = c.ink2)
                routes.forEach { route ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRouteId = if (selectedRouteId == route.id) "" else route.id }
                            .padding(vertical = 4.dp),
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedRouteId == route.id,
                            onClick = { selectedRouteId = if (selectedRouteId == route.id) "" else route.id },
                        )
                        Text(route.name, style = VTheme.type.body, color = c.ink)
                    }
                }
            }
            VButton(
                text = "Create Vehicle",
                variant = VButtonVariant.Primary,
                size = VButtonSize.Sm,
                onClick = {
                    if (busNumber.isNotBlank()) {
                        viewModel.createVehicle(
                            CreateVehicleRequest(
                                busNumber = busNumber,
                                capacity = capacity.toIntOrNull() ?: 40,
                                routeId = selectedRouteId.ifBlank { null },
                                driverName = driverName.ifBlank { null },
                                driverPhone = driverPhone.ifBlank { null },
                            )
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun CreateAssignmentForm(
    viewModel: TransportViewModel,
    routes: List<TransportRoute>,
    vehicles: List<TransportVehicle>,
) {
    val c = VTheme.colors
    var studentId by remember { mutableStateOf("") }
    var selectedRouteId by remember { mutableStateOf("") }
    var selectedStopId by remember { mutableStateOf("") }
    var selectedVehicleId by remember { mutableStateOf("") }

    val selectedRoute = routes.find { it.id == selectedRouteId }

    VCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Assign Student to Route", style = VTheme.type.h3, color = c.ink)
            VInput(
                value = studentId,
                onValueChange = { studentId = it },
                label = "Student ID",
                placeholder = "Paste student UUID",
                modifier = Modifier.fillMaxWidth(),
            )
            if (routes.isNotEmpty()) {
                Text("Select route:", style = VTheme.type.caption, color = c.ink2)
                routes.forEach { route ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedRouteId = if (selectedRouteId == route.id) "" else route.id
                                selectedStopId = ""
                            }
                            .padding(vertical = 4.dp),
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedRouteId == route.id,
                            onClick = {
                                selectedRouteId = if (selectedRouteId == route.id) "" else route.id
                                selectedStopId = ""
                            },
                        )
                        Text(route.name, style = VTheme.type.body, color = c.ink)
                    }
                }
            }
            if (selectedRoute != null && selectedRoute.stops.isNotEmpty()) {
                Text("Select stop:", style = VTheme.type.caption, color = c.ink2)
                selectedRoute.stops.forEach { stop ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedStopId = if (selectedStopId == stop.id) "" else stop.id
                            }
                            .padding(vertical = 4.dp),
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedStopId == stop.id,
                            onClick = { selectedStopId = if (selectedStopId == stop.id) "" else stop.id },
                        )
                        Text("${stop.name} (#${stop.sequence})", style = VTheme.type.body, color = c.ink)
                    }
                }
            }
            if (vehicles.isNotEmpty()) {
                Text("Select vehicle:", style = VTheme.type.caption, color = c.ink2)
                vehicles.forEach { vehicle ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedVehicleId = if (selectedVehicleId == vehicle.id) "" else vehicle.id
                            }
                            .padding(vertical = 4.dp),
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedVehicleId == vehicle.id,
                            onClick = { selectedVehicleId = if (selectedVehicleId == vehicle.id) "" else vehicle.id },
                        )
                        Text(vehicle.busNumber, style = VTheme.type.body, color = c.ink)
                    }
                }
            }
            VButton(
                text = "Assign Student",
                variant = VButtonVariant.Primary,
                size = VButtonSize.Sm,
                enabled = studentId.isNotBlank() && selectedRouteId.isNotBlank() && selectedStopId.isNotBlank() && selectedVehicleId.isNotBlank(),
                onClick = {
                    viewModel.createAssignment(
                        CreateAssignmentRequest(
                            studentId = studentId,
                            routeId = selectedRouteId,
                            stopId = selectedStopId,
                            vehicleId = selectedVehicleId,
                        )
                    )
                },
            )
        }
    }
}

@Composable
private fun RouteCard(
    route: TransportRoute,
    onDelete: () -> Unit,
) {
    val c = VTheme.colors
    VCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(route.name, style = VTheme.type.h3, color = c.ink)
                route.description?.let {
                    Text(it, style = VTheme.type.caption, color = c.ink2)
                }
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
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VButton(
                text = "Delete",
                variant = VButtonVariant.Destructive,
                size = VButtonSize.Sm,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun VehicleCard(
    vehicle: TransportVehicle,
    onDelete: () -> Unit,
) {
    val c = VTheme.colors
    VCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(vehicle.busNumber, style = VTheme.type.h3, color = c.ink)
                Text(
                    "Capacity: ${vehicle.capacity}",
                    style = VTheme.type.caption,
                    color = c.ink2,
                )
                vehicle.driverName?.let {
                    Text("Driver: $it", style = VTheme.type.caption, color = c.ink2)
                }
            }
            VBadge(
                text = if (vehicle.isActive) "Active" else "Inactive",
                tone = if (vehicle.isActive) VBadgeTone.Success else VBadgeTone.Neutral,
            )
        }
        Spacer(Modifier.height(8.dp))
        VButton(
            text = "Delete",
            variant = VButtonVariant.Destructive,
            size = VButtonSize.Sm,
            onClick = onDelete,
        )
    }
}

@Composable
private fun AssignmentCard(
    assignment: TransportAssignment,
    onDeactivate: () -> Unit,
) {
    val c = VTheme.colors
    VCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                assignment.studentName ?: assignment.studentId,
                style = VTheme.type.h3,
                color = c.ink,
            )
            Text(
                "Route: ${assignment.routeName ?: assignment.routeId}",
                style = VTheme.type.body,
                color = c.ink2,
            )
            Text(
                "Stop: ${assignment.stopName ?: assignment.stopId}",
                style = VTheme.type.caption,
                color = c.ink2,
            )
            Text(
                "Bus: ${assignment.busNumber ?: assignment.vehicleId}",
                style = VTheme.type.caption,
                color = c.ink2,
            )
            Spacer(Modifier.height(8.dp))
            VButton(
                text = "Deactivate",
                variant = VButtonVariant.Destructive,
                size = VButtonSize.Sm,
                onClick = onDeactivate,
            )
        }
    }
}
