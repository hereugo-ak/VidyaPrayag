/*
 * File: TransportRouting.kt
 * Module: feature.transport
 *
 * API endpoints for the Transport Tracking feature (TRANSPORT_TRACKING_SPEC.md §6).
 *
 *   Admin (school-scoped, JWT + requireSchoolContext):
 *     GET    /api/v1/school/transport/routes                   — list routes
 *     POST   /api/v1/school/transport/routes                   — create route + stops
 *     GET    /api/v1/school/transport/routes/{routeId}         — route detail with stops
 *     PUT    /api/v1/school/transport/routes/{routeId}         — update route
 *     DELETE /api/v1/school/transport/routes/{routeId}         — delete route (cascade)
 *     POST   /api/v1/school/transport/routes/{routeId}/stops   — add stop to route
 *     DELETE /api/v1/school/transport/stops/{stopId}           — delete stop
 *     GET    /api/v1/school/transport/vehicles                 — list vehicles
 *     POST   /api/v1/school/transport/vehicles                 — create vehicle
 *     PUT    /api/v1/school/transport/vehicles/{vehicleId}     — update vehicle
 *     DELETE /api/v1/school/transport/vehicles/{vehicleId}     — delete vehicle
 *     GET    /api/v1/school/transport/assignments              — list assignments
 *     POST   /api/v1/school/transport/assignments              — assign student to route
 *     DELETE /api/v1/school/transport/assignments/{assignmentId} — deactivate assignment
 *     GET    /api/v1/school/transport/attendance?route_id=&date= — daily attendance
 *     POST   /api/v1/school/transport/fees                     — create transport fee record
 *
 *   Driver (JWT):
 *     POST   /api/v1/transport/location                        — update GPS location
 *     POST   /api/v1/transport/pickup                          — mark pickup
 *     POST   /api/v1/transport/drop                            — mark drop
 *
 *   Parent (JWT):
 *     GET    /api/v1/parent/transport/live-location/{childId}  — live bus location + route progress
 *     GET    /api/v1/parent/transport/route/{childId}          — child's route + stops
 */
package com.littlebridge.enrollplus.feature.transport

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.okMessage
import com.littlebridge.enrollplus.core.principalUserId
import com.littlebridge.enrollplus.core.requireSchoolContext
import com.littlebridge.enrollplus.core.resolveSchoolIdForUser
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import java.time.LocalDate
import java.util.UUID

