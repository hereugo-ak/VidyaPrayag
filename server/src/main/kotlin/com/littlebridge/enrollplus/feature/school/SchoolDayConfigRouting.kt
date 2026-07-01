package com.littlebridge.enrollplus.feature.school

import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.okMessage
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.core.requireSchoolContext
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.SchoolDayConfigTable
import com.littlebridge.enrollplus.db.SchoolDaySlotType
import com.littlebridge.enrollplus.db.SchoolDaySlotsTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID as JUUID

private val SDC_HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Serializable
data class SchoolDaySlotDto(
    @SerialName("slot_index") val slotIndex: Int,
    @SerialName("slot_type") val slotType: String,
    val label: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("is_double") val isDouble: Boolean = false,
    @SerialName("double_group") val doubleGroup: Int = 0,
)

@Serializable
data class SchoolDayConfigDto(
    val id: String,
    val name: String,
    @SerialName("applicable_days") val applicableDays: String,
    @SerialName("class_level") val classLevel: String,
    val slots: List<SchoolDaySlotDto>,
    @SerialName("is_active") val isActive: Boolean,
)

@Serializable
data class SchoolDayConfigListResponse(
    val configs: List<SchoolDayConfigDto>,
)

@Serializable
data class CreateSchoolDayConfigRequest(
    val name: String,
    @SerialName("applicable_days") val applicableDays: String,
    @SerialName("class_level") val classLevel: String,
    val slots: List<SchoolDaySlotDto>,
)

@Serializable
data class UpdateSchoolDayConfigRequest(
    val name: String,
    @SerialName("applicable_days") val applicableDays: String,
    @SerialName("class_level") val classLevel: String,
    val slots: List<SchoolDaySlotDto>,
    @SerialName("is_active") val isActive: Boolean = true,
)

private val VALID_SLOT_TYPES = setOf(
    SchoolDaySlotType.TEACHING, SchoolDaySlotType.BREAK, SchoolDaySlotType.ASSEMBLY,
    SchoolDaySlotType.LAB, SchoolDaySlotType.FREE, SchoolDaySlotType.ZERO,
)

private val VALID_CLASS_LEVELS = setOf("ALL", "PRIMARY", "SECONDARY")

private val SYSTEM_SCHOOL_ID: JUUID = JUUID(0, 0)

private fun parseTime(hhmm: String): LocalTime =
    LocalTime.parse(hhmm, SDC_HHMM)

private fun validateSlots(slots: List<SchoolDaySlotDto>): String? {
    if (slots.isEmpty()) return null
    val seenIndices = mutableSetOf<Int>()
    val intervals = mutableListOf<Pair<LocalTime, LocalTime>>()
    for (slot in slots) {
        if (slot.slotType !in VALID_SLOT_TYPES) {
            return "Invalid slot_type: ${slot.slotType}. Allowed: ${VALID_SLOT_TYPES.joinToString()}"
        }
        val start = try { parseTime(slot.startTime) } catch (_: Exception) {
            return "Invalid start_time format for slot ${slot.slotIndex}: expected HH:mm"
        }
        val end = try { parseTime(slot.endTime) } catch (_: Exception) {
            return "Invalid end_time format for slot ${slot.slotIndex}: expected HH:mm"
        }
        if (!end.isAfter(start)) {
            return "Slot ${slot.slotIndex}: end_time must be after start_time"
        }
        if (!seenIndices.add(slot.slotIndex)) {
            return "Duplicate slot_index: ${slot.slotIndex}"
        }
        intervals.add(start to end)
    }
    intervals.sortBy { it.first }
    for (i in 1 until intervals.size) {
        if (intervals[i].first < intervals[i - 1].second) {
            return "Time overlap detected between slots: ${intervals[i - 1].first}-${intervals[i - 1].second} and ${intervals[i].first}-${intervals[i].second}"
        }
    }
    return null
}

private fun validateApplicableDays(days: String): String? {
    val parts = days.split(",").map { it.trim() }
    val nums = parts.mapNotNull { it.toIntOrNull() }
    if (nums.size != parts.size || nums.any { it !in 1..7 }) {
        return "applicable_days must be comma-separated weekday numbers (1-7), e.g. \"1,2,3,4,5\""
    }
    return null
}

