package com.littlebridge.enrollplus.feature.transport.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TransportRoute(
    val id: String,
    val schoolId: String,
    val name: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val stops: List<TransportStop> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class TransportStop(
    val id: String,
    val routeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val sequence: Int,
    val estimatedTime: String? = null,
    val createdAt: String = ""
)

@Serializable
data class TransportVehicle(
    val id: String,
    val schoolId: String,
    val routeId: String? = null,
    val busNumber: String,
    val capacity: Int = 40,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val driverLicense: String? = null,
    val isActive: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class TransportAssignment(
    val id: String,
    val schoolId: String,
    val studentId: String,
    val studentName: String? = null,
    val routeId: String,
    val routeName: String? = null,
    val stopId: String,
    val stopName: String? = null,
    val vehicleId: String,
    val busNumber: String? = null,
    val isActive: Boolean = true,
    val createdAt: String = ""
)

@Serializable
data class TransportTracking(
    val id: String,
    val vehicleId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Float? = null,
    val heading: Float? = null,
    val recordedAt: String = ""
)

@Serializable
data class TransportAttendance(
    val id: String,
    val schoolId: String,
    val studentId: String,
    val studentName: String? = null,
    val routeId: String,
    val date: String,
    val pickupStatus: String? = null,
    val dropStatus: String? = null,
    val pickupTime: String? = null,
    val dropTime: String? = null
)

@Serializable
data class RouteProgress(
    val routeId: String,
    val routeName: String,
    val vehicleId: String,
    val busNumber: String,
    val currentLocation: TransportTracking? = null,
    val stops: List<TransportStop> = emptyList(),
    val nextStop: TransportStop? = null,
    val etaMinutes: Int? = null
)

// ── Request bodies ───────────────────────────────────────────────────────────

@Serializable
data class CreateRouteRequest(
    val name: String,
    val description: String? = null,
    val stops: List<CreateStopRequest> = emptyList()
)

@Serializable
data class CreateStopRequest(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val sequence: Int,
    val estimatedTime: String? = null
)

@Serializable
data class UpdateRouteRequest(
    val name: String? = null,
    val description: String? = null,
    val isActive: Boolean? = null
)

@Serializable
data class CreateVehicleRequest(
    val busNumber: String,
    val capacity: Int = 40,
    val routeId: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val driverLicense: String? = null
)

@Serializable
data class UpdateVehicleRequest(
    val busNumber: String? = null,
    val capacity: Int? = null,
    val routeId: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val driverLicense: String? = null,
    val isActive: Boolean? = null
)

@Serializable
data class CreateAssignmentRequest(
    val studentId: String,
    val routeId: String,
    val stopId: String,
    val vehicleId: String
)

@Serializable
data class UpdateLocationRequest(
    val vehicleId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Float? = null,
    val heading: Float? = null
)

@Serializable
data class MarkPickupDropRequest(
    val studentId: String,
    val routeId: String,
    val date: String? = null
)

@Serializable
data class CreateTransportFeeRequest(
    val studentId: String,
    val amount: Double,
    val dueDate: String? = null,
    val description: String? = null
)
