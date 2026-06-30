// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/data/EffectivenessPriorsRepository.kt
package com.littlebridge.enrollplus.feature.pews.data

import com.littlebridge.enrollplus.feature.pews.domain.EffectivenessPriorEntity
import java.util.UUID

/**
 * Repository contract for PEWS effectiveness priors (the LEARN flywheel's
 * policy memory). Per-school, per-cause-family, per-action-type.
 *
 * SOLID:
 *   - I: feature-scoped interface — only effectiveness prior operations.
 *   - D: services depend on this abstraction, not on the Exposed impl.
 */
interface EffectivenessPriorsRepository {

    /** Get the prior for a (school, causeFamily, actionType) tuple, or null. */
    suspend fun get(schoolId: UUID, causeFamily: String, actionType: String): EffectivenessPriorEntity?

    /** Get all priors for a school (for the prescriptive admin view). */
    suspend fun listForSchool(schoolId: UUID): List<EffectivenessPriorEntity>

    /** Get all priors for a cause family in a school (for get_similar_resolved_cases). */
    suspend fun listForCauseFamily(schoolId: UUID, causeFamily: String): List<EffectivenessPriorEntity>

    /**
     * Record an outcome: increment nTried, conditionally increment nImproved,
     * recompute improveRate and avgDaysToImprove. Upserts the row if it doesn't exist.
     */
    suspend fun recordOutcome(
        schoolId: UUID,
        causeFamily: String,
        actionType: String,
        improved: Boolean,
        daysToImprove: Double?,
    ): Boolean
}
