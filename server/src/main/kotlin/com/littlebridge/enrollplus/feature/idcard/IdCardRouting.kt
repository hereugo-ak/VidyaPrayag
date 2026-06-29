package com.littlebridge.enrollplus.feature.idcard

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.okMessage
import com.littlebridge.enrollplus.core.principalUserId
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.StudentsTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

fun Route.idCardRouting() {
    val service = IdCardService()

    // ── Admin endpoints (school admin only) ────────────────────────────
    authenticate("jwt") {
        route("/api/v1/school/id-cards") {

            // GET templates
            get("/templates") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val templates = service.getTemplates(ctx.schoolId)
                call.ok(templates)
            }

            // POST create template
            post("/templates") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<CreateTemplateRequest>()
                if (req.name.isBlank()) {
                    call.fail("Template name is required", HttpStatusCode.BadRequest, "VALIDATION_ERROR")
                    return@post
                }
                if (req.roleType !in listOf("student", "teacher", "staff")) {
                    call.fail("Invalid role type", HttpStatusCode.BadRequest, "INVALID_ROLE_TYPE")
                    return@post
                }
                val template = service.createTemplate(ctx.schoolId, req)
                call.ok(template, "Template created", HttpStatusCode.Created)
            }

            // DELETE deactivate template
            delete("/templates/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@delete
                val templateId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid template id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@delete }
                val success = service.deactivateTemplate(ctx.schoolId, templateId)
                if (success) call.okMessage("Template deactivated")
                else call.fail("Template not found", HttpStatusCode.NotFound, "TEMPLATE_NOT_FOUND")
            }

            // POST generate cards
            post("/generate") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<GenerateIdCardRequest>()
                if (req.scope !in listOf("class", "all_students", "all_staff")) {
                    call.fail("Invalid scope", HttpStatusCode.BadRequest, "INVALID_SCOPE")
                    return@post
                }
                if (req.scope == "class" && req.classId.isNullOrBlank()) {
                    call.fail("Class ID required when scope is 'class'", HttpStatusCode.BadRequest, "CLASS_ID_REQUIRED")
                    return@post
                }
                try {
                    val result = service.generateCards(ctx.schoolId, req)
                    call.ok(result, "Generated ${result.count} ID cards")
                } catch (e: IllegalArgumentException) {
                    call.fail(e.message ?: "Generation failed", HttpStatusCode.BadRequest, e.message)
                } catch (e: Exception) {
                    call.fail("ID card generation failed: ${e.message}", HttpStatusCode.InternalServerError, "GENERATION_FAILED")
                }
            }

            // GET all cards for school (with optional pagination/search)
            get {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val page = call.parameters["page"]?.toIntOrNull()
                val limit = call.parameters["limit"]?.toIntOrNull()
                val search = call.parameters["search"]
                val personType = call.parameters["personType"]

                if (page != null || limit != null || !search.isNullOrBlank() || !personType.isNullOrBlank()) {
                    // Paginated response
                    val cards = service.getCardsBySchoolPaginated(
                        schoolId = ctx.schoolId,
                        page = page ?: 1,
                        limit = limit ?: 50,
                        search = search,
                        personType = personType,
                    )
                    val total = service.getCardCount(ctx.schoolId, search, personType)
                    call.ok(mapOf(
                        "cards" to cards,
                        "total" to total,
                        "page" to (page ?: 1),
                        "limit" to (limit ?: 50),
                    ))
                } else {
                    // Backward-compatible: return all cards
                    val cards = service.getCardsBySchool(ctx.schoolId)
                    call.ok(cards)
                }
            }

            // GET PDF URL for a specific card
            get("/{id}/pdf") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val cardId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid card id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                val card = service.getCardById(cardId)
                if (card == null) {
                    call.fail("Card not found", HttpStatusCode.NotFound, "CARD_NOT_FOUND")
                    return@get
                }
                if (card.pdfUrl.isNullOrBlank()) {
                    call.fail("PDF not available", HttpStatusCode.NotFound, "CARD_NOT_FOUND")
                    return@get
                }
                call.ok(mapOf("pdfUrl" to card.pdfUrl))
            }

            // DELETE soft-delete a card
            delete("/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@delete
                val cardId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid card id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@delete }
                val success = service.deleteCard(ctx.schoolId, cardId)
                if (success) call.okMessage("Card deleted")
                else call.fail("Card not found", HttpStatusCode.NotFound, "CARD_NOT_FOUND")
            }
        }
    }

    // ── Public: serve QR PNG on-demand (no auth needed for QR display) ──
    get("/api/v1/id-card/{id}/qr.png") {
        val cardId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: run { call.fail("Invalid card id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
        val card = service.getCardById(cardId)
        if (card == null) {
            call.fail("Card not found", HttpStatusCode.NotFound, "CARD_NOT_FOUND")
            return@get
        }
        val qrPng = QrCodeGenerator.generatePng(card.qrCodeData)
        call.respondBytes(qrPng, ContentType.Image.PNG)
    }

    // ── Parent: view child's digital ID card ───────────────────────────
    authenticate("jwt") {
        get("/api/v1/parent/id-card/{childId}") {
            val parentUserId = call.principalUserId()
                ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
            val childId = call.parameters["childId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: run { call.fail("Invalid child id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }

            // Verify parent has access to this child
            val hasAccess = dbQuery {
                ChildrenTable.selectAll()
                    .where {
                        (ChildrenTable.id eq childId) and (ChildrenTable.parentId eq UUID.fromString(parentUserId))
                    }
                    .any()
            }
            if (!hasAccess) {
                call.fail("Not your child", HttpStatusCode.Forbidden, "NOT_OWN_CARD")
                return@get
            }

            // Look up student via studentCode from children table
            val studentCode = dbQuery {
                ChildrenTable.selectAll()
                    .where { ChildrenTable.id eq childId }
                    .singleOrNull()
                    ?.get(ChildrenTable.studentCode)
            } ?: run {
                call.fail("Child not found", HttpStatusCode.NotFound, "CARD_NOT_FOUND")
                return@get
            }

            if (studentCode.isNullOrBlank()) {
                call.fail("No ID card found. Ask admin to generate.", HttpStatusCode.NotFound, "CARD_NOT_FOUND")
                return@get
            }

            // Find student by code
            val studentId = dbQuery {
                StudentsTable.selectAll()
                    .where { StudentsTable.studentCode eq studentCode }
                    .singleOrNull()
                    ?.get(StudentsTable.id)?.value
            } ?: run {
                call.fail("No ID card found. Ask admin to generate.", HttpStatusCode.NotFound, "CARD_NOT_FOUND")
                return@get
            }

            val card = service.getCardByPerson(studentId, "student")
            if (card == null) {
                call.fail("No ID card found. Ask admin to generate.", HttpStatusCode.NotFound, "CARD_NOT_FOUND")
                return@get
            }
            call.ok(card)
        }
    }

    // ── Teacher: view own digital ID card ──────────────────────────────
    authenticate("jwt") {
        get("/api/v1/teacher/id-card") {
            val userId = call.principalUserId()
                ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
            val personId = UUID.fromString(userId)
            val card = service.getCardByPerson(personId, "teacher")
            if (card == null) {
                call.fail("No ID card found. Ask admin to generate.", HttpStatusCode.NotFound, "CARD_NOT_FOUND")
                return@get
            }
            call.ok(card)
        }
    }

    // ── Staff: view own digital ID card ───────────────────────────────
    authenticate("jwt") {
        get("/api/v1/staff/id-card") {
            val userId = call.principalUserId()
                ?: run { call.fail("Unauthorized", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
            // NonTeachingStaffTable has no userId FK — look up by email match.
            val staffId = service.getStaffIdByAppUserId(UUID.fromString(userId))
            if (staffId == null) {
                call.fail("No ID card found. Ask admin to generate.", HttpStatusCode.NotFound, "CARD_NOT_FOUND")
                return@get
            }
            val card = service.getCardByPerson(staffId, "staff")
            if (card == null) {
                call.fail("No ID card found. Ask admin to generate.", HttpStatusCode.NotFound, "CARD_NOT_FOUND")
                return@get
            }
            call.ok(card)
        }
    }
}
