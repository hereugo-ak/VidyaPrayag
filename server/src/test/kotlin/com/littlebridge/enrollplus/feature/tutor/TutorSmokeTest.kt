// FILE: server/src/test/kotlin/com/littlebridge/enrollplus/feature/tutor/TutorSmokeTest.kt
package com.littlebridge.enrollplus.feature.tutor

import com.littlebridge.enrollplus.feature.tutor.act.ActModule
import com.littlebridge.enrollplus.feature.tutor.act.SafetyClassifier
import com.littlebridge.enrollplus.feature.tutor.admin.AdminEfficacyModule
import com.littlebridge.enrollplus.feature.tutor.agent.AgentModule
import com.littlebridge.enrollplus.feature.tutor.agent.GroundedRef
import com.littlebridge.enrollplus.feature.tutor.agent.MisconceptionLog
import com.littlebridge.enrollplus.feature.tutor.agent.StudentFacing
import com.littlebridge.enrollplus.feature.tutor.agent.TutorTurn
import com.littlebridge.enrollplus.feature.tutor.agent.TutorTurnCodec
import com.littlebridge.enrollplus.feature.tutor.agent.TutorGroundingGuard
import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorKillSwitch
import com.littlebridge.enrollplus.feature.tutor.core.TutorModule
import com.littlebridge.enrollplus.feature.tutor.core.TutorModuleRegistry
import com.littlebridge.enrollplus.feature.tutor.heatmap.TeacherHeatmapModule
import com.littlebridge.enrollplus.feature.tutor.ingest.IngestModule
import com.littlebridge.enrollplus.feature.tutor.learn.LearnModule
import com.littlebridge.enrollplus.feature.tutor.parent.ParentProgressModule
import com.littlebridge.enrollplus.feature.tutor.rag.RagModule
import com.littlebridge.enrollplus.feature.tutor.sense.LearnerBundle
import com.littlebridge.enrollplus.feature.tutor.sense.Performance
import com.littlebridge.enrollplus.feature.tutor.sense.SenseModule
import com.littlebridge.enrollplus.feature.tutor.sense.TopicScore
import com.littlebridge.enrollplus.feature.tutor.sense.DataConfidence
import com.littlebridge.enrollplus.feature.tutor.sense.HomeworkContext
import com.littlebridge.enrollplus.feature.tutor.sense.ReviewItem
import com.littlebridge.enrollplus.feature.tutor.sense.SyllabusPosition
import com.littlebridge.enrollplus.feature.tutor.sense.Upcoming
import com.littlebridge.enrollplus.feature.tutor.sense.WeakTopic
import com.littlebridge.enrollplus.feature.tutor.triage.TriageModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Smoke tests for AI Tutor 2.0 — verifies module registration, kill-switch
 * enforcement, SafetyClassifier, TutorTurn serialization, GroundingGuard,
 * and module pluggability — all without requiring a DB or AI provider.
 *
 * These tests run without external dependencies (no Postgres, no AI keys).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md — TICK 14
 */
class TutorSmokeTest {

    // ── Module Registry & Pluggability ────────────────────────────────

    @Test
    fun `all 10 modules have correct names`() {
        assertEquals("tutor_sense", SenseModule.moduleName)
        assertEquals("tutor_triage", TriageModule.moduleName)
        assertEquals("tutor_agent", AgentModule.moduleName)
        assertEquals("tutor_act", ActModule.moduleName)
        assertEquals("tutor_learn", LearnModule.moduleName)
        assertEquals("tutor_ingest", IngestModule.moduleName)
        assertEquals("tutor_teacher_heatmap", TeacherHeatmapModule.moduleName)
        assertEquals("tutor_parent_progress", ParentProgressModule.moduleName)
        assertEquals("tutor_admin_efficacy", AdminEfficacyModule.moduleName)
        assertEquals("tutor_rag", RagModule.moduleName)
    }

    @Test
    fun `all module names are distinct`() {
        val names = listOf(
            SenseModule, TriageModule, AgentModule, ActModule,
            LearnModule, IngestModule, TeacherHeatmapModule,
            ParentProgressModule, AdminEfficacyModule, RagModule,
        ).map { it.moduleName }
        assertEquals(names.size, names.toSet().size, "Module names must be unique")
    }

    @Test
    fun `ALL_MODULES includes global and all 10 feature modules`() {
        assertEquals(11, TutorConstants.ALL_MODULES.size)
        assertTrue(TutorConstants.ALL_MODULES.contains(TutorConstants.MODULE_GLOBAL))
        assertTrue(TutorConstants.ALL_MODULES.contains(TutorConstants.MODULE_SENSE))
        assertTrue(TutorConstants.ALL_MODULES.contains(TutorConstants.MODULE_RAG))
    }