fun Route.transportRouting() {
    authenticate("jwt") {

        // ── Admin: Routes ─────────────────────────────────────────────────
        route("/api/v1/school/transport/routes") {

            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val activeOnly = call.request.queryParameters["all"]?.toBooleanStrictOrNull() != true
                val routes = TransportService().listRoutes(ctx.schoolId, activeOnly)
                call.ok(routes, "Routes (${routes.size})")
            }

            post {
                val ctx = call.requireSchoolContext() ?: return@post
                val req = runCatching { call.receive<CreateRouteRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                if (req.name.isBlank()) {
                    call.fail("Route name is required"); return@post
                }
                val route = TransportService().createRoute(ctx.schoolId, req)
                call.ok(route, "Route created", HttpStatusCode.Created)
            }

            get("/{routeId}") {
                val ctx = call.requireSchoolContext() ?: return@get
                val routeId = call.parameters["routeId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid route id"); return@get }
                val route = TransportService().getRoute(ctx.schoolId, routeId)
                if (route == null) {
                    call.fail("Route not found", HttpStatusCode.NotFound, "NOT_FOUND")
                } else {
                    call.ok(route, "Route detail")
                }
            }

            put("/{routeId}") {
                val ctx = call.requireSchoolContext() ?: return@put
                val routeId = call.parameters["routeId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid route id"); return@put }
                val req = runCatching { call.receive<UpdateRouteRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@put }
                val updated = TransportService().updateRoute(ctx.schoolId, routeId, req)
                if (updated) call.okMessage("Route updated") else call.fail("Route not found", HttpStatusCode.NotFound)
            }

            delete("/{routeId}") {
                val ctx = call.requireSchoolContext() ?: return@delete
                val routeId = call.parameters["routeId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid route id"); return@delete }
                val deleted = TransportService().deleteRoute(ctx.schoolId, routeId)
                if (deleted) call.okMessage("Route deleted") else call.fail("Route not found", HttpStatusCode.NotFound)
            }

            // Add stop to route
            post("/{routeId}/stops") {
                val ctx = call.requireSchoolContext() ?: return@post
                val routeId = call.parameters["routeId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid route id"); return@post }
                val req = runCatching { call.receive<CreateStopRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                val stop = TransportService().addStop(ctx.schoolId, routeId, req)
                if (stop == null) {
                    call.fail("Route not found", HttpStatusCode.NotFound, "NOT_FOUND")
                } else {
                    call.ok(stop, "Stop added", HttpStatusCode.Created)
                }
            }
        }

        // ── Admin: Stops (delete) ─────────────────────────────────────────
        delete("/api/v1/school/transport/stops/{stopId}") {
            val ctx = call.requireSchoolContext() ?: return@delete
            val stopId = call.parameters["stopId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: run { call.fail("Invalid stop id"); return@delete }
            val deleted = TransportService().deleteStop(ctx.schoolId, stopId)
            if (deleted) call.okMessage("Stop deleted") else call.fail("Stop not found", HttpStatusCode.NotFound)
        }

        // ── Admin: Vehicles ───────────────────────────────────────────────
        route("/api/v1/school/transport/vehicles") {

            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val activeOnly = call.request.queryParameters["all"]?.toBooleanStrictOrNull() != true
                val vehicles = TransportService().listVehicles(ctx.schoolId, activeOnly)
                call.ok(vehicles, "Vehicles (${vehicles.size})")
            }

            post {
                val ctx = call.requireSchoolContext() ?: return@post
                val req = runCatching { call.receive<CreateVehicleRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                if (req.busNumber.isBlank()) {
                    call.fail("Bus number is required"); return@post
                }
                val vehicle = TransportService().createVehicle(ctx.schoolId, req)
                call.ok(vehicle, "Vehicle created", HttpStatusCode.Created)
            }

            put("/{vehicleId}") {
                val ctx = call.requireSchoolContext() ?: return@put
                val vehicleId = call.parameters["vehicleId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid vehicle id"); return@put }
                val req = runCatching { call.receive<UpdateVehicleRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@put }
                val updated = TransportService().updateVehicle(ctx.schoolId, vehicleId, req)
                if (updated) call.okMessage("Vehicle updated") else call.fail("Vehicle not found", HttpStatusCode.NotFound)
            }

            delete("/{vehicleId}") {
                val ctx = call.requireSchoolContext() ?: return@delete
                val vehicleId = call.parameters["vehicleId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid vehicle id"); return@delete }
                val deleted = TransportService().deleteVehicle(ctx.schoolId, vehicleId)
                if (deleted) call.okMessage("Vehicle deleted") else call.fail("Vehicle not found", HttpStatusCode.NotFound)
            }
        }

        // ── Admin: Assignments ────────────────────────────────────────────
        route("/api/v1/school/transport/assignments") {

            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val routeId = call.request.queryParameters["route_id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                val studentId = call.request.queryParameters["student_id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                val assignments = TransportService().listAssignments(ctx.schoolId, routeId, studentId)
                call.ok(assignments, "Assignments (${assignments.size})")
            }

            post {
                val ctx = call.requireSchoolContext() ?: return@post
                val req = runCatching { call.receive<CreateAssignmentRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                val assignment = TransportService().createAssignment(ctx.schoolId, req)
                if (assignment == null) {
                    call.fail("Invalid student/route/stop/vehicle id", HttpStatusCode.BadRequest)
                } else {
                    call.ok(assignment, "Assignment created", HttpStatusCode.Created)
                }
            }

            delete("/{assignmentId}") {
                val ctx = call.requireSchoolContext() ?: return@delete
                val assignmentId = call.parameters["assignmentId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid assignment id"); return@delete }
                val deactivated = TransportService().deactivateAssignment(ctx.schoolId, assignmentId)
                if (deactivated) call.okMessage("Assignment deactivated") else call.fail("Assignment not found", HttpStatusCode.NotFound)
            }
        }

        // ── Admin: Attendance ─────────────────────────────────────────────
        get("/api/v1/school/transport/attendance") {
            val ctx = call.requireSchoolContext() ?: return@get
            val routeId = call.request.queryParameters["route_id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: run { call.fail("route_id query parameter is required"); return@get }
            val dateStr = call.request.queryParameters["date"] ?: LocalDate.now().toString()
            val date = runCatching { LocalDate.parse(dateStr) }.getOrNull()
                ?: run { call.fail("Invalid date format (use YYYY-MM-DD)"); return@get }
            val attendance = TransportService().getDailyAttendance(ctx.schoolId, routeId, date)
            call.ok(attendance, "Attendance (${attendance.size})")
        }

        // ── Admin: Transport Fee ──────────────────────────────────────────
        post("/api/v1/school/transport/fees") {
            val ctx = call.requireSchoolContext() ?: return@post
            val req = runCatching { call.receive<CreateTransportFeeRequest>() }.getOrNull()
                ?: run { call.fail("Invalid request body"); return@post }
            val created = TransportService().createTransportFee(ctx.schoolId, req)
            if (created) call.okMessage("Transport fee created") else call.fail("Failed to create transport fee")
        }

        // ── Driver: Location + Pickup/Drop ────────────────────────────────
        route("/api/v1/transport") {

            post("/location") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post
                }
                val req = runCatching { call.receive<UpdateLocationRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                val tracking = TransportService().updateLocation(req)
                if (tracking == null) {
                    call.fail("Invalid vehicle id", HttpStatusCode.BadRequest)
                } else {
                    call.ok(tracking, "Location updated")
                }
            }

            post("/pickup") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post
                }
                val req = runCatching { call.receive<MarkPickupDropRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                val studentId = runCatching { UUID.fromString(req.studentId) }.getOrNull()
                    ?: run { call.fail("Invalid student id"); return@post }
                val routeId = runCatching { UUID.fromString(req.routeId) }.getOrNull()
                    ?: run { call.fail("Invalid route id"); return@post }
                val date = req.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
                val schoolId = resolveSchoolIdForUser(uid) ?: run {
                    call.fail("No school associated with this user", HttpStatusCode.Forbidden); return@post
                }
                val attendance = TransportService().markPickup(
                    schoolId = schoolId,
                    studentId = studentId,
                    routeId = routeId,
                    date = date
                )
                if (attendance == null) {
                    call.fail("Failed to mark pickup", HttpStatusCode.InternalServerError)
                } else {
                    call.ok(attendance, "Pickup marked")
                }
            }

            post("/drop") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post
                }
                val req = runCatching { call.receive<MarkPickupDropRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                val studentId = runCatching { UUID.fromString(req.studentId) }.getOrNull()
                    ?: run { call.fail("Invalid student id"); return@post }
                val routeId = runCatching { UUID.fromString(req.routeId) }.getOrNull()
                    ?: run { call.fail("Invalid route id"); return@post }
                val date = req.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
                val schoolId = resolveSchoolIdForUser(uid) ?: run {
                    call.fail("No school associated with this user", HttpStatusCode.Forbidden); return@post
                }
                val attendance = TransportService().markDrop(
                    schoolId = schoolId,
                    studentId = studentId,
                    routeId = routeId,
                    date = date
                )
                if (attendance == null) {
                    call.fail("Failed to mark drop", HttpStatusCode.InternalServerError)
                } else {
                    call.ok(attendance, "Drop marked")
                }
            }
        }

        // ── Parent: Live location + Route ─────────────────────────────────
        route("/api/v1/parent/transport") {

            get("/live-location/{childId}") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }
                val childId = call.parameters["childId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid child id"); return@get }
                val progress = TransportService().getLiveLocationForChild(uid, childId)
                if (progress == null) {
                    call.fail("No transport assignment found for this child", HttpStatusCode.NotFound, "NO_ASSIGNMENT")
                } else {
                    call.ok(progress, "Live location")
                }
            }

            get("/route/{childId}") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }
                val childId = call.parameters["childId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid child id"); return@get }
                val route = TransportService().getRouteForChild(uid, childId)
                if (route == null) {
                    call.fail("No transport assignment found for this child", HttpStatusCode.NotFound, "NO_ASSIGNMENT")
                } else {
                    call.ok(route, "Route detail")
                }
            }
        }
    }
}
