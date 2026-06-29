/*
 * File: PewsIntegrationTest.kt
 * Module: feature.pews.test
 *
 * PEWS 2.0 — Integration smoke tests.
 *
 * Two paths verified:
 *   1. Happy path: ModuleRegistry registers all 6 modules, all module names
 *      are present and unique.
 *   2. Kill switch 503 path: when a module is killed via KillSwitchConfig,
 *      KillSwitchGuard.require() throws PewsDisabledException.
 *
 * These are pure in-memory tests — no HTTP server, no DB. They verify the
 * wiring contract, not the full pipeline (which requires a running DB).
 */
package com.littlebridge.enrollplus.feature.pews

import com.littlebridge.enrollplus.feature.pews.act.ActModule
import com.littlebridge.enrollplus.feature.pews.caseworker.CaseworkerModule
import com.littlebridge.enrollplus.feature.pews.core.KillSwitchGuard
import com.littlebridge.enrollplus.feature.pews.core.ModuleRegistry
import com.littlebridge.enrollplus.feature.pews.core.PewsDisabledException
import com.littlebridge.enrollplus.feature.pews.insights.MockInsightsModule
import com.littlebridge.enrollplus.feature.pews.learn.LearnModule
import com.littlebridge.enrollplus.feature.pews.sense.SenseModule
import com.littlebridge.enrollplus.feature.pews.triage.TriageModule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PewsIntegrationTest {

    @BeforeTest
    fun setup() {
        ModuleRegistry.clear()
    }

    @AfterTest
    fun teardown() {
        ModuleRegistry.clear()
    }

    // ── 1. Happy path: all 6 modules register correctly ───────────────────

    @Test
    fun allSixModulesRegisterSuccessfully() {
        ModuleRegistry.register(SenseModule)
        ModuleRegistry.register(TriageModule)
        ModuleRegistry.register(CaseworkerModule)
        ModuleRegistry.register(ActModule)
        ModuleRegistry.register(LearnModule)
        ModuleRegistry.register(MockInsightsModule)

        val all = ModuleRegistry.all()
        assertEquals(6, all.size, "Expected 6 registered modules")
    }

    @Test
    fun allModuleNamesAreUnique() {
        ModuleRegistry.register(SenseModule)
        ModuleRegistry.register(TriageModule)
        ModuleRegistry.register(CaseworkerModule)
        ModuleRegistry.register(ActModule)
        ModuleRegistry.register(LearnModule)
        ModuleRegistry.register(MockInsightsModule)

        val names = ModuleRegistry.all().map { it.moduleName }
        assertEquals(names.size, names.toSet().size, "Module names must be unique")
    }

    @Test
    fun expectedModuleNamesPresent() {
        ModuleRegistry.register(SenseModule)
        ModuleRegistry.register(TriageModule)
        ModuleRegistry.register(CaseworkerModule)
        ModuleRegistry.register(ActModule)
        ModuleRegistry.register(LearnModule)
        ModuleRegistry.register(MockInsightsModule)

        val names = ModuleRegistry.all().map { it.moduleName }.toSet()
        assertTrue("sense" in names, "SenseModule missing")
        assertTrue("triage" in names, "TriageModule missing")
        assertTrue("caseworker" in names, "CaseworkerModule missing")
        assertTrue("act" in names, "ActModule missing")
        assertTrue("learn" in names, "LearnModule missing")
        assertTrue("insights" in names, "MockInsightsModule missing")
    }

    // ── 2. Kill switch 503 path ────────────────────────────────────────────

    @Test
    fun killSwitchGuard_throwsWhenModuleKilled() {
        // KillSwitchConfig is fail-open before first reload (loaded=false),
        // so we need to simulate the killed state by directly manipulating
        // the internal state. Since KillSwitchConfig reads from a ConcurrentHashMap
        // and checks `loaded`, we use reflection to set the state.
        val config = com.littlebridge.enrollplus.feature.pews.core.KillSwitchConfig

        // Use reflection to set loaded=true and inject a killed flag
        val loadedField = config.javaClass.getDeclaredField("loaded")
        loadedField.isAccessible = true
        loadedField.setBoolean(config, true)

        val flagsField = config.javaClass.getDeclaredField("flags")
        flagsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flags = flagsField.get(config) as java.util.concurrent.ConcurrentHashMap<String, Boolean>
        flags["test_module"] = true

        // Now the guard should throw
        assertFailsWith<PewsDisabledException> {
            KillSwitchGuard.require("test_module")
        }

        // Cleanup
        flags.remove("test_module")
        loadedField.setBoolean(config, false)
    }

    @Test
    fun killSwitchGuard_passesWhenModuleNotKilled() {
        // When not loaded (fail-open), guard should pass
        val config = com.littlebridge.enrollplus.feature.pews.core.KillSwitchConfig
        val loadedField = config.javaClass.getDeclaredField("loaded")
        loadedField.isAccessible = true
        val originalLoaded = loadedField.getBoolean(config)
        loadedField.setBoolean(config, false)

        // Should not throw — fail-open when not loaded
        KillSwitchGuard.require("any_module")

        // Restore
        loadedField.setBoolean(config, originalLoaded)
    }

    @Test
    fun killSwitchGuard_globalKillBlocksAllModules() {
        val config = com.littlebridge.enrollplus.feature.pews.core.KillSwitchConfig

        val loadedField = config.javaClass.getDeclaredField("loaded")
        loadedField.isAccessible = true
        loadedField.setBoolean(config, true)

        val globalKilledField = config.javaClass.getDeclaredField("globalKilled")
        globalKilledField.isAccessible = true
        globalKilledField.setBoolean(config, true)

        // Global kill should block any module
        assertFailsWith<PewsDisabledException> {
            KillSwitchGuard.require("sense")
        }
        assertFailsWith<PewsDisabledException> {
            KillSwitchGuard.require("act")
        }
        assertFailsWith<PewsDisabledException> {
            KillSwitchGuard.require("learn")
        }

        // Cleanup
        globalKilledField.setBoolean(config, false)
        loadedField.setBoolean(config, false)
    }

    // ── 3. ModuleRegistry idempotency ─────────────────────────────────────

    @Test
    fun moduleRegistry_reRegisterReplacesOldEntry() {
        ModuleRegistry.register(SenseModule)
        assertEquals(1, ModuleRegistry.all().size)

        // Re-registering same module name should replace, not duplicate
        ModuleRegistry.register(SenseModule)
        assertEquals(1, ModuleRegistry.all().size, "Re-registration should replace, not duplicate")
    }

    @Test
    fun moduleRegistry_clearRemovesAllModules() {
        ModuleRegistry.register(SenseModule)
        ModuleRegistry.register(TriageModule)
        assertEquals(2, ModuleRegistry.all().size)

        ModuleRegistry.clear()
        assertEquals(0, ModuleRegistry.all().size, "clear() should remove all modules")
    }
}
