// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/data/CaseFileRepository.kt
package com.littlebridge.enrollplus.feature.pews.data

import com.littlebridge.enrollplus.feature.pews.domain.CaseFileEntity
import java.util.UUID

/**
 * Repository contract for PEWS case file persistence (Tier-2 Caseworker output).
 *
 * SOLID:
 *   - I: feature-scoped interface — only case file operations.
 *   - D: services depend on this abstraction, not on the Exposed impl.
 */
interface CaseFileRepository {

    /** Persist a new case file. Returns the generated ID. */
    suspend fun create(caseFile: CaseFileEntity): UUID

    /** Read a case file by ID. */
    suspend fun getById(schoolId: UUID, caseFileId: UUID): CaseFileEntity?

    /** Latest case file for a student (most recent first). */
    suspend fun latestForStudent(schoolId: UUID, studentCode: String): CaseFileEntity?

    /** All case files for a student, ordered newest-first. */
    suspend fun historyForStudent(schoolId: UUID, studentCode: String, limit: Int = 10): List<CaseFileEntity>
}
