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
                        "icon_url": "https://cdn.vidyaprayag.com/icons/intel.png",
                        "heading": "Next-Gen Intelligence",
                        "description": "Proprietary systems powering the ecosystem.",
                        "is_live": true
                      }
                    ]
                """.trimIndent(),
                "list_of_portals" to """
                    [
                      {
                        "icon_url": "https://cdn.vidyaprayag.com/icons/parent.png",
                        "heading": "Parent Portal",
                        "description": "Monitor your child's holistic growth.",
                        "is_live": true
                      },
                      {
                        "icon_url": "https://cdn.vidyaprayag.com/icons/admin.png",
                        "heading": "Admin Portal",
                        "description": "Manage institutional performance and analytics.",
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
        }
    }
}