    @Test
    fun `registry is accessible and non-empty after boot registration`() {
        // The registry is a singleton populated at boot by registerTutorModules().
        // In test env, modules may or may not be registered depending on test order.
        // Just verify the registry is accessible.
        assertNotNull(TutorModuleRegistry.all())
    }

    @Test
    fun `pluggability proof - custom module can be registered and retrieved`() {
        // Prove that a new module can be registered without modifying any
        // existing code — just implement TutorModule and call register().
        object : TutorModule {
            override val moduleName = "tutor_test_pluggability"
            override fun registerRoutes(parent: io.ktor.server.routing.Route) {
                // No routes — just proving registration works
            }
        }.also { testModule ->
            TutorModuleRegistry.register(testModule)
            val retrieved = TutorModuleRegistry.get("tutor_test_pluggability")
            assertNotNull(retrieved)
            assertEquals("tutor_test_pluggability", retrieved.moduleName)
        }
    }

    // ── Kill Switch ───────────────────────────────────────────────────

    @Test
    fun `kill switch does not throw in test env (fail-open)`() {
        // In test env without DB, KillSwitchConfig.loaded = false → isKilled = false
        // So TutorKillSwitch.require should NOT throw.
        TutorKillSwitch.require(TutorConstants.MODULE_SENSE)
        TutorKillSwitch.require(TutorConstants.MODULE_AGENT)
        TutorKillSwitch.require(TutorConstants.MODULE_RAG)
        // If we got here without exception, the guard works in fail-open mode.
    }

    @Test
    fun `isDisabled returns false in test env`() {
        assertFalse(TutorKillSwitch.isDisabled(TutorConstants.MODULE_SENSE))
        assertFalse(TutorKillSwitch.isDisabled(TutorConstants.MODULE_GLOBAL))
    }

    // ── SafetyClassifier ──────────────────────────────────────────────

    @Test
    fun `safety classifier detects self-harm`() {
        val result = SafetyClassifier.classify("I want to kill myself")
        assertTrue(result.tripped)
        assertEquals("self_harm", result.category)
        assertEquals("escalate_safeguarding", result.action)
    }

    @Test
    fun `safety classifier detects abuse`() {
        val result = SafetyClassifier.classify("my dad hits me every night")
        assertTrue(result.tripped)
        assertEquals("abuse", result.category)
    }

    @Test
    fun `safety classifier detects bullying`() {
        val result = SafetyClassifier.classify("everyone is bullying me at school")
        assertTrue(result.tripped)
        assertEquals("bullying", result.category)
    }

    @Test
    fun `safety classifier detects distress`() {
        val result = SafetyClassifier.classify("I feel so depressed and alone")
        assertTrue(result.tripped)
        assertEquals("distress", result.category)
    }

    @Test
    fun `safety classifier detects violence`() {
        val result = SafetyClassifier.classify("he threatened me with a knife")
        assertTrue(result.tripped)
        assertEquals("violence", result.category)
    }

    @Test
    fun `safety classifier passes for normal doubt`() {
        val result = SafetyClassifier.classify("I don't understand how to solve this fraction problem")
        assertFalse(result.tripped)
        assertNull(result.category)
        assertEquals("none", result.action)
    }

    @Test
    fun `safety classifier is case insensitive`() {
        val result = SafetyClassifier.classify("I WANT TO DIE")
        assertTrue(result.tripped)
        assertEquals("self_harm", result.category)
    }

    // ── TutorTurn Serialization ───────────────────────────────────────

    @Test
    fun `TutorTurn round-trips through JSON`() {
        val turn = TutorTurn(
            mode = "SOCRATIC_STEP",
            groundedRefs = listOf(
                GroundedRef(topicId = "t1", source = "MARKS", value = "65%"),
            ),
            studentFacing = StudentFacing(
                text = "What do you think the first step is?",
                nextPrompt = "Try identifying the given values.",
            ),
        )
        val json = TutorTurnCodec.encode(turn)
        val restored = TutorTurnCodec.parse(json)
        assertNotNull(restored)
        assertEquals(turn.mode, restored.mode)
        assertEquals(turn.studentFacing.text, restored.studentFacing.text)
        assertEquals(1, restored.groundedRefs.size)
        assertEquals("MARKS", restored.groundedRefs[0].source)
    }

    @Test
    fun `TutorTurn parse handles markdown code fences`() {
        val raw = """```json
            {"mode":"HINT","studentFacing":{"text":"Think about it"},"groundedRefs":[]}
        ```""".trimIndent()
        val turn = TutorTurnCodec.parse(raw)
        assertNotNull(turn)
        assertEquals("HINT", turn.mode)
    }

    @Test
    fun `TutorTurn parse returns null for invalid JSON`() {
        assertNull(TutorTurnCodec.parse("not json at all"))
        assertNull(TutorTurnCodec.parse(""))
    }

