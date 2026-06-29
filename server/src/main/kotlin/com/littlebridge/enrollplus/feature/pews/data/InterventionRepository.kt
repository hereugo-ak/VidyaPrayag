// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/data/InterventionRepository.kt
package com.littlebridge.enrollplus.feature.pews.data

import com.littlebridge.enrollplus.feature.pews.domain.InterventionEntity
import java.time.LocalDate
import java.util.UUID

/**
 * Repository contract for PEWS intervention persistence.
 *
 * SOLID:
 *   - I: feature-scoped interface — only intervention operations.
 *   - D: services depend on this abstraction, not on the Exposed impl.
 */
interface InterventionRepository {

    /** Create a new intervention. Returns the generated ID. */
    suspend fun create(intervention: InterventionEntity): UUID

    /** Read a single intervention by ID, scoped to school. */
    suspend fun getById(schoolId: UUID, interventionId: UUID): InterventionEntity?

    /** List interventions for a school, optionally filtered by owner/status. */
    suspend fun list(
        schoolId: UUID,
        ownerUserId: UUID? = null,
        status: String? = null,
    ): List<InterventionEntity>

    /** Check if a student already has an open/in-progress intervention. */
    suspend fun hasOpenIntervention(schoolId: UUID, studentCode: String): Boolean

    /** Update status/notes/outcome/actionType. Returns true if found and updated. */
    suspend fun update(
        schoolId: UUID,
        interventionId: UUID,
        status: String? = null,
        notes: String? = null,
        outcome: String? = null,
        actionType: String? = null,
        planJson: String? = null,
        slaDays: Int? = null,
        escalationLevel: Int? = null,
        followUpDate: LocalDate? = null,
        ownerUserId: UUID? = null,
    ): Boolean

    /** Find interventions past their SLA that haven't been escalated yet. */
    suspend fun findPastSla(schoolId: UUID, asOf: LocalDate): List<InterventionEntity>

    /** Effectiveness rollup counts for a school. */
    suspend fun effectivenessCounts(schoolId: UUID): EffectivenessCounts
}

data class EffectivenessCounts(
    val total: Int,
    val open: Int,
    val done: Int,
    val dismissed: Int,
    val improved: Int,
    val unchanged: Int,
    val worsened: Int,
)
