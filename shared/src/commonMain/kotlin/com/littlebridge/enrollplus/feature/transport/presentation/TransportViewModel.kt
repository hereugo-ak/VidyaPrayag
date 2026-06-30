package com.littlebridge.enrollplus.feature.transport.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.transport.domain.model.*
import com.littlebridge.enrollplus.feature.transport.domain.repository.TransportRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransportScreenState(
    val isLoading: Boolean = false,
    val routes: List<TransportRoute> = emptyList(),
    val vehicles: List<TransportVehicle> = emptyList(),
    val assignments: List<TransportAssignment> = emptyList(),
    val attendance: List<TransportAttendance> = emptyList(),
    val routeProgress: RouteProgress? = null,
    val childRoute: TransportRoute? = null,
    val error: String? = null,
    val infoMessage: String? = null,
)

class TransportViewModel(
    private val repository: TransportRepository,
    private val prefs: PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TransportScreenState())
    val state: StateFlow<TransportScreenState> = _state.asStateFlow()

    private suspend fun token(): String = prefs.getUserToken().first() ?: ""

    private var pollingJob: Job? = null

    // ── Admin: Routes ─────────────────────────────────────────────────────

    fun loadRoutes(all: Boolean = false) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.listRoutes(token(), all)) {
                is NetworkResult.Success -> _state.update { it.copy(isLoading = false, routes = result.data.data ?: emptyList()) }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun createRoute(request: CreateRouteRequest) {
        _state.update { it.copy(isLoading = true, error = null, infoMessage = null) }
        viewModelScope.launch {
            when (val result = repository.createRoute(token(), request)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isLoading = false, infoMessage = "Route created") }
                    loadRoutes()
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun updateRoute(routeId: String, request: UpdateRouteRequest) {
        viewModelScope.launch {
            when (val result = repository.updateRoute(token(), routeId, request)) {
                is NetworkResult.Success -> { _state.update { it.copy(infoMessage = "Route updated") }; loadRoutes() }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun deleteRoute(routeId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteRoute(token(), routeId)) {
                is NetworkResult.Success -> { _state.update { it.copy(infoMessage = "Route deleted") }; loadRoutes() }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    // ── Admin: Vehicles ───────────────────────────────────────────────────

    fun loadVehicles(all: Boolean = false) {
        viewModelScope.launch {
            when (val result = repository.listVehicles(token(), all)) {
                is NetworkResult.Success -> _state.update { it.copy(vehicles = result.data.data ?: emptyList()) }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun createVehicle(request: CreateVehicleRequest) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.createVehicle(token(), request)) {
                is NetworkResult.Success -> { _state.update { it.copy(isLoading = false, infoMessage = "Vehicle created") }; loadVehicles() }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun updateVehicle(vehicleId: String, request: UpdateVehicleRequest) {
        viewModelScope.launch {
            when (val result = repository.updateVehicle(token(), vehicleId, request)) {
                is NetworkResult.Success -> { _state.update { it.copy(infoMessage = "Vehicle updated") }; loadVehicles() }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun deleteVehicle(vehicleId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteVehicle(token(), vehicleId)) {
                is NetworkResult.Success -> { _state.update { it.copy(infoMessage = "Vehicle deleted") }; loadVehicles() }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    // ── Admin: Assignments ────────────────────────────────────────────────

    fun loadAssignments(routeId: String? = null, studentId: String? = null) {
        viewModelScope.launch {
            when (val result = repository.listAssignments(token(), routeId, studentId)) {
                is NetworkResult.Success -> _state.update { it.copy(assignments = result.data.data ?: emptyList()) }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun createAssignment(request: CreateAssignmentRequest) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.createAssignment(token(), request)) {
                is NetworkResult.Success -> { _state.update { it.copy(isLoading = false, infoMessage = "Student assigned") }; loadAssignments() }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(isLoading = false, error = "Connection error") }
            }
        }
    }

    fun deactivateAssignment(assignmentId: String) {
        viewModelScope.launch {
            when (val result = repository.deactivateAssignment(token(), assignmentId)) {
                is NetworkResult.Success -> { _state.update { it.copy(infoMessage = "Assignment deactivated") }; loadAssignments() }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    // ── Admin: Attendance ─────────────────────────────────────────────────

    fun loadAttendance(routeId: String, date: String) {
        viewModelScope.launch {
            when (val result = repository.getDailyAttendance(token(), routeId, date)) {
                is NetworkResult.Success -> _state.update { it.copy(attendance = result.data.data ?: emptyList()) }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    // ── Parent: Live tracking ─────────────────────────────────────────────

    fun loadLiveLocation(childId: String) {
        viewModelScope.launch {
            when (val result = repository.getLiveLocation(token(), childId)) {
                is NetworkResult.Success -> _state.update { it.copy(routeProgress = result.data.data) }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun startPollingLiveLocation(childId: String, intervalMs: Long = 30_000L) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                loadLiveLocation(childId)
                delay(intervalMs)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun loadChildRoute(childId: String) {
        viewModelScope.launch {
            when (val result = repository.getRouteForChild(token(), childId)) {
                is NetworkResult.Success -> _state.update { it.copy(childRoute = result.data.data) }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    // ── Driver: Location + Pickup/Drop ────────────────────────────────────

    fun updateLocation(request: UpdateLocationRequest) {
        viewModelScope.launch {
            when (val result = repository.updateLocation(token(), request)) {
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
                else -> {}
            }
        }
    }

    fun markPickup(request: MarkPickupDropRequest) {
        viewModelScope.launch {
            when (val result = repository.markPickup(token(), request)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(infoMessage = "Pickup marked") }
                    result.data.data?.let { updated ->
                        _state.update { s ->
                            s.copy(attendance = s.attendance.map {
                                if (it.studentId == updated.studentId && it.date == updated.date) updated else it
                            })
                        }
                    }
                }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun markDrop(request: MarkPickupDropRequest) {
        viewModelScope.launch {
            when (val result = repository.markDrop(token(), request)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(infoMessage = "Drop marked") }
                    result.data.data?.let { updated ->
                        _state.update { s ->
                            s.copy(attendance = s.attendance.map {
                                if (it.studentId == updated.studentId && it.date == updated.date) updated else it
                            })
                        }
                    }
                }
                is NetworkResult.Error -> _state.update { it.copy(error = result.message) }
                is NetworkResult.ConnectionError -> _state.update { it.copy(error = "Connection error") }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, infoMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