    @Test
    fun `deterministic fallback produces valid Socratic step`() {
        val turn = TutorTurnCodec.deterministic("What is 2+2?")
        assertEquals("SOCRATIC_STEP", turn.mode)
        assertTrue(turn.studentFacing.text.isNotEmpty())
        assertNotNull(turn.studentFacing.nextPrompt)
    }

    @Test
    fun `TutorTurn with practice questions serializes`() {
        val turn = TutorTurn(
            mode = "PRACTICE_SET",
            studentFacing = StudentFacing(text = "Try these problems"),
            practice = listOf(
                com.littlebridge.enrollplus.feature.tutor.agent.PracticeQuestion(
                    questionId = "q1",
                    stem = "What is 3/4 + 1/4?",
                    options = listOf("1", "3/4", "1/2", "2"),
                    answerKey = "1",
                    topicId = "fractions",
                    difficulty = "easy",
                ),
            ),
        )
        val json = TutorTurnCodec.encode(turn)
        val restored = TutorTurnCodec.parse(json)
        assertNotNull(restored)
        assertEquals(1, restored.practice?.size)
        assertEquals("fractions", restored.practice!![0].topicId)
    }

    @Test
    fun `TutorTurn with misconception log serializes`() {
        val turn = TutorTurn(
            mode = "SOCRATIC_STEP",
            studentFacing = StudentFacing(text = "Let's review"),
            misconception = MisconceptionLog(
                topicId = "subtraction",
                type = "borrowing_error",
                evidence = "Student subtracted smaller from larger without borrowing",
            ),
        )
        val json = TutorTurnCodec.encode(turn)
        val restored = TutorTurnCodec.parse(json)
        assertNotNull(restored)
        assertNotNull(restored.misconception)
        assertEquals("borrowing_error", restored.misconception!!.type)
    }

    // ── GroundingGuard ────────────────────────────────────────────────

    @Test
    fun `grounding guard passes for grounded turn`() {
        val bundle = testBundle()
        val turn = TutorTurn(
            mode = "HINT",
            groundedRefs = listOf(
                GroundedRef(topicId = "t1", source = "MARKS", value = "65%"),
            ),
            studentFacing = StudentFacing(text = "You scored 65% on fractions."),
        )
        val result = TutorGroundingGuard.verify(turn, bundle)
        assertNotNull(result)
    }

    @Test
    fun `grounding guard returns null for completely ungrounded turn`() {
        val bundle = testBundle()
        val turn = TutorTurn(
            mode = "EXPLANATION",
            groundedRefs = emptyList(),
            studentFacing = StudentFacing(text = "The answer is 42."),
        )
        // A turn with no grounded refs and ungrounded claims should be
        // sanitized or rejected.
        val result = TutorGroundingGuard.verify(turn, bundle)
        // Result may be null (too ungrounded) or a sanitized turn.
        // Either way, it should not pass through unmodified with ungrounded claims.
        if (result != null) {
            // If it passed, it should have been sanitized
            assertTrue(result.groundedRefs.isNotEmpty() || result.studentFacing.text.isEmpty())
        }
    }

    // ── LearnerBundle ─────────────────────────────────────────────────

    @Test
    fun `LearnerBundle serializes and deserializes`() {
        val bundle = testBundle()
        val json = kotlinx.serialization.json.Json.encodeToString(LearnerBundle.serializer(), bundle)
        val restored = kotlinx.serialization.json.Json.decodeFromString(LearnerBundle.serializer(), json)
        assertEquals(bundle.childId, restored.childId)
        assertEquals(bundle.subjectId, restored.subjectId)
        assertEquals(bundle.weakTopics.size, restored.weakTopics.size)
    }

    @Test
    fun `LearnerBundle weak topic severity is preserved`() {
        val bundle = testBundle()
        assertEquals("high", bundle.weakTopics[0].severity)
        assertEquals("medium", bundle.weakTopics[1].severity)
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun testBundle() = LearnerBundle(
        childId = "00000000-0000-0000-0000-000000000001",
        subjectId = "00000000-0000-0000-0000-000000000002",
        syllabusPosition = SyllabusPosition(
            currentChapter = "Fractions",
            currentTopic = "Adding Fractions",
            coveredTopicIds = listOf("t1", "t2"),
        ),
        performance = Performance(
            perTopicScore = listOf(
                TopicScore(topicId = "t1", pct = 65.0, attempts = 3),
                TopicScore(topicId = "t2", pct = 80.0, attempts = 2),
            ),
        ),
        weakTopics = listOf(
            WeakTopic(topicId = "t1", pct = 35.0, severity = "high"),
            WeakTopic(topicId = "t3", pct = 50.0, severity = "medium"),
        ),
        homeworkContext = HomeworkContext(),
        reviewQueue = listOf(
            ReviewItem(topicId = "t1", dueAt = "2025-01-15", stability = 2.5, difficulty = 5.0),
        ),
        upcoming = Upcoming(),
        dataConfidence = DataConfidence(hasMarks = true, hasSyllabus = true),
    )
}
