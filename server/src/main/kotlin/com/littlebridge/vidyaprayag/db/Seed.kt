/*
 * File: Seed.kt
 * Module: db
 *
 * CMS-only seeder.
 *
 * This file replaces the previous "Seed.kt" which inserted hardcoded demo
 * schools, demo announcements, demo enquiries, etc.  Those were a footgun:
 * they polluted the real database with phantom rows on every cold start.
 *
 * What this file does instead — and ONLY this — is INSERT-IF-MISSING the
 * CMS strings that drive the public landing page and the splash-screen
 * /config/app-status handshake.  These are not user-data; they're product
 * copy and feature flags, and they MUST exist for the first cold boot of
 * the app to render correctly.
 *
 * Idempotency:
 *   - For each key we only insert when no row exists.
 *   - Operators can edit `cms_landing_content` / `app_config` directly in
 *     the Supabase dashboard; subsequent backend restarts will respect the
 *     edits because we never UPDATE here.
 *
 * To turn the seed off in production:
 *   APP_SEED_CMS=false   (env var, see DatabaseFactory.kt)
 *
 * Spec refs:
 *   - vidya_prayag_api_spec.artifact.md §Common Landing Page
 *   - vidya_prayag_api_spec.artifact.md §Splash / Startup
 */
package com.littlebridge.vidyaprayag.db

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object CmsSeed {

    fun ensureLandingAndConfig() {
        transaction {
            val landingDefaults = mapOf(
                "top_tagline" to "\"Education with Trust.\"",
                "sub_tagline" to "\"Progress with Purpose.\"",
                "parent_info" to """
                    {
                      "top_tagline": "FOR PARENTS",
                      "sub_tagline": "Find the perfect school for your child's unique journey",
                      "list_of_features": ["Data-driven insights", "Verified institutional profiles"],
                      "list_of_sub_features": ["Match score", "Direct inquiry"]
                    }
                """.trimIndent(),
                "school_info" to """
                    {
                      "top_tagline": "FOR SCHOOLS",
                      "sub_tagline": "Scale excellence with intelligence.",
                      "list_of_features": ["Institutional management tools", "Growth tracking"],
                      "list_of_sub_features": ["Predictive analysis", "Automated workflows"]
                    }
                """.trimIndent(),
                "list_of_offerings" to """
                    [
                      {
                        "icon_url": "https://dumoiojpkizxkzzxdzss.supabase.co/storage/v1/object/sign/vidya-prayag/ic_chat_wa.png?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV9kMWZmYzY2Ni04YTcwLTRmMmItODE3OC00YzBlZGM0YjA2ODIiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJ2aWR5YS1wcmF5YWcvaWNfY2hhdF93YS5wbmciLCJpYXQiOjE3Nzk0ODM5MzAsImV4cCI6MTgxMTAxOTkzMH0.qEp2WtNkLSDRC9YKu4cf0lEy7DnOa8gS1xW_-DpGdSU",
                        "heading": "WhatsApp-First",
                        "description": "Seamless communication between parents and faculty without app fatigue.",
                        "is_live": true
                      },
                      {
                        "icon_url": "https://dumoiojpkizxkzzxdzss.supabase.co/storage/v1/object/sign/vidya-prayag/ic_sri.png?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV9kMWZmYzY2Ni04YTcwLTRmMmItODE3OC00YzBlZGM0YjA2ODIiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJ2aWR5YS1wcmF5YWcvaWNfc3JpLnBuZyIsImlhdCI6MTc3OTQ4Mzk1NiwiZXhwIjoxODExMDE5OTU2fQ.9sJQ16aDwGJJiR8ryYVBUzDiOUuTtBoTRSqc22A2G1Q",
                        "heading": "SRI Index",
                        "description": "Standardized Reliability Index for objective school performance tracking.",
                        "is_live": true
                      },
                      {
                        "icon_url": "https://dumoiojpkizxkzzxdzss.supabase.co/storage/v1/object/sign/vidya-prayag/ic_pews.png?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV9kMWZmYzY2Ni04YTcwLTRmMmItODE3OC00YzBlZGM0YjA2ODIiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJ2aWR5YS1wcmF5YWcvaWNfcGV3cy5wbmciLCJpYXQiOjE3Nzk0ODM5NzAsImV4cCI6MTgxMTAxOTk3MH0.fQ7ZrF9JQTWnFM8P8Oq6uAaqK5TTTaLYT3fDvll-2N0",
                        "heading": "PEWS System",
                        "description": "Predictive Early Warning System for student safety and academic risk.",
                        "is_live": true
                      },
                      {
                        "icon_url": "https://dumoiojpkizxkzzxdzss.supabase.co/storage/v1/object/sign/vidya-prayag/ic_intel_graph.png?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV9kMWZmYzY2Ni04YTcwLTRmMmItODE3OC00YzBlZGM0YjA2ODIiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJ2aWR5YS1wcmF5YWcvaWNfaW50ZWxfZ3JhcGgucG5nIiwiaWF0IjoxNzc5NDgzOTgyLCJleHAiOjE4MTEwMTk5ODJ9.qf4Ty6iKGdJWB9rpSC0RkoRpC0iPoQvYUOSPAYEUJ9Q",
                        "heading": "Intelligence Graph",
                        "description": "Mapping student talent and progress across a decade of learning data.",
                        "is_live": true
                      }
                    ]
                """.trimIndent(),
                "list_of_portals" to """
                    [
                      {
                        "icon_url": "https://dumoiojpkizxkzzxdzss.supabase.co/storage/v1/object/sign/vidya-prayag/ic_intel_graph.png?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV9kMWZmYzY2Ni04YTcwLTRmMmItODE3OC00YzBlZGM0YjA2ODIiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJ2aWR5YS1wcmF5YWcvaWNfaW50ZWxfZ3JhcGgucG5nIiwiaWF0IjoxNzc5NDgzOTgyLCJleHAiOjE4MTEwMTk5ODJ9.qf4Ty6iKGdJWB9rpSC0RkoRpC0iPoQvYUOSPAYEUJ9Q",
                        "heading": "Teacher's Portal",
                        "description": "Monitor your student's growth.",
                        "is_live": true
                      }
                    ]
                """.trimIndent(),
                "login_modes" to """["EMAIL","MOBILE"]""",
                "tos_link" to "\"https://vidyaprayag.com/terms\"",
                "privacy_policy_link" to "\"https://vidyaprayag.com/privacy\""
            )

            val existingLanding = LandingContentTable
                .selectAll()
                .map { it[LandingContentTable.key] }
                .toSet()
            landingDefaults.forEach { (k, v) ->
                if (k !in existingLanding) {
                    LandingContentTable.insert {
                        it[key] = k
                        it[value] = v
                        it[updatedAt] = Instant.now()
                    }
                }
            }

            val appDefaults = mapOf(
                "version_check" to """
                    {
                      "current_version": "1.0.0",
                      "minimum_required_version": "1.0.0",
                      "force_update": false,
                      "update_url": "https://play.google.com/store/apps/details?id=com.littlebridge.vidyaprayag",
                      "update_message": "A new version with performance improvements is available."
                    }
                """.trimIndent(),
                "maintenance" to """
                    {
                      "is_under_maintenance": false,
                      "estimated_end_time": null,
                      "message": "We're upgrading our servers. We'll be back shortly."
                    }
                """.trimIndent(),
                "flags" to """
                    {
                      "is_whatsapp_sync_enabled": true,
                      "show_scholarships": false,
                      "is_ai_narrative_live": false,
                      "theme_mode_override": "SYSTEM",
                      "support_contact": "+91-9876543210"
                    }
                """.trimIndent()
            )

            val existingCfg = AppConfigTable
                .selectAll()
                .map { it[AppConfigTable.key] }
                .toSet()
            appDefaults.forEach { (k, v) ->
                if (k !in existingCfg) {
                    AppConfigTable.insert {
                        it[key] = k
                        it[value] = v
                        it[updatedAt] = Instant.now()
                    }
                }
            }

            // -------------------------------------------------------------
            // Parent ecosystem CMS defaults (parent_api_spec.artifact.md).
            //
            // All UI strings, statistics, and configurations for the Parent
            // module are backend-driven. The keys below match what the
            // routes in feature.parent / feature.content.SupportRouting
            // look up at runtime; defaults are used as fallbacks if a key
            // is ever missing in production.
            //
            // Idempotent: inserted only when the key does not already exist.
            // -------------------------------------------------------------
            val parentDefaults = mapOf(
                // Onboarding Step 1
                "parent_onboarding_step1_screen_config" to """
                    {
                      "header_title": "Let's build a profile for your child.",
                      "header_subtitle": "We use this information to curate the best learning path.",
                      "progress_label": "Step 1 of 3",
                      "progress_value": 0.33
                    }
                """.trimIndent(),
                "parent_available_grades" to """["Grade 1","Grade 2","Grade 3","Nursery","KG"]""",
                "parent_available_interests" to """["Music","Art","STEM","Sports","Languages"]""",
                "parent_onboarding_footer_text" to "\"Your data is encrypted and secure.\"",

                // Onboarding Step 2
                "parent_available_boards" to """["CBSE","ICSE","IB","State Board"]""",
                "parent_available_focus_areas" to """
                    [
                      {"id":"acad","title":"Academics","icon":"school"},
                      {"id":"sports","title":"Sports","icon":"sports_soccer"}
                    ]
                """.trimIndent(),
                "parent_budget_config" to """
                    {
                      "min_value": 0,
                      "max_value": 10000,
                      "default_range": [2000, 5000],
                      "currency_symbol": "$"
                    }
                """.trimIndent(),

                // Dashboard
                "parent_dashboard_curation_logic" to "\"Curation aligned with NEP 2020 developmental milestones.\"",
                "parent_dashboard_info_alerts" to """
                    [
                      {"id":"ptm_upcoming","title":"Upcoming PTM","value":"Nov 25","type":"INFO"}
                    ]
                """.trimIndent(),

                // Track Progress
                "parent_track_journey_description" to "\"On track for next grade transition\"",
                "parent_track_academic_label" to "\"NEP ALIGNED\"",
                "parent_track_badges" to """
                    [
                      {"title":"Social Star","icon":"workspace_premium","is_locked":false,"colors":["#B6C7EB","#006C49"]},
                      {"title":"Math Whiz","icon":"calculate","is_locked":true,"colors":["#FFD580","#B26A00"]}
                    ]
                """.trimIndent(),
                "parent_track_academic_competencies" to """
                    [
                      {"title":"Literacy","progress":0.85,"icon":"translate"},
                      {"title":"Numeracy","progress":0.78,"icon":"calculate"},
                      {"title":"Creativity","progress":0.72,"icon":"palette"}
                    ]
                """.trimIndent(),
                "parent_track_ei_description" to "\"Significant growth in Social Interaction this month.\"",
                "parent_track_ei_metrics" to """{"Empathy":0.8,"Resilience":0.7,"Social":0.9}""",
                "parent_track_play_discovery" to """
                    [
                      {"title":"Agility","description":"Gross motor met","status":"MET"},
                      {"title":"Curiosity","description":"Exploration in progress","status":"IN_PROGRESS"}
                    ]
                """.trimIndent(),

                // Fees CMS
                "parent_fees_announcements" to """
                    [
                      {"id":"f1","title":"Deadline","time":"2h ago","desc":"Submit Q3 fees.","type":"Payment"}
                    ]
                """.trimIndent(),

                // Support / Drawer
                "parent_support_config" to """
                    {
                      "support_contact": "+91-9876543210",
                      "categories": ["TECHNICAL","ACADEMIC","ADMISSIONS","FEES"],
                      "help_center_url": "https://vidyaprayag.com/help"
                    }
                """.trimIndent()
            )

            val existingCfg2 = AppConfigTable
                .selectAll()
                .map { it[AppConfigTable.key] }
                .toSet()
            parentDefaults.forEach { (k, v) ->
                if (k !in existingCfg2) {
                    AppConfigTable.insert {
                        it[key] = k
                        it[value] = v
                        it[updatedAt] = Instant.now()
                    }
                }
            }
        }
    }
}
