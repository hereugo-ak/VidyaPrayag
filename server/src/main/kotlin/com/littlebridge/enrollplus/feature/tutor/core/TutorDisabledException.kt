// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/core/TutorDisabledException.kt
package com.littlebridge.enrollplus.feature.tutor.core

import com.littlebridge.enrollplus.feature.pews.core.PewsDisabledException

/**
 * Thrown when an AI Tutor 2.0 module is disabled by the kill switch.
 *
 * Extends [PewsDisabledException] so it still works with any code that catches
 * the parent type, but the global StatusPages handler checks for this subtype
 * FIRST and returns `{"tutor":"disabled","module":"<name>"}` instead of the
 * PEWS-flavoured response.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §15 acceptance criteria —
 * kill switch = true → route returns 503 with `{"tutor":"disabled","module":"<name>"}`
 */
class TutorDisabledException(
    moduleName: String,
) : PewsDisabledException(moduleName)
