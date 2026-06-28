package com.littlebridge.enrollplus.feature.transport.domain.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.transport.domain.model.*

interface TransportRepository {

    // Admin: Routes
    suspend fun listRoutes(token: String, all: Boolean = false): NetworkResult<ApiResponse<List<TransportRoute>>>
    suspend fun createRoute(token: String, request: CreateRouteRequest): NetworkResult<ApiResponse<TransportRoute>>
    suspend fun getRoute(token: String, routeId: String): NetworkResult<ApiResponse<TransportRoute>>
    suspend fun updateRoute(token: String, routeId: String, request: UpdateRouteRequest): NetworkResult<ApiResponse<Unit>>
    suspend fun deleteRoute(token: String, routeId: String): NetworkResult<ApiResponse<Unit>>
    suspend fun addStop(token: String, routeId: String, request: CreateStopRequest): NetworkResult<ApiResponse<TransportStop>>
    suspend fun deleteStop(token: String, stopId: String): NetworkResult<ApiResponse<Unit>>

    // Admin: Vehicles
    suspend fun listVehicles(token: String, all: Boolean = false): NetworkResult<ApiResponse<List<TransportVehicle>>>
    suspend fun createVehicle(token: String, request: CreateVehicleRequest): NetworkResult<ApiResponse<TransportVehicle>>
    suspend fun updateVehicle(token: String, vehicleId: String, request: UpdateVehicleRequest): NetworkResult<ApiResponse<Unit>>
    suspend fun deleteVehicle(token: String, vehicleId: String): NetworkResult<ApiResponse<Unit>>

    // Admin: Assignments
    suspend fun listAssignments(token: String, routeId: String? = null, studentId: String? = null): NetworkResult<ApiResponse<List<TransportAssignment>>>
    suspend fun createAssignment(token: String, request: CreateAssignmentRequest): NetworkResult<ApiResponse<TransportAssignment>>
    suspend fun deactivateAssignment(token: String, assignmentId: String): NetworkResult<ApiResponse<Unit>>

    // Admin: Attendance
    suspend fun getDailyAttendance(token: String, routeId: String, date: String): NetworkResult<ApiResponse<List<TransportAttendance>>>

    // Admin: Transport Fee
    suspend fun createTransportFee(token: String, request: CreateTransportFeeRequest): NetworkResult<ApiResponse<Unit>>

    // Driver
    suspend fun updateLocation(token: String, request: UpdateLocationRequest): NetworkResult<ApiResponse<TransportTracking>>
    suspend fun markPickup(token: String, request: MarkPickupDropRequest): NetworkResult<ApiResponse<TransportAttendance>>
    suspend fun markDrop(token: String, request: MarkPickupDropRequest): NetworkResult<ApiResponse<TransportAttendance>>

    // Parent
    suspend fun getLiveLocation(token: String, childId: String): NetworkResult<ApiResponse<RouteProgress>>
    suspend fun getRouteForChild(token: String, childId: String): NetworkResult<ApiResponse<TransportRoute>>
}
