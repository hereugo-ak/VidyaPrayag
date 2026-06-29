package com.littlebridge.enrollplus.feature.idcard

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.IdCardTemplatesTable
import com.littlebridge.enrollplus.db.IdCardsTable
import com.littlebridge.enrollplus.db.NonTeachingStaffTable
import com.littlebridge.enrollplus.db.SchoolBrandingTable
import com.littlebridge.enrollplus.db.SchoolClassesTable
import com.littlebridge.enrollplus.db.SchoolsTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.feature.media.SupabaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable
data class IdCardTemplateDto(
    val id: String,
    val schoolId: String,
    val name: String,
    val roleType: String,
    val frontConfig: String,
    val backConfig: String,
    val isActive: Boolean,
    val createdAt: String,
)

@Serializable
data class CreateTemplateRequest(
    val name: String,
    val roleType: String,       // student | teacher | staff
    val frontConfig: String,    // JSON
    val backConfig: String,     // JSON
)

@Serializable
data class GenerateIdCardRequest(
    val templateId: String,
    val scope: String,          // class | all_students | all_staff
    val classId: String? = null,
)

@Serializable
data class GenerateIdCardResponse(
    val cardIds: List<String>,
    val pdfUrl: String?,
    val count: Int,
)

@Serializable
data class IdCardDto(
    val id: String,
    val schoolId: String? = null,
    val personId: String,
    val personType: String,
    val personName: String,
    val pdfUrl: String? = null,
    val digitalCardUrl: String? = null,
    val qrCodeData: String,
    val validTill: String? = null,
    val status: String = "ready",
)

class IdCardService {

    private val qrBaseUrl: String get() =
        System.getenv("IDCARD_QR_BASE_URL") ?: "https://app.vidyaprayag.in/verify"

    // ── Template CRUD ──────────────────────────────────────────────────

    suspend fun createTemplate(schoolId: UUID, req: CreateTemplateRequest): IdCardTemplateDto = dbQuery {
        // Deactivate existing active templates for same role type
        IdCardTemplatesTable.update(
            { (IdCardTemplatesTable.schoolId eq schoolId) and (IdCardTemplatesTable.roleType eq req.roleType) and (IdCardTemplatesTable.isActive eq true) }
        ) {
            it[isActive] = false
        }

        val insertedRow = IdCardTemplatesTable.insertAndGetId {
            it[IdCardTemplatesTable.schoolId] = schoolId
            it[name] = req.name
            it[roleType] = req.roleType
            it[frontConfig] = req.frontConfig
            it[backConfig] = req.backConfig
            it[isActive] = true
            it[createdAt] = Instant.now()
        }
        IdCardTemplatesTable.selectAll()
            .where { IdCardTemplatesTable.id eq insertedRow }
            .singleOrNull()
            ?.toTemplateDto()
            ?: throw IllegalStateException("Template insert succeeded but row not found")
    }

    suspend fun getTemplates(schoolId: UUID): List<IdCardTemplateDto> = dbQuery {
        IdCardTemplatesTable.selectAll()
            .where { IdCardTemplatesTable.schoolId eq schoolId }
            .orderBy(IdCardTemplatesTable.createdAt, SortOrder.DESC)
            .map { it.toTemplateDto() }
    }

    suspend fun getActiveTemplate(schoolId: UUID, roleType: String): IdCardTemplateDto? = dbQuery {
        IdCardTemplatesTable.selectAll()
            .where { (IdCardTemplatesTable.schoolId eq schoolId) and (IdCardTemplatesTable.roleType eq roleType) and (IdCardTemplatesTable.isActive eq true) }
            .singleOrNull()
            ?.toTemplateDto()
    }

    suspend fun deactivateTemplate(schoolId: UUID, templateId: UUID): Boolean = dbQuery {
        val rows = IdCardTemplatesTable.update(
            { (IdCardTemplatesTable.id eq templateId) and (IdCardTemplatesTable.schoolId eq schoolId) }
        ) {
            it[isActive] = false
        }
        rows > 0
    }

    private suspend fun getTemplateById(id: UUID): IdCardTemplateDto? = dbQuery {
        IdCardTemplatesTable.selectAll()
            .where { IdCardTemplatesTable.id eq id }
            .singleOrNull()
            ?.toTemplateDto()
    }

    // ── Card Generation ────────────────────────────────────────────────

