package com.littlebridge.enrollplus.feature.transport.data.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.transport.data.remote.TransportApi
import com.littlebridge.enrollplus.feature.transport.domain.model.*
import com.littlebridge.enrollplus.feature.transport.domain.repository.TransportRepository

class TransportRepositoryImpl(
    private val api: TransportApi,
) : TransportRepository {

    override suspend fun listRoutes(token: String, all: Boolean) = api.listRoutes(token, all)
    override suspend fun createRoute(token: String, request: CreateRouteRequest) = api.createRoute(token, request)
    override suspend fun getRoute(token: String, routeId: String) = api.getRoute(token, routeId)
    override suspend fun updateRoute(token: String, routeId: String, request: UpdateRouteRequest) = api.updateRoute(token, routeId, request)
    override suspend fun deleteRoute(token: String, routeId: String) = api.deleteRoute(token, routeId)
    override suspend fun addStop(token: String, routeId: String, request: CreateStopRequest) = api.addStop(token, routeId, request)
    override suspend fun deleteStop(token: String, stopId: String) = api.deleteStop(token, stopId)

    override suspend fun listVehicles(token: String, all: Boolean) = api.listVehicles(token, all)
    override suspend fun createVehicle(token: String, request: CreateVehicleRequest) = api.createVehicle(token, request)
    override suspend fun updateVehicle(token: String, vehicleId: String, request: UpdateVehicleRequest) = api.updateVehicle(token, vehicleId, request)
    override suspend fun deleteVehicle(token: String, vehicleId: String) = api.deleteVehicle(token, vehicleId)

    override suspend fun listAssignments(token: String, routeId: String?, studentId: String?) = api.listAssignments(token, routeId, studentId)
    override suspend fun createAssignment(token: String, request: CreateAssignmentRequest) = api.createAssignment(token, request)
    override suspend fun deactivateAssignment(token: String, assignmentId: String) = api.deactivateAssignment(token, assignmentId)

    override suspend fun getDailyAttendance(token: String, routeId: String, date: String) = api.getDailyAttendance(token, routeId, date)
    override suspend fun createTransportFee(token: String, request: CreateTransportFeeRequest) = api.createTransportFee(token, request)

    override suspend fun updateLocation(token: String, request: UpdateLocationRequest) = api.updateLocation(token, request)
    override suspend fun markPickup(token: String, request: MarkPickupDropRequest) = api.markPickup(token, request)
    override suspend fun markDrop(token: String, request: MarkPickupDropRequest) = api.markDrop(token, request)

    override suspend fun getLiveLocation(token: String, childId: String) = api.getLiveLocation(token, childId)
    override suspend fun getRouteForChild(token: String, childId: String) = api.getRouteForChild(token, childId)
}
