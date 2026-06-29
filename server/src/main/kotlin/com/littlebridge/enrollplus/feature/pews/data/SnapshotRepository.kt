// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/data/SnapshotRepository.kt
package com.littlebridge.enrollplus.feature.pews.data

import com.littlebridge.enrollplus.feature.pews.domain.SnapshotEntity
import java.time.LocalDate
import java.util.UUID

/**
 * Repository contract for PEWS risk snapshot persistence.
 *
 * SOLID:
 *   - I: feature-scoped interface — only snapshot operations.
 *   - D: services depend on this abstraction, not on the Exposed impl.
 *   - L: any implementation (Exposed, in-memory mock, etc.) is substitutable.
 */
interface SnapshotRepository {

    /** Upsert a snapshot for (school, student, runDate). Idempotent. */
    suspend fun upsert(snapshot: SnapshotEntity): UUID

    /** Latest run date that has snapshots for a school (or null). */
    suspend fun latestRunDate(schoolId: UUID): LocalDate?

    /** All snapshots for a school on a given run date (or latest if null). */
    suspend fun cohort(schoolId: UUID, runDate: LocalDate? = null): List<SnapshotEntity>

    /** One student's snapshot for a given run date (or latest if null). */
    suspend fun studentSnapshot(schoolId: UUID, studentCode: String, runDate: LocalDate? = null): SnapshotEntity?

    /** History (last N run dates) for one student. */
    suspend fun studentHistory(schoolId: UUID, studentCode: String, limit: Int = 12): List<SnapshotEntity>

    /** Previous snapshot for a student (the run before the given date). */
    suspend fun previousSnapshot(schoolId: UUID, studentCode: String, beforeDate: LocalDate): SnapshotEntity?

    /** Update AI fields on an existing snapshot row. */
    suspend fun updateAiFields(
        schoolId: UUID, studentCode: String, runDate: LocalDate,
        narrative: String?, cause: String?, recommendation: String?, providerUsed: String?,
    ): Boolean

    /** All active school IDs (for the daily job iteration). */
    suspend fun activeSchoolIds(): List<UUID>
}