private fun rowToConfigDtoOrNull(configId: JUUID, schoolId: JUUID): SchoolDayConfigDto? {
    val configRow = SchoolDayConfigTable.selectAll()
        .where { (SchoolDayConfigTable.id eq configId) and (SchoolDayConfigTable.schoolId eq schoolId) }
        .singleOrNull() ?: return null

    val slotRows = SchoolDaySlotsTable.selectAll()
        .where { (SchoolDaySlotsTable.configId eq configId) and (SchoolDaySlotsTable.schoolId eq schoolId) }
        .orderBy(SchoolDaySlotsTable.slotIndex)
        .toList()

    return SchoolDayConfigDto(
        id = configRow[SchoolDayConfigTable.id].value.toString(),
        name = configRow[SchoolDayConfigTable.name],
        applicableDays = configRow[SchoolDayConfigTable.applicableDays],
        classLevel = configRow[SchoolDayConfigTable.classLevel],
        slots = slotRows.map { r ->
            SchoolDaySlotDto(
                slotIndex = r[SchoolDaySlotsTable.slotIndex],
                slotType = r[SchoolDaySlotsTable.slotType],
                label = r[SchoolDaySlotsTable.label],
                startTime = r[SchoolDaySlotsTable.startTime].format(SDC_HHMM),
                endTime = r[SchoolDaySlotsTable.endTime].format(SDC_HHMM),
                isDouble = r[SchoolDaySlotsTable.isDouble],
                doubleGroup = r[SchoolDaySlotsTable.doubleGroup],
            )
        },
        isActive = configRow[SchoolDayConfigTable.isActive],
    )
}

