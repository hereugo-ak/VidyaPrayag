package com.littlebridge.enrollplus.feature.transport

import com.littlebridge.enrollplus.db.*
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.andWhere
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ── DTOs ─────────────────────────────────────────────────────────────────────

@Serializable
data class TransportRouteDto(
    val id: String,
    val schoolId: String,
    val name: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val stops: List<TransportStopDto> = emptyList(),
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class TransportStopDto(
    val id: String,
    val routeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val sequence: Int,
    val estimatedTime: String? = null,
    val createdAt: String
)

@Serializable
data class TransportVehicleDto(
    val id: String,
    val schoolId: String,
    val routeId: String? = null,
    val busNumber: String,
    val capacity: Int = 40,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val driverLicense: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class TransportAssignmentDto(
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
    val createdAt: String
)

@Serializable
data class TransportTrackingDto(
    val id: String,
    val vehicleId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Float? = null,
    val heading: Float? = null,
    val recordedAt: String
)

@Serializable
data class TransportAttendanceDto(
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
data class RouteProgressDto(
    val routeId: String,
    val routeName: String,
    val vehicleId: String,
    val busNumber: String,
    val currentLocation: TransportTrackingDto? = null,
    val stops: List<TransportStopDto> = emptyList(),
    val nextStop: TransportStopDto? = null,
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

// ── Service ──────────────────────────────────────────────────────────────────

class TransportService {

    // ── Routes ───────────────────────────────────────────────────────────────

    suspend fun listRoutes(schoolId: UUID, activeOnly: Boolean = true): List<TransportRouteDto> = dbQuery {
        val query = TransportRoutesTable.selectAll()
            .where { TransportRoutesTable.schoolId eq schoolId }
        if (activeOnly) {
            query.andWhere { TransportRoutesTable.isActive eq true }
        }
        query.orderBy(TransportRoutesTable.createdAt, SortOrder.DESC)
            .map { rowToRoute(it) }
    }

    suspend fun getRoute(schoolId: UUID, routeId: UUID): TransportRouteDto? = dbQuery {
        val route = TransportRoutesTable.selectAll()
            .where { (TransportRoutesTable.id eq routeId) and (TransportRoutesTable.schoolId eq schoolId) }
            .singleOrNull()
            ?: return@dbQuery null
        val stops = TransportStopsTable.selectAll()
            .where { TransportStopsTable.routeId eq routeId }
            .orderBy(TransportStopsTable.sequence, SortOrder.ASC)
            .map { rowToStop(it) }
        rowToRoute(route).copy(stops = stops)
    }

    suspend fun createRoute(schoolId: UUID, req: CreateRouteRequest): TransportRouteDto = dbQuery {
        val now = Instant.now()
        val routeId = TransportRoutesTable.insert {
            it[TransportRoutesTable.schoolId] = schoolId
            it[name] = req.name
            it[description] = req.description
            it[isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }[TransportRoutesTable.id].value

        val stops = req.stops.sortedBy { it.sequence }.map { stopReq ->
            val stopId = TransportStopsTable.insert {
                it[TransportStopsTable.routeId] = routeId
                it[name] = stopReq.name
                it[latitude] = stopReq.latitude
                it[longitude] = stopReq.longitude
                it[sequence] = stopReq.sequence
                it[estimatedTime] = stopReq.estimatedTime
                it[createdAt] = now
            }[TransportStopsTable.id]
            TransportStopDto(
                id = stopId.toString(),
                routeId = routeId.toString(),
                name = stopReq.name,
                latitude = stopReq.latitude,
                longitude = stopReq.longitude,
                sequence = stopReq.sequence,
                estimatedTime = stopReq.estimatedTime,
                createdAt = now.toString()
            )
        }

        TransportRouteDto(
            id = routeId.toString(),
            schoolId = schoolId.toString(),
            name = req.name,
            description = req.description,
            isActive = true,
            stops = stops,
            createdAt = now.toString(),
            updatedAt = now.toString()
        )
    }

    suspend fun updateRoute(schoolId: UUID, routeId: UUID, req: UpdateRouteRequest): Boolean = dbQuery {
        val now = Instant.now()
        val updated = TransportRoutesTable.update({
            (TransportRoutesTable.id eq routeId) and (TransportRoutesTable.schoolId eq schoolId)
        }) {
            req.name?.let { v -> it[name] = v }
            req.description?.let { v -> it[description] = v }
            req.isActive?.let { v -> it[isActive] = v }
            it[updatedAt] = now
        }
        updated > 0
    }

    suspend fun deleteRoute(schoolId: UUID, routeId: UUID): Boolean = dbQuery {
        val deleted = TransportRoutesTable.deleteWhere {
            (TransportRoutesTable.id eq routeId) and (TransportRoutesTable.schoolId eq schoolId)
        }
        deleted > 0
    }

    // ── Stops ────────────────────────────────────────────────────────────────

    suspend fun addStop(schoolId: UUID, routeId: UUID, req: CreateStopRequest): TransportStopDto? = dbQuery {
        val routeExists = TransportRoutesTable.selectAll()
            .where { (TransportRoutesTable.id eq routeId) and (TransportRoutesTable.schoolId eq schoolId) }
            .any()
        if (!routeExists) return@dbQuery null

        val now = Instant.now()
        val stopId = TransportStopsTable.insert {
            it[TransportStopsTable.routeId] = routeId
            it[name] = req.name
            it[latitude] = req.latitude
            it[longitude] = req.longitude
            it[sequence] = req.sequence
            it[estimatedTime] = req.estimatedTime
            it[createdAt] = now
        }[TransportStopsTable.id]

        rowToStop(TransportStopsTable.selectAll().where { TransportStopsTable.id eq stopId }.single())
    }

    suspend fun deleteStop(schoolId: UUID, stopId: UUID): Boolean = dbQuery {
        val stop = TransportStopsTable.selectAll().where { TransportStopsTable.id eq stopId }.singleOrNull()
            ?: return@dbQuery false
        val routeOwned = TransportRoutesTable.selectAll()
            .where { (TransportRoutesTable.id eq stop[TransportStopsTable.routeId]) and (TransportRoutesTable.schoolId eq schoolId) }
            .any()
        if (!routeOwned) return@dbQuery false
        TransportStopsTable.deleteWhere { TransportStopsTable.id eq stopId } > 0
    }

    // ── Vehicles ─────────────────────────────────────────────────────────────

    suspend fun listVehicles(schoolId: UUID, activeOnly: Boolean = true): List<TransportVehicleDto> = dbQuery {
        val query = TransportVehiclesTable.selectAll()
            .where { TransportVehiclesTable.schoolId eq schoolId }
        if (activeOnly) {
            query.andWhere { TransportVehiclesTable.isActive eq true }
        }
        query.orderBy(TransportVehiclesTable.createdAt, SortOrder.DESC)
            .map { rowToVehicle(it) }
    }

    suspend fun getVehicle(schoolId: UUID, vehicleId: UUID): TransportVehicleDto? = dbQuery {
        TransportVehiclesTable.selectAll()
            .where { (TransportVehiclesTable.id eq vehicleId) and (TransportVehiclesTable.schoolId eq schoolId) }
            .singleOrNull()
            ?.let { rowToVehicle(it) }
    }

    suspend fun createVehicle(schoolId: UUID, req: CreateVehicleRequest): TransportVehicleDto = dbQuery {
        val now = Instant.now()
        val routeUuid = req.routeId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val vehicleId = TransportVehiclesTable.insert {
            it[TransportVehiclesTable.schoolId] = schoolId
            it[busNumber] = req.busNumber
            it[capacity] = req.capacity
            routeUuid?.let { r -> it[routeId] = r }
            it[driverName] = req.driverName
            it[driverPhone] = req.driverPhone
            it[driverLicense] = req.driverLicense
            it[isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }[TransportVehiclesTable.id]
        rowToVehicle(TransportVehiclesTable.selectAll().where { TransportVehiclesTable.id eq vehicleId }.single())
    }

    suspend fun updateVehicle(schoolId: UUID, vehicleId: UUID, req: UpdateVehicleRequest): Boolean = dbQuery {
        val now = Instant.now()
        val routeUuid = req.routeId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val updated = TransportVehiclesTable.update({
            (TransportVehiclesTable.id eq vehicleId) and (TransportVehiclesTable.schoolId eq schoolId)
        }) {
            req.busNumber?.let { v -> it[busNumber] = v }
            req.capacity?.let { v -> it[capacity] = v }
            routeUuid?.let { r -> it[routeId] = r }
            req.driverName?.let { v -> it[driverName] = v }
            req.driverPhone?.let { v -> it[driverPhone] = v }
            req.driverLicense?.let { v -> it[driverLicense] = v }
            req.isActive?.let { v -> it[isActive] = v }
            it[updatedAt] = now
        }
        updated > 0
    }

    suspend fun deleteVehicle(schoolId: UUID, vehicleId: UUID): Boolean = dbQuery {
        TransportVehiclesTable.deleteWhere {
            (TransportVehiclesTable.id eq vehicleId) and (TransportVehiclesTable.schoolId eq schoolId)
        } > 0
    }

    // ── Assignments ──────────────────────────────────────────────────────────

    suspend fun listAssignments(schoolId: UUID, routeId: UUID? = null, studentId: UUID? = null): List<TransportAssignmentDto> = dbQuery {
        val query = TransportAssignmentsTable.selectAll()
            .where { TransportAssignmentsTable.schoolId eq schoolId }
        if (routeId != null) {
            query.andWhere { TransportAssignmentsTable.routeId eq routeId }
        }
        if (studentId != null) {
            query.andWhere { TransportAssignmentsTable.studentId eq studentId }
        }
        query.orderBy(TransportAssignmentsTable.createdAt, SortOrder.DESC).map { row ->
            val studentName = StudentsTable.selectAll()
                .where { StudentsTable.id eq row[TransportAssignmentsTable.studentId] }
                .singleOrNull()?.get(StudentsTable.fullName)
            val routeName = TransportRoutesTable.selectAll()
                .where { TransportRoutesTable.id eq row[TransportAssignmentsTable.routeId] }
                .singleOrNull()?.get(TransportRoutesTable.name)
            val stopName = TransportStopsTable.selectAll()
                .where { TransportStopsTable.id eq row[TransportAssignmentsTable.stopId] }
                .singleOrNull()?.get(TransportStopsTable.name)
            val busNumber = TransportVehiclesTable.selectAll()
                .where { TransportVehiclesTable.id eq row[TransportAssignmentsTable.vehicleId] }
                .singleOrNull()?.get(TransportVehiclesTable.busNumber)
            rowToAssignment(row).copy(
                studentName = studentName,
                routeName = routeName,
                stopName = stopName,
                busNumber = busNumber
            )
        }
    }

    suspend fun createAssignment(schoolId: UUID, req: CreateAssignmentRequest): TransportAssignmentDto? = dbQuery {
        val studentId = runCatching { UUID.fromString(req.studentId) }.getOrNull() ?: return@dbQuery null
        val routeId = runCatching { UUID.fromString(req.routeId) }.getOrNull() ?: return@dbQuery null
        val stopId = runCatching { UUID.fromString(req.stopId) }.getOrNull() ?: return@dbQuery null
        val vehicleId = runCatching { UUID.fromString(req.vehicleId) }.getOrNull() ?: return@dbQuery null

        val now = Instant.now()
        val assignmentId = TransportAssignmentsTable.insert {
            it[TransportAssignmentsTable.schoolId] = schoolId
            it[TransportAssignmentsTable.studentId] = studentId
            it[TransportAssignmentsTable.routeId] = routeId
            it[TransportAssignmentsTable.stopId] = stopId
            it[TransportAssignmentsTable.vehicleId] = vehicleId
            it[isActive] = true
            it[createdAt] = now
        }[TransportAssignmentsTable.id]

        rowToAssignment(TransportAssignmentsTable.selectAll().where { TransportAssignmentsTable.id eq assignmentId }.single())
    }

    suspend fun deactivateAssignment(schoolId: UUID, assignmentId: UUID): Boolean = dbQuery {
        TransportAssignmentsTable.update({
            (TransportAssignmentsTable.id eq assignmentId) and (TransportAssignmentsTable.schoolId eq schoolId)
        }) {
            it[isActive] = false
        } > 0
    }

    // ── Tracking ─────────────────────────────────────────────────────────────

    suspend fun updateLocation(req: UpdateLocationRequest): TransportTrackingDto? {
        val vehicleId = runCatching { UUID.fromString(req.vehicleId) }.getOrNull() ?: return null
        val now = Instant.now()
        val trackingDto = dbQuery {
            val trackingId = TransportTrackingTable.insert {
                it[TransportTrackingTable.vehicleId] = vehicleId
                it[latitude] = req.latitude
                it[longitude] = req.longitude
                it[speed] = req.speed
                it[heading] = req.heading
                it[recordedAt] = now
            }[TransportTrackingTable.id]
            rowToTracking(TransportTrackingTable.selectAll().where { TransportTrackingTable.id eq trackingId }.single())
        }

        // ── Geofence notification dispatch ────────────────────────────────
        // After storing the GPS ping, check if the bus is within 500m of any
        // stop on its route. If so, notify all parents of students assigned
        // to that stop via the Notify spine (in-app + FCM push).
        // Run OUTSIDE the dbQuery transaction to avoid nested-transaction deadlocks.
        checkGeofenceAndNotify(vehicleId, req.latitude, req.longitude)

        return trackingDto
    }

    /**
     * Geofence check: if the bus is within [GEOFENCE_RADIUS_M] of any stop,
     * fire a push notification to parents of students assigned to that stop.
     * Fire-and-forget — never blocks the location update response.
     */
    private suspend fun checkGeofenceAndNotify(vehicleId: UUID, lat: Double, lng: Double) {
        try {
            // Resolve the vehicle's route + school
            val vehicle = dbQuery {
                TransportVehiclesTable.selectAll()
                    .where { TransportVehiclesTable.id eq vehicleId }
                    .singleOrNull()
            } ?: return
            val routeId = vehicle[TransportVehiclesTable.routeId] ?: return
            val schoolId = vehicle[TransportVehiclesTable.schoolId]

            // Find the nearest stop within geofence radius
            val stops = dbQuery {
                TransportStopsTable.selectAll()
                    .where { TransportStopsTable.routeId eq routeId }
                    .orderBy(TransportStopsTable.sequence, SortOrder.ASC)
                    .toList()
            }
            val nearestStop = stops.minByOrNull { stop ->
                haversineMeters(lat, lng, stop[TransportStopsTable.latitude], stop[TransportStopsTable.longitude])
            } ?: return
            val distance = haversineMeters(lat, lng, nearestStop[TransportStopsTable.latitude], nearestStop[TransportStopsTable.longitude])
            if (distance > GEOFENCE_RADIUS_M) return

            val stopId = nearestStop[TransportStopsTable.id].value
            val stopName = nearestStop[TransportStopsTable.name]

            // Find all active assignments for this stop → student IDs
            val assignments = dbQuery {
                TransportAssignmentsTable.selectAll()
                    .where {
                        (TransportAssignmentsTable.stopId eq stopId) and
                        (TransportAssignmentsTable.isActive eq true)
                    }
                    .toList()
            }
            if (assignments.isEmpty()) return

            // Resolve student codes → parent user IDs via ChildrenTable
            val studentIds = assignments.map { it[TransportAssignmentsTable.studentId] }
            val studentCodes = dbQuery {
                StudentsTable.selectAll()
                    .where { StudentsTable.id inList studentIds.map { org.jetbrains.exposed.dao.id.EntityID(it, StudentsTable) } }
                    .associate { it[StudentsTable.id].value to it[StudentsTable.studentCode] }
            }
            val codes = studentCodes.values.filterNotNull()
            if (codes.isEmpty()) return

            val parentIds = dbQuery {
                ChildrenTable.selectAll()
                    .where {
                        (ChildrenTable.studentCode inList codes) and
                        (ChildrenTable.isActive eq true)
                    }
                    .map { it[ChildrenTable.parentId] }
                    .distinct()
            }
            if (parentIds.isEmpty()) return

            // Dispatch notification via the Notify spine (in-app + FCM push)
            com.littlebridge.enrollplus.feature.notifications.Notify.toUsers(
                userIds = parentIds,
                category = "transport",
                title = "Bus approaching: $stopName",
                body = "The bus is ${distance.toInt()}m away from $stopName. Please be ready for pickup/drop.",
                schoolId = schoolId,
                deepLink = "/parent/transport/live-location",
                refType = "transport_stop",
                refId = stopId.toString(),
            )
        } catch (e: Exception) {
            // Geofence dispatch is best-effort — never crash the location update.
            println("TRANSPORT: geofence notification failed: ${e.message}")
        }
    }

    suspend fun getLiveLocation(vehicleId: UUID): TransportTrackingDto? = dbQuery {
        TransportTrackingTable.selectAll()
            .where { TransportTrackingTable.vehicleId eq vehicleId }
            .orderBy(TransportTrackingTable.recordedAt, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.let { rowToTracking(it) }
    }

    suspend fun getRouteProgress(schoolId: UUID, routeId: UUID): RouteProgressDto? = dbQuery {
        val route = TransportRoutesTable.selectAll()
            .where { (TransportRoutesTable.id eq routeId) and (TransportRoutesTable.schoolId eq schoolId) }
            .singleOrNull() ?: return@dbQuery null

        val vehicle = TransportVehiclesTable.selectAll()
            .where { (TransportVehiclesTable.routeId eq routeId) and (TransportVehiclesTable.isActive eq true) }
            .singleOrNull()

        val stops = TransportStopsTable.selectAll()
            .where { TransportStopsTable.routeId eq routeId }
            .orderBy(TransportStopsTable.sequence, SortOrder.ASC)
            .map { rowToStop(it) }

        val currentLocation = vehicle?.let { v ->
            TransportTrackingTable.selectAll()
                .where { TransportTrackingTable.vehicleId eq v[TransportVehiclesTable.id].value }
                .orderBy(TransportTrackingTable.recordedAt, SortOrder.DESC)
                .limit(1)
                .singleOrNull()
                ?.let { rowToTracking(it) }
        }

        val nextStop = computeNextStop(stops, currentLocation)

        RouteProgressDto(
            routeId = routeId.toString(),
            routeName = route[TransportRoutesTable.name],
            vehicleId = vehicle?.get(TransportVehiclesTable.id)?.toString() ?: "",
            busNumber = vehicle?.get(TransportVehiclesTable.busNumber) ?: "",
            currentLocation = currentLocation,
            stops = stops,
            nextStop = nextStop,
            etaMinutes = nextStop?.let { computeEtaMinutes(currentLocation, it) }
        )
    }

    // ── Attendance ───────────────────────────────────────────────────────────

    suspend fun markPickup(schoolId: UUID, studentId: UUID, routeId: UUID, date: LocalDate): TransportAttendanceDto? = dbQuery {
        val now = Instant.now()
        val existing = TransportAttendanceTable.selectAll()
            .where {
                (TransportAttendanceTable.schoolId eq schoolId) and
                (TransportAttendanceTable.studentId eq studentId) and
                (TransportAttendanceTable.date eq date)
            }.singleOrNull()

        if (existing == null) {
            val id = TransportAttendanceTable.insert {
                it[TransportAttendanceTable.schoolId] = schoolId
                it[TransportAttendanceTable.studentId] = studentId
                it[TransportAttendanceTable.routeId] = routeId
                it[TransportAttendanceTable.date] = date
                it[pickupStatus] = "picked"
                it[pickupTime] = now
                it[createdAt] = now
            }[TransportAttendanceTable.id]
            rowToAttendance(TransportAttendanceTable.selectAll().where { TransportAttendanceTable.id eq id }.single())
        } else {
            TransportAttendanceTable.update({
                (TransportAttendanceTable.schoolId eq schoolId) and
                (TransportAttendanceTable.studentId eq studentId) and
                (TransportAttendanceTable.date eq date)
            }) {
                it[pickupStatus] = "picked"
                it[pickupTime] = now
            }
            rowToAttendance(TransportAttendanceTable.selectAll()
                .where { TransportAttendanceTable.id eq existing[TransportAttendanceTable.id] }.single())
        }
    }

    suspend fun markDrop(schoolId: UUID, studentId: UUID, routeId: UUID, date: LocalDate): TransportAttendanceDto? = dbQuery {
        val now = Instant.now()
        val existing = TransportAttendanceTable.selectAll()
            .where {
                (TransportAttendanceTable.schoolId eq schoolId) and
                (TransportAttendanceTable.studentId eq studentId) and
                (TransportAttendanceTable.date eq date)
            }.singleOrNull()

        if (existing == null) {
            val id = TransportAttendanceTable.insert {
                it[TransportAttendanceTable.schoolId] = schoolId
                it[TransportAttendanceTable.studentId] = studentId
                it[TransportAttendanceTable.routeId] = routeId
                it[TransportAttendanceTable.date] = date
                it[dropStatus] = "dropped"
                it[dropTime] = now
                it[createdAt] = now
            }[TransportAttendanceTable.id]
            rowToAttendance(TransportAttendanceTable.selectAll().where { TransportAttendanceTable.id eq id }.single())
        } else {
            TransportAttendanceTable.update({
                (TransportAttendanceTable.schoolId eq schoolId) and
                (TransportAttendanceTable.studentId eq studentId) and
                (TransportAttendanceTable.date eq date)
            }) {
                it[dropStatus] = "dropped"
                it[dropTime] = now
            }
            rowToAttendance(TransportAttendanceTable.selectAll()
                .where { TransportAttendanceTable.id eq existing[TransportAttendanceTable.id] }.single())
        }
    }

    suspend fun getDailyAttendance(schoolId: UUID, routeId: UUID, date: LocalDate): List<TransportAttendanceDto> = dbQuery {
        TransportAttendanceTable.selectAll()
            .where {
                (TransportAttendanceTable.schoolId eq schoolId) and
                (TransportAttendanceTable.routeId eq routeId) and
                (TransportAttendanceTable.date eq date)
            }
            .orderBy(TransportAttendanceTable.studentId, SortOrder.ASC)
            .map { row ->
                val studentName = StudentsTable.selectAll()
                    .where { StudentsTable.id eq row[TransportAttendanceTable.studentId] }
                    .singleOrNull()?.get(StudentsTable.fullName)
                rowToAttendance(row).copy(studentName = studentName)
            }
    }

    // ── Transport Fee ────────────────────────────────────────────────────────

    suspend fun createTransportFee(schoolId: UUID, req: CreateTransportFeeRequest): Boolean = dbQuery {
        val studentId = runCatching { UUID.fromString(req.studentId) }.getOrNull() ?: return@dbQuery false
        val now = Instant.now()
        val student = StudentsTable.selectAll()
            .where { (StudentsTable.id eq studentId) and (StudentsTable.schoolId eq schoolId) }
            .singleOrNull() ?: return@dbQuery false

        // Resolve parent UUID via ChildrenTable (studentCode links child → parent).
        val studentCode = student[StudentsTable.studentCode] ?: return@dbQuery false
        val parentId = ChildrenTable.selectAll()
            .where {
                (ChildrenTable.studentCode eq studentCode) and (ChildrenTable.isActive eq true)
            }
            .map { it[ChildrenTable.parentId] }
            .firstOrNull() ?: return@dbQuery false

        FeeRecordsTable.insert {
            it[FeeRecordsTable.schoolId] = schoolId
            it[FeeRecordsTable.childId] = studentId
            it[FeeRecordsTable.parentId] = parentId
            it[title] = "Transport Fee"
            it[description] = req.description ?: "Transport fee for ${student[StudentsTable.fullName]}"
            it[amount] = req.amount
            it[currency] = "INR"
            it[dueDate] = req.dueDate
            it[status] = "DUE"
            it[category] = "Transport"
            it[createdAt] = now
            it[updatedAt] = now
        }
        true
    }

    // ── Parent: live location for child ──────────────────────────────────────

    suspend fun getLiveLocationForChild(parentId: UUID, childId: UUID): RouteProgressDto? = dbQuery {
        val child = ChildrenTable.selectAll()
            .where { (ChildrenTable.id eq childId) and (ChildrenTable.parentId eq parentId) }
            .singleOrNull() ?: return@dbQuery null

        val studentCode = child[ChildrenTable.studentCode] ?: return@dbQuery null
        val student = StudentsTable.selectAll()
            .where { StudentsTable.studentCode eq studentCode }
            .singleOrNull() ?: return@dbQuery null
        val studentUuid = student[StudentsTable.id].value

        val assignment = TransportAssignmentsTable.selectAll()
            .where {
                (TransportAssignmentsTable.studentId eq studentUuid) and
                (TransportAssignmentsTable.isActive eq true)
            }.singleOrNull() ?: return@dbQuery null

        getRouteProgress(
            schoolId = assignment[TransportAssignmentsTable.schoolId],
            routeId = assignment[TransportAssignmentsTable.routeId]
        )
    }

    suspend fun getRouteForChild(parentId: UUID, childId: UUID): TransportRouteDto? = dbQuery {
        val child = ChildrenTable.selectAll()
            .where { (ChildrenTable.id eq childId) and (ChildrenTable.parentId eq parentId) }
            .singleOrNull() ?: return@dbQuery null

        val studentCode = child[ChildrenTable.studentCode] ?: return@dbQuery null
        val student = StudentsTable.selectAll()
            .where { StudentsTable.studentCode eq studentCode }
            .singleOrNull() ?: return@dbQuery null
        val studentUuid = student[StudentsTable.id].value

        val assignment = TransportAssignmentsTable.selectAll()
            .where {
                (TransportAssignmentsTable.studentId eq studentUuid) and
                (TransportAssignmentsTable.isActive eq true)
            }.singleOrNull() ?: return@dbQuery null

        getRoute(assignment[TransportAssignmentsTable.schoolId], assignment[TransportAssignmentsTable.routeId])
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun rowToRoute(row: org.jetbrains.exposed.sql.ResultRow): TransportRouteDto =
        TransportRouteDto(
            id = row[TransportRoutesTable.id].toString(),
            schoolId = row[TransportRoutesTable.schoolId].toString(),
            name = row[TransportRoutesTable.name],
            description = row[TransportRoutesTable.description],
            isActive = row[TransportRoutesTable.isActive],
            createdAt = row[TransportRoutesTable.createdAt].toString(),
            updatedAt = row[TransportRoutesTable.updatedAt].toString()
        )

    private fun rowToStop(row: org.jetbrains.exposed.sql.ResultRow): TransportStopDto =
        TransportStopDto(
            id = row[TransportStopsTable.id].toString(),
            routeId = row[TransportStopsTable.routeId].toString(),
            name = row[TransportStopsTable.name],
            latitude = row[TransportStopsTable.latitude],
            longitude = row[TransportStopsTable.longitude],
            sequence = row[TransportStopsTable.sequence],
            estimatedTime = row[TransportStopsTable.estimatedTime],
            createdAt = row[TransportStopsTable.createdAt].toString()
        )

    private fun rowToVehicle(row: org.jetbrains.exposed.sql.ResultRow): TransportVehicleDto =
        TransportVehicleDto(
            id = row[TransportVehiclesTable.id].toString(),
            schoolId = row[TransportVehiclesTable.schoolId].toString(),
            routeId = row[TransportVehiclesTable.routeId]?.toString(),
            busNumber = row[TransportVehiclesTable.busNumber],
            capacity = row[TransportVehiclesTable.capacity],
            driverName = row[TransportVehiclesTable.driverName],
            driverPhone = row[TransportVehiclesTable.driverPhone],
            driverLicense = row[TransportVehiclesTable.driverLicense],
            isActive = row[TransportVehiclesTable.isActive],
            createdAt = row[TransportVehiclesTable.createdAt].toString(),
            updatedAt = row[TransportVehiclesTable.updatedAt].toString()
        )

    private fun rowToAssignment(row: org.jetbrains.exposed.sql.ResultRow): TransportAssignmentDto =
        TransportAssignmentDto(
            id = row[TransportAssignmentsTable.id].toString(),
            schoolId = row[TransportAssignmentsTable.schoolId].toString(),
            studentId = row[TransportAssignmentsTable.studentId].toString(),
            routeId = row[TransportAssignmentsTable.routeId].toString(),
            stopId = row[TransportAssignmentsTable.stopId].toString(),
            vehicleId = row[TransportAssignmentsTable.vehicleId].toString(),
            isActive = row[TransportAssignmentsTable.isActive],
            createdAt = row[TransportAssignmentsTable.createdAt].toString()
        )

    private fun rowToTracking(row: org.jetbrains.exposed.sql.ResultRow): TransportTrackingDto =
        TransportTrackingDto(
            id = row[TransportTrackingTable.id].toString(),
            vehicleId = row[TransportTrackingTable.vehicleId].toString(),
            latitude = row[TransportTrackingTable.latitude],
            longitude = row[TransportTrackingTable.longitude],
            speed = row[TransportTrackingTable.speed],
            heading = row[TransportTrackingTable.heading],
            recordedAt = row[TransportTrackingTable.recordedAt].toString()
        )

    private fun rowToAttendance(row: org.jetbrains.exposed.sql.ResultRow): TransportAttendanceDto =
        TransportAttendanceDto(
            id = row[TransportAttendanceTable.id].toString(),
            schoolId = row[TransportAttendanceTable.schoolId].toString(),
            studentId = row[TransportAttendanceTable.studentId].toString(),
            routeId = row[TransportAttendanceTable.routeId].toString(),
            date = row[TransportAttendanceTable.date].toString(),
            pickupStatus = row[TransportAttendanceTable.pickupStatus],
            dropStatus = row[TransportAttendanceTable.dropStatus],
            pickupTime = row[TransportAttendanceTable.pickupTime]?.toString(),
            dropTime = row[TransportAttendanceTable.dropTime]?.toString()
        )

    private fun computeNextStop(stops: List<TransportStopDto>, currentLocation: TransportTrackingDto?): TransportStopDto? {
        if (stops.isEmpty() || currentLocation == null) return stops.firstOrNull()
        val nearestStop = stops.minByOrNull { stop ->
            haversineMeters(currentLocation.latitude, currentLocation.longitude, stop.latitude, stop.longitude)
        }
        return nearestStop
    }

    private fun computeEtaMinutes(currentLocation: TransportTrackingDto?, stop: TransportStopDto): Int? {
        if (currentLocation == null) return null
        val distanceM = haversineMeters(currentLocation.latitude, currentLocation.longitude, stop.latitude, stop.longitude)
        val speedMs = (currentLocation.speed ?: 30f) / 3.6  // km/h → m/s, default 30 km/h
        if (speedMs <= 0f) return null
        return (distanceM / speedMs / 60).toInt().coerceAtLeast(1)
    }

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    companion object {
        private const val GEOFENCE_RADIUS_M = 500.0
    }
}
