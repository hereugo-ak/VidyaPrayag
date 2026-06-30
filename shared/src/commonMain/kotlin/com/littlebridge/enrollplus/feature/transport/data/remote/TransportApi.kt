package com.littlebridge.enrollplus.feature.transport.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.transport.domain.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class TransportApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    // ── Admin: Routes ─────────────────────────────────────────────────

    suspend fun listRoutes(token: String, all: Boolean = false): NetworkResult<ApiResponse<List<TransportRoute>>> = safeApiCall {
        client.get(getUrl("api/v1/school/transport/routes")) {
            bearerAuth(token)
            if (all) parameter("all", "true")
        }
    }

    suspend fun createRoute(token: String, request: CreateRouteRequest): NetworkResult<ApiResponse<TransportRoute>> = safeApiCall {
        client.post(getUrl("api/v1/school/transport/routes")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getRoute(token: String, routeId: String): NetworkResult<ApiResponse<TransportRoute>> = safeApiCall {
        client.get(getUrl("api/v1/school/transport/routes/$routeId")) {
            bearerAuth(token)
        }
    }

    suspend fun updateRoute(token: String, routeId: String, request: UpdateRouteRequest): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.put(getUrl("api/v1/school/transport/routes/$routeId")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteRoute(token: String, routeId: String): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.delete(getUrl("api/v1/school/transport/routes/$routeId")) {
            bearerAuth(token)
        }
    }

    suspend fun addStop(token: String, routeId: String, request: CreateStopRequest): NetworkResult<ApiResponse<TransportStop>> = safeApiCall {
        client.post(getUrl("api/v1/school/transport/routes/$routeId/stops")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteStop(token: String, stopId: String): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.delete(getUrl("api/v1/school/transport/stops/$stopId")) {
            bearerAuth(token)
        }
    }

    // ── Admin: Vehicles ───────────────────────────────────────────────

    suspend fun listVehicles(token: String, all: Boolean = false): NetworkResult<ApiResponse<List<TransportVehicle>>> = safeApiCall {
        client.get(getUrl("api/v1/school/transport/vehicles")) {
            bearerAuth(token)
            if (all) parameter("all", "true")
        }
    }

    suspend fun createVehicle(token: String, request: CreateVehicleRequest): NetworkResult<ApiResponse<TransportVehicle>> = safeApiCall {
        client.post(getUrl("api/v1/school/transport/vehicles")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun updateVehicle(token: String, vehicleId: String, request: UpdateVehicleRequest): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.put(getUrl("api/v1/school/transport/vehicles/$vehicleId")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteVehicle(token: String, vehicleId: String): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.delete(getUrl("api/v1/school/transport/vehicles/$vehicleId")) {
            bearerAuth(token)
        }
    }

    // ── Admin: Assignments ────────────────────────────────────────────

    suspend fun listAssignments(token: String, routeId: String? = null, studentId: String? = null): NetworkResult<ApiResponse<List<TransportAssignment>>> = safeApiCall {
        client.get(getUrl("api/v1/school/transport/assignments")) {
            bearerAuth(token)
            routeId?.let { parameter("route_id", it) }
            studentId?.let { parameter("student_id", it) }
        }
    }

    suspend fun createAssignment(token: String, request: CreateAssignmentRequest): NetworkResult<ApiResponse<TransportAssignment>> = safeApiCall {
        client.post(getUrl("api/v1/school/transport/assignments")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deactivateAssignment(token: String, assignmentId: String): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.delete(getUrl("api/v1/school/transport/assignments/$assignmentId")) {
            bearerAuth(token)
        }
    }

    // ── Admin: Attendance ─────────────────────────────────────────────

    suspend fun getDailyAttendance(token: String, routeId: String, date: String): NetworkResult<ApiResponse<List<TransportAttendance>>> = safeApiCall {
        client.get(getUrl("api/v1/school/transport/attendance")) {
            bearerAuth(token)
            parameter("route_id", routeId)
            parameter("date", date)
        }
    }

    // ── Admin: Transport Fee ──────────────────────────────────────────

    suspend fun createTransportFee(token: String, request: CreateTransportFeeRequest): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.post(getUrl("api/v1/school/transport/fees")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // ── Driver: Location + Pickup/Drop ────────────────────────────────

    suspend fun updateLocation(token: String, request: UpdateLocationRequest): NetworkResult<ApiResponse<TransportTracking>> = safeApiCall {
        client.post(getUrl("api/v1/transport/location")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun markPickup(token: String, request: MarkPickupDropRequest): NetworkResult<ApiResponse<TransportAttendance>> = safeApiCall {
        client.post(getUrl("api/v1/transport/pickup")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun markDrop(token: String, request: MarkPickupDropRequest): NetworkResult<ApiResponse<TransportAttendance>> = safeApiCall {
        client.post(getUrl("api/v1/transport/drop")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // ── Parent: Live location + Route ─────────────────────────────────

    suspend fun getLiveLocation(token: String, childId: String): NetworkResult<ApiResponse<RouteProgress>> = safeApiCall {
        client.get(getUrl("api/v1/parent/transport/live-location/$childId")) {
            bearerAuth(token)
        }
    }

    suspend fun getRouteForChild(token: String, childId: String): NetworkResult<ApiResponse<TransportRoute>> = safeApiCall {
        client.get(getUrl("api/v1/parent/transport/route/$childId")) {
            bearerAuth(token)
        }
    }
}