    suspend fun generateCards(schoolId: UUID, req: GenerateIdCardRequest): GenerateIdCardResponse {
        // Use the specified template if it exists and belongs to the school,
        // otherwise fall back to the active template for the role type.
        val template = getTemplateById(UUID.fromString(req.templateId))
            ?.takeIf { UUID.fromString(it.schoolId) == schoolId }
            ?: getActiveTemplate(schoolId, roleTypeForScope(req.scope))
            ?: throw IllegalArgumentException("NO_TEMPLATE")

        val persons = when (req.scope) {
            "all_staff" -> getAllStaff(schoolId)
            "all_students" -> getAllStudents(schoolId)
            "class" -> {
                val classId = req.classId ?: throw IllegalArgumentException("CLASS_ID_REQUIRED")
                getStudentsByClass(schoolId, classId)
            }
            else -> throw IllegalArgumentException("INVALID_SCOPE")
        }

        if (persons.isEmpty()) {
            return GenerateIdCardResponse(cardIds = emptyList(), pdfUrl = null, count = 0)
        }

        val schoolInfo = getSchoolInfo(schoolId)
        val validTillStr = LocalDate.now().plusMonths(12).format(DateTimeFormatter.ISO_DATE)
        val uploadSemaphore = Semaphore(10) // Limit concurrent uploads to avoid overwhelming Supabase

        // Phase 1: Render all card pairs in parallel (CPU-bound, safe on Dispatchers.IO)
        data class RenderedCard(
            val person: PersonInfo,
            val qrData: String,
            val frontPng: ByteArray,
            val backPng: ByteArray,
            val digitalUrl: String?,
        )

        val renderedCards = coroutineScope {
            persons.map { person ->
                async(Dispatchers.IO) {
                    val qrData = "$qrBaseUrl?id=${person.id}&type=${person.type}"
                    val qrPng = QrCodeGenerator.generatePng(qrData)

                    val cardData = IdCardRenderer.CardData(
                        personName = person.name,
                        personType = person.type,
                        classOrDept = person.classOrDept,
                        schoolName = schoolInfo.name,
                        schoolLogoUrl = schoolInfo.logoUrl,
                        photoUrl = person.photoUrl,
                        qrCodePng = qrPng,
                        emergencyContact = person.emergencyContact,
                        bloodGroup = person.bloodGroup,
                        address = schoolInfo.address,
                        validTill = validTillStr,
                        primaryColor = schoolInfo.brandColor,
                        frontConfig = template.frontConfig,
                        backConfig = template.backConfig,
                    )

                    val frontPng = IdCardRenderer.renderFront(cardData)
                    val backPng = IdCardRenderer.renderBack(cardData)

                    // Upload digital card image with concurrency limit
                    val digitalUrl = uploadSemaphore.withPermit {
                        uploadToStorage(schoolId, frontPng, "image/png")
                    }

                    RenderedCard(person, qrData, frontPng, backPng, digitalUrl)
                }
            }.awaitAll()
        }

        // Phase 2: Batch DB inserts
        val cardIds = mutableListOf<UUID>()
        for (rc in renderedCards) {
            val cardId = dbQuery {
                IdCardsTable.insertAndGetId {
                    it[IdCardsTable.schoolId] = schoolId
                    it[personId] = rc.person.id
                    it[personType] = rc.person.type
                    it[personName] = rc.person.name
                    it[templateId] = UUID.fromString(req.templateId)
                    it[pdfUrl] = null
                    it[digitalCardUrl] = rc.digitalUrl
                    it[qrCodeData] = rc.qrData
                    it[validTill] = LocalDate.now().plusMonths(12)
                    it[status] = "ready"
                    it[createdAt] = Instant.now()
                }.value
            }
            cardIds.add(cardId)
        }

        // Phase 3: Generate combined PDF (streamed to temp file to avoid OOM)
        val cardPairs = renderedCards.map { it.frontPng to it.backPng }
        val pdfBytes = PdfGenerator.generate(cardPairs)
        val pdfUrl = uploadToStorage(schoolId, pdfBytes, "application/pdf")

        // Update all cards with PDF URL
        if (pdfUrl != null) {
            for (cid in cardIds) {
                dbQuery {
                    IdCardsTable.update({ IdCardsTable.id eq cid }) {
                        it[IdCardsTable.pdfUrl] = pdfUrl
                    }
                }
            }
        }

        return GenerateIdCardResponse(
            cardIds = cardIds.map { it.toString() },
            pdfUrl = pdfUrl,
            count = cardIds.size,
        )
    }

    // ── Card Retrieval & Soft Delete ──────────────────────────────────

    suspend fun deleteCard(schoolId: UUID, cardId: UUID): Boolean = dbQuery {
        val updated = IdCardsTable.update(
            { (IdCardsTable.id eq cardId) and (IdCardsTable.schoolId eq schoolId) and (IdCardsTable.status neq "deleted") }
        ) {
            it[status] = "deleted"
        }
        updated > 0
    }

    suspend fun getCardByPerson(personId: UUID, personType: String): IdCardDto? = dbQuery {
        IdCardsTable.selectAll()
            .where { (IdCardsTable.personId eq personId) and (IdCardsTable.personType eq personType) and (IdCardsTable.status neq "deleted") }
            .orderBy(IdCardsTable.createdAt, SortOrder.DESC)
            .firstOrNull()
            ?.toCardDto()
    }

