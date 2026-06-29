// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/data/ConfigRepository.kt
package com.littlebridge.enrollplus.feature.pews.data

import com.littlebridge.enrollplus.feature.pews.domain.PewsConfigEntity
import java.util.UUID

/**
 * Repository contract for PEWS per-school configuration.
 *
 * SOLID:
 *   - I: feature-scoped interface — only config operations.
 *   - D: services depend on this abstraction, not on the Exposed impl.
 */
interface ConfigRepository {

    /** Read per-school config (with sane defaults if row is missing). */
    suspend fun read(schoolId: UUID): PewsConfigEntity

    /** Write (upsert) per-school config. */
    suspend fun write(schoolId: UUID, config: PewsConfigEntity): Boolean
}
