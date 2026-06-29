// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/domain/PewsEntities.kt
package com.littlebridge.enrollplus.feature.pews.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.Instant
import java.util.UUID

// ── Snapshot ──────────────────────────────────────────────────────────

@Serializable
data class SignalDto(
    val kind: String,
    val label: String,
    val severity: Int,       // 0..100
    val isLeading: Boolean = false,
    val evidenceRef: String? = null,
)

data class SnapshotEntity(
    val id: UUID? = null,
    val schoolId: UUID,
    val studentCode: String,
    val runDate: LocalDate,
    val riskScore: Int,
    val riskLevel: String,       // watch|medium|high
    val attendancePct: Int? = null,
    val marksPct: Int? = null,
    val leaveCount: Int = 0,
    val attendanceSlope: Double? = null,
    val marksSlope: Double? = null,
    val signals: List<SignalDto> = emptyList(),
    val signalHash: String = "",
    val aiNarrative: String? = null,
    val aiCause: String? = null,
    val aiRecommendation: String? = null,
    val aiProviderUsed: String? = null,
    val createdAt: Instant = Instant.now(),
    // PEWS 2.0 expanded fields
    val confidence: Double? = null,
    val leadingScore: Int? = null,
    val causeFamily: String? = null,
    val deltasJson: String? = null,
)

// ── Intervention ──────────────────────────────────────────────────────

data class PlanStepDto(
    val step: Int,
    val action: String,
    val owner: String,          // class_teacher|admin|counsellor
    val slaDays: Int,
    val condition: String? = null,
    val rationale: String? = null,
)

data class InterventionEntity(
    val id: UUID? = null,
    val schoolId: UUID,
    val studentCode: String,
    val snapshotId: UUID? = null,
    val ownerUserId: UUID,
    val actionType: String,
    val status: String = "open",
    val notes: String? = null,
    val openedAt: Instant = Instant.now(),
    val resolvedAt: Instant? = null,
    val outcome: String? = null,
    val createdAt: Instant = Instant.now(),
    // PEWS 2.0 managed casework fields
    val planJson: String? = null,
    val slaDays: Int? = null,
    val escalationLevel: Int = 0,
    val followUpDate: LocalDate? = null,
    val caseFileId: UUID? = null,
    val urgency: String? = null,
    val causeFamily: String? = null,
)

// ── Case File ─────────────────────────────────────────────────────────

@Serializable
data class HypothesisDto(
    val cause: String,
    val confidence: Double,
    val evidence: List<String> = emptyList(),
)

@Serializable
data class ParentDraftDto(
    val language: String,      // hi|en|...
    val tone: String,          // warm, non-clinical
    val body: String,
)

data class CaseFileEntity(
    val id: UUID? = null,
    val schoolId: UUID,
    val snapshotId: UUID? = null,
    val studentCode: String,
    val caseFileJson: String,
    val narrative: String? = null,
    val urgency: String? = null,
    val skipReason: String? = null,
    val parentDraftJson: String? = null,
    val parentDraftLang: String? = null,
    val providerUsed: String? = null,
    val modelUsed: String? = null,
    val groundingPassed: Boolean = true,
    val createdAt: Instant = Instant.now(),
)

// ── Effectiveness Prior ───────────────────────────────────────────────

data class EffectivenessPriorEntity(
    val id: UUID? = null,
    val schoolId: UUID,
    val causeFamily: String,
    val actionType: String,
    val nTried: Int = 0,
    val nImproved: Int = 0,
    val improveRate: Double = 0.0,
    val avgDaysToImprove: Double = 0.0,
    val updatedAt: Instant = Instant.now(),
)

// ── Config ────────────────────────────────────────────────────────────

data class PewsConfigEntity(
    val useRelativeThresholds: Boolean = true,
    val attendanceFloorPct: Int = 75,
    val marksFloorPct: Int = 40,
    val leaveFloorCount: Int = 3,
    val runFrequency: String = "daily",
    val aiNarrativeEnabled: Boolean = true,
    val parentShareEnabled: Boolean = false,
)