    suspend fun getCardById(cardId: UUID): IdCardDto? = dbQuery {
        IdCardsTable.selectAll()
            .where { (IdCardsTable.id eq cardId) and (IdCardsTable.status neq "deleted") }
            .singleOrNull()
            ?.toCardDto()
    }

    suspend fun getCardsBySchool(schoolId: UUID): List<IdCardDto> = dbQuery {
        IdCardsTable.selectAll()
            .where { (IdCardsTable.schoolId eq schoolId) and (IdCardsTable.status neq "deleted") }
            .orderBy(IdCardsTable.createdAt, SortOrder.DESC)
            .map { it.toCardDto() }
    }

    suspend fun getCardsBySchoolPaginated(
        schoolId: UUID,
        page: Int = 1,
        limit: Int = 50,
        search: String? = null,
        personType: String? = null,
    ): List<IdCardDto> = dbQuery {
        val offset = ((page - 1).coerceAtLeast(0)) * limit
        val query = IdCardsTable.selectAll()
            .where { (IdCardsTable.schoolId eq schoolId) and (IdCardsTable.status neq "deleted") }

        val filtered = if (personType != null && personType.isNotBlank()) {
            query.andWhere { IdCardsTable.personType eq personType }
        } else query

        val searched = if (!search.isNullOrBlank()) {
            filtered.andWhere { IdCardsTable.personName like "%$search%" }
        } else filtered

        searched
            .orderBy(IdCardsTable.createdAt, SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { it.toCardDto() }
    }

    suspend fun getCardCount(schoolId: UUID, search: String? = null, personType: String? = null): Long = dbQuery {
        val query = IdCardsTable.selectAll()
            .where { (IdCardsTable.schoolId eq schoolId) and (IdCardsTable.status neq "deleted") }

        val filtered = if (personType != null && personType.isNotBlank()) {
            query.andWhere { IdCardsTable.personType eq personType }
        } else query

        val searched = if (!search.isNullOrBlank()) {
            filtered.andWhere { IdCardsTable.personName like "%$search%" }
        } else filtered

        searched.count()
    }

    suspend fun getExpiringCards(withinDays: Int = 30): List<IdCardDto> = dbQuery {
        val cutoff = LocalDate.now().plusDays(withinDays.toLong())
        IdCardsTable.selectAll()
            .where {
                (IdCardsTable.validTill lessEq cutoff) and
                (IdCardsTable.validTill greaterEq LocalDate.now())
            }
            .map { it.toCardDto() }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    suspend fun getStaffIdByAppUserId(appUserId: UUID): UUID? = dbQuery {
        // OPT-06: Direct FK lookup first
        val directMatch = NonTeachingStaffTable.selectAll()
            .where { (NonTeachingStaffTable.appUserId eq appUserId) and (NonTeachingStaffTable.isActive eq true) }
            .singleOrNull()
            ?.get(NonTeachingStaffTable.id)?.value
        if (directMatch != null) return@dbQuery directMatch

        // Fallback: email-based matching for records not yet backfilled
        val email = AppUsersTable.selectAll()
            .where { AppUsersTable.id eq appUserId }
            .singleOrNull()
            ?.get(AppUsersTable.email)
            ?: return@dbQuery null

        NonTeachingStaffTable.selectAll()
            .where { (NonTeachingStaffTable.email eq email) and (NonTeachingStaffTable.isActive eq true) }
            .singleOrNull()
            ?.get(NonTeachingStaffTable.id)?.value
    }

    private fun roleTypeForScope(scope: String): String = when (scope) {
        "all_staff" -> "staff"
        "all_students", "class" -> "student"
        else -> "student"
    }

    private data class PersonInfo(
        val id: UUID,
        val name: String,
        val type: String,
        val classOrDept: String,
        val photoUrl: String?,
        val emergencyContact: String?,
        val bloodGroup: String?,
    )

    private suspend fun getAllStudents(schoolId: UUID): List<PersonInfo> = dbQuery {
        StudentsTable.selectAll()
            .where { (StudentsTable.schoolId eq schoolId) and (StudentsTable.isActive eq true) }
            .map {
                PersonInfo(
                    id = it[StudentsTable.id].value,
                    name = it[StudentsTable.fullName],
                    type = "student",
                    classOrDept = "${it[StudentsTable.className]}-${it[StudentsTable.section]}",
                    photoUrl = it[StudentsTable.profilePhotoUrl],
                    emergencyContact = it[StudentsTable.parentPhone],
                    bloodGroup = null,
                )
            }
    }

    private suspend fun getStudentsByClass(schoolId: UUID, classId: String): List<PersonInfo> = dbQuery {
        val classRow = SchoolClassesTable.selectAll()
            .where { (SchoolClassesTable.id eq UUID.fromString(classId)) and (SchoolClassesTable.schoolId eq schoolId) }
            .singleOrNull() ?: return@dbQuery emptyList()

        val className = classRow[SchoolClassesTable.name]

        StudentsTable.selectAll()
            .where {
                (StudentsTable.schoolId eq schoolId) and
                (StudentsTable.isActive eq true) and
                (StudentsTable.className eq className)
            }
            .map {
                PersonInfo(
                    id = it[StudentsTable.id].value,
                    name = it[StudentsTable.fullName],
                    type = "student",
                    classOrDept = "${it[StudentsTable.className]}-${it[StudentsTable.section]}",
                    photoUrl = it[StudentsTable.profilePhotoUrl],
                    emergencyContact = it[StudentsTable.parentPhone],
                    bloodGroup = null,
                )
            }
    }

    private suspend fun getAllStaff(schoolId: UUID): List<PersonInfo> = dbQuery {
        // Teachers from app_users
        val teachers = AppUsersTable.selectAll()
            .where { (AppUsersTable.schoolId eq schoolId) and (AppUsersTable.role eq "teacher") and (AppUsersTable.isActive eq true) }
            .map {
                PersonInfo(
                    id = it[AppUsersTable.id].value,
                    name = it[AppUsersTable.fullName],
                    type = "teacher",
                    classOrDept = "Teacher",
                    photoUrl = it[AppUsersTable.profilePicUrl],
                    emergencyContact = it[AppUsersTable.phone],
                    bloodGroup = null,
                )
            }

        // Non-teaching staff
        val staff = NonTeachingStaffTable.selectAll()
            .where { (NonTeachingStaffTable.schoolId eq schoolId) and (NonTeachingStaffTable.isActive eq true) }
            .map {
                PersonInfo(
                    id = it[NonTeachingStaffTable.id].value,
                    name = it[NonTeachingStaffTable.fullName],
                    type = "staff",
                    classOrDept = it[NonTeachingStaffTable.department] ?: it[NonTeachingStaffTable.role],
                    photoUrl = it[NonTeachingStaffTable.photoUrl],
                    emergencyContact = it[NonTeachingStaffTable.phone],
                    bloodGroup = null,
                )
            }

        teachers + staff
    }

    private data class SchoolInfo(
        val name: String,
        val logoUrl: String?,
        val address: String?,
        val brandColor: String,
    )

    private suspend fun getSchoolInfo(schoolId: UUID): SchoolInfo = dbQuery {
        val school = SchoolsTable.selectAll()
            .where { SchoolsTable.id eq schoolId }
            .single()

        // Try branding table for custom colors
        val branding = SchoolBrandingTable.selectAll()
            .where { SchoolBrandingTable.schoolId eq schoolId }
            .singleOrNull()

        SchoolInfo(
            name = school[SchoolsTable.name],
            logoUrl = branding?.get(SchoolBrandingTable.logoUrl) ?: school[SchoolsTable.logoUrl],
            address = school[SchoolsTable.fullAddress],
            brandColor = branding?.get(SchoolBrandingTable.primaryColor) ?: school[SchoolsTable.brandColor],
        )
    }

    private suspend fun uploadToStorage(schoolId: UUID, bytes: ByteArray, contentType: String): String? {
        if (!SupabaseStorage.isConfigured()) return null
        val kind = if (contentType == "application/pdf") "PDF" else "IMAGE"
        val result = SupabaseStorage.upload(schoolId, kind, bytes, contentType)
        return result?.url
    }

    // ── Row Mappers ────────────────────────────────────────────────────

    private fun ResultRow.toTemplateDto() = IdCardTemplateDto(
        id = this[IdCardTemplatesTable.id].toString(),
        schoolId = this[IdCardTemplatesTable.schoolId].toString(),
        name = this[IdCardTemplatesTable.name],
        roleType = this[IdCardTemplatesTable.roleType],
        frontConfig = this[IdCardTemplatesTable.frontConfig],
        backConfig = this[IdCardTemplatesTable.backConfig],
        isActive = this[IdCardTemplatesTable.isActive],
        createdAt = this[IdCardTemplatesTable.createdAt].toString(),
    )

    private fun ResultRow.toCardDto() = IdCardDto(
        id = this[IdCardsTable.id].toString(),
        schoolId = this[IdCardsTable.schoolId].toString(),
        personId = this[IdCardsTable.personId].toString(),
        personType = this[IdCardsTable.personType],
        personName = this[IdCardsTable.personName],
        pdfUrl = this[IdCardsTable.pdfUrl],
        digitalCardUrl = this[IdCardsTable.digitalCardUrl],
        qrCodeData = this[IdCardsTable.qrCodeData],
        validTill = this[IdCardsTable.validTill]?.toString(),
        status = this[IdCardsTable.status],
    )
}