fun Route.schoolDayConfigRouting() {
    authenticate("jwt") {
        route("/api/v1/school/day-config") {

            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val list = dbQuery {
                    val configs = SchoolDayConfigTable.selectAll()
                        .where { SchoolDayConfigTable.schoolId eq ctx.schoolId }
                        .orderBy(SchoolDayConfigTable.name)
                        .toList()
                    configs.map { r ->
                        val cid = r[SchoolDayConfigTable.id].value
                        val slotRows = SchoolDaySlotsTable.selectAll()
                            .where { (SchoolDaySlotsTable.configId eq cid) and (SchoolDaySlotsTable.schoolId eq ctx.schoolId) }
                            .orderBy(SchoolDaySlotsTable.slotIndex)
                            .toList()
                        SchoolDayConfigDto(
                            id = cid.toString(),
                            name = r[SchoolDayConfigTable.name],
                            applicableDays = r[SchoolDayConfigTable.applicableDays],
                            classLevel = r[SchoolDayConfigTable.classLevel],
                            slots = slotRows.map { s ->
                                SchoolDaySlotDto(
                                    slotIndex = s[SchoolDaySlotsTable.slotIndex],
                                    slotType = s[SchoolDaySlotsTable.slotType],
                                    label = s[SchoolDaySlotsTable.label],
                                    startTime = s[SchoolDaySlotsTable.startTime].format(SDC_HHMM),
                                    endTime = s[SchoolDaySlotsTable.endTime].format(SDC_HHMM),
                                    isDouble = s[SchoolDaySlotsTable.isDouble],
                                    doubleGroup = s[SchoolDaySlotsTable.doubleGroup],
                                )
                            },
                            isActive = r[SchoolDayConfigTable.isActive],
                        )
                    }
                }
                call.ok(SchoolDayConfigListResponse(list), message = "School day configs fetched")
            }

            get("/for-class") {
                val ctx = call.requireSchoolContext() ?: return@get
                val classLevel = call.request.queryParameters["class_level"] ?: "ALL"
                val weekdayStr = call.request.queryParameters["weekday"]
                val weekday = weekdayStr?.toIntOrNull() ?: java.time.LocalDate.now().dayOfWeek.value

                val dto = dbQuery {
                    fun resolveForSchool(schoolId: JUUID): SchoolDayConfigDto? {
                        val configs = SchoolDayConfigTable.selectAll()
                            .where {
                                (SchoolDayConfigTable.schoolId eq schoolId) and
                                    (SchoolDayConfigTable.isActive eq true) and
                                    (SchoolDayConfigTable.classLevel eq classLevel)
                            }
                            .toList()
                            .filter { r ->
                                val days = r[SchoolDayConfigTable.applicableDays]
                                    .split(",").map { it.trim().toIntOrNull() }.filterNotNull()
                                weekday in days
                            }
                        return configs.sortedBy { it[SchoolDayConfigTable.name] }.firstOrNull()?.let { r ->
                            val cid = r[SchoolDayConfigTable.id].value
                            val slotRows = SchoolDaySlotsTable.selectAll()
                                .where { (SchoolDaySlotsTable.configId eq cid) and (SchoolDaySlotsTable.schoolId eq schoolId) }
                                .orderBy(SchoolDaySlotsTable.slotIndex)
                                .toList()
                            SchoolDayConfigDto(
                                id = cid.toString(),
                                name = r[SchoolDayConfigTable.name],
                                applicableDays = r[SchoolDayConfigTable.applicableDays],
                                classLevel = r[SchoolDayConfigTable.classLevel],
                                slots = slotRows.map { s ->
                                    SchoolDaySlotDto(
                                        slotIndex = s[SchoolDaySlotsTable.slotIndex],
                                        slotType = s[SchoolDaySlotsTable.slotType],
                                        label = s[SchoolDaySlotsTable.label],
                                        startTime = s[SchoolDaySlotsTable.startTime].format(SDC_HHMM),
                                        endTime = s[SchoolDaySlotsTable.endTime].format(SDC_HHMM),
                                        isDouble = s[SchoolDaySlotsTable.isDouble],
                                        doubleGroup = s[SchoolDaySlotsTable.doubleGroup],
                                    )
                                },
                                isActive = r[SchoolDayConfigTable.isActive],
                            )
                        }
                    }
                    resolveForSchool(ctx.schoolId) ?: resolveForSchool(SYSTEM_SCHOOL_ID)
                }

                if (dto != null) {
                    call.ok(dto, message = "School day config for class")
                } else {
                    call.okMessage("No config found for the given class and weekday")
                }
            }

            get("/{id}") {
                val ctx = call.requireSchoolContext() ?: return@get
                val configId = call.parameters["id"] ?: run {
                    call.fail("Config ID required", HttpStatusCode.BadRequest, "BAD_REQUEST")
                    return@get
                }
                val uuid = try { JUUID.fromString(configId) } catch (_: Exception) {
                    call.fail("Invalid config ID", HttpStatusCode.BadRequest, "BAD_REQUEST")
                    return@get
                }
                val dto = dbQuery { rowToConfigDtoOrNull(uuid, ctx.schoolId) }
                if (dto != null) {
                    call.ok(dto, message = "School day config fetched")
                } else {
                    call.fail("School day config not found", HttpStatusCode.NotFound, "NOT_FOUND")
                }
            }

            post {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<CreateSchoolDayConfigRequest>()
                if (req.name.isBlank() || req.applicableDays.isBlank()) {
                    call.fail("Name and applicable_days are required", HttpStatusCode.BadRequest, "VALIDATION")
                    return@post
                }
                validateApplicableDays(req.applicableDays.trim())?.let {
                    call.fail(it, HttpStatusCode.BadRequest, "VALIDATION"); return@post
                }
                if (req.classLevel.trim() !in VALID_CLASS_LEVELS) {
                    call.fail("class_level must be one of: ${VALID_CLASS_LEVELS.joinToString()}", HttpStatusCode.BadRequest, "VALIDATION")
                    return@post
                }
                validateSlots(req.slots)?.let {
                    call.fail(it, HttpStatusCode.BadRequest, "VALIDATION"); return@post
                }
                val now = Instant.now()
                val newId = JUUID.randomUUID()
                dbQuery {
                    SchoolDayConfigTable.insert {
                        it[SchoolDayConfigTable.id] = newId
                        it[SchoolDayConfigTable.schoolId] = ctx.schoolId
                        it[SchoolDayConfigTable.name] = req.name.trim()
                        it[SchoolDayConfigTable.applicableDays] = req.applicableDays.trim()
                        it[SchoolDayConfigTable.classLevel] = req.classLevel.trim()
                        it[SchoolDayConfigTable.isActive] = true
                        it[SchoolDayConfigTable.createdAt] = now
                        it[SchoolDayConfigTable.updatedAt] = now
                    }
                    req.slots.forEach { slot ->
                        SchoolDaySlotsTable.insert {
                            it[SchoolDaySlotsTable.configId] = newId
                            it[SchoolDaySlotsTable.schoolId] = ctx.schoolId
                            it[SchoolDaySlotsTable.slotIndex] = slot.slotIndex
                            it[SchoolDaySlotsTable.slotType] = slot.slotType
                            it[SchoolDaySlotsTable.label] = slot.label
                            it[SchoolDaySlotsTable.startTime] = parseTime(slot.startTime)
                            it[SchoolDaySlotsTable.endTime] = parseTime(slot.endTime)
                            it[SchoolDaySlotsTable.isDouble] = slot.isDouble
                            it[SchoolDaySlotsTable.doubleGroup] = slot.doubleGroup
                            it[SchoolDaySlotsTable.createdAt] = now
                        }
                    }
                }
                val dto = dbQuery { rowToConfigDtoOrNull(newId, ctx.schoolId) }
                if (dto != null) {
                    call.created(dto, "School day config created")
                } else {
                    call.fail("Failed to create config", HttpStatusCode.InternalServerError, "INTERNAL")
                }
            }

            put("/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@put
                val configId = call.parameters["id"] ?: run {
                    call.fail("Config ID required", HttpStatusCode.BadRequest, "BAD_REQUEST")
                    return@put
                }
                val uuid = try { JUUID.fromString(configId) } catch (_: Exception) {
                    call.fail("Invalid config ID", HttpStatusCode.BadRequest, "BAD_REQUEST")
                    return@put
                }
                val req = call.receive<UpdateSchoolDayConfigRequest>()
                if (req.name.isBlank() || req.applicableDays.isBlank()) {
                    call.fail("Name and applicable_days are required", HttpStatusCode.BadRequest, "VALIDATION")
                    return@put
                }
                validateApplicableDays(req.applicableDays.trim())?.let {
                    call.fail(it, HttpStatusCode.BadRequest, "VALIDATION"); return@put
                }
                if (req.classLevel.trim() !in VALID_CLASS_LEVELS) {
                    call.fail("class_level must be one of: ${VALID_CLASS_LEVELS.joinToString()}", HttpStatusCode.BadRequest, "VALIDATION")
                    return@put
                }
                validateSlots(req.slots)?.let {
                    call.fail(it, HttpStatusCode.BadRequest, "VALIDATION"); return@put
                }
                val now = Instant.now()
                val updated = dbQuery {
                    val count = SchoolDayConfigTable.update(
                        { (SchoolDayConfigTable.id eq uuid) and (SchoolDayConfigTable.schoolId eq ctx.schoolId) }
                    ) {
                        it[SchoolDayConfigTable.name] = req.name.trim()
                        it[SchoolDayConfigTable.applicableDays] = req.applicableDays.trim()
                        it[SchoolDayConfigTable.classLevel] = req.classLevel.trim()
                        it[SchoolDayConfigTable.isActive] = req.isActive
                        it[SchoolDayConfigTable.updatedAt] = now
                    }
                    if (count == 0) return@dbQuery false
                    SchoolDaySlotsTable.deleteWhere {
                        (SchoolDaySlotsTable.configId eq uuid) and (SchoolDaySlotsTable.schoolId eq ctx.schoolId)
                    }
                    req.slots.forEach { slot ->
                        SchoolDaySlotsTable.insert {
                            it[SchoolDaySlotsTable.configId] = uuid
                            it[SchoolDaySlotsTable.schoolId] = ctx.schoolId
                            it[SchoolDaySlotsTable.slotIndex] = slot.slotIndex
                            it[SchoolDaySlotsTable.slotType] = slot.slotType
                            it[SchoolDaySlotsTable.label] = slot.label
                            it[SchoolDaySlotsTable.startTime] = parseTime(slot.startTime)
                            it[SchoolDaySlotsTable.endTime] = parseTime(slot.endTime)
                            it[SchoolDaySlotsTable.isDouble] = slot.isDouble
                            it[SchoolDaySlotsTable.doubleGroup] = slot.doubleGroup
                            it[SchoolDaySlotsTable.createdAt] = now
                        }
                    }
                    true
                }
                if (!updated) {
                    call.fail("School day config not found", HttpStatusCode.NotFound, "NOT_FOUND")
                    return@put
                }
                val dto = dbQuery { rowToConfigDtoOrNull(uuid, ctx.schoolId) }
                if (dto != null) {
                    call.ok(dto, message = "School day config updated")
                } else {
                    call.fail("Failed to fetch updated config", HttpStatusCode.InternalServerError, "INTERNAL")
                }
            }

            delete("/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@delete
                val configId = call.parameters["id"] ?: run {
                    call.fail("Config ID required", HttpStatusCode.BadRequest, "BAD_REQUEST")
                    return@delete
                }
                val uuid = try { JUUID.fromString(configId) } catch (_: Exception) {
                    call.fail("Invalid config ID", HttpStatusCode.BadRequest, "BAD_REQUEST")
                    return@delete
                }
                val deactivated = dbQuery {
                    SchoolDayConfigTable.update(
                        { (SchoolDayConfigTable.id eq uuid) and (SchoolDayConfigTable.schoolId eq ctx.schoolId) }
                    ) {
                        it[SchoolDayConfigTable.isActive] = false
                        it[SchoolDayConfigTable.updatedAt] = Instant.now()
                    } > 0
                }
                if (deactivated) {
                    call.okMessage("School day config deactivated")
                } else {
                    call.fail("School day config not found", HttpStatusCode.NotFound, "NOT_FOUND")
                }
            }
        }
    }
}
