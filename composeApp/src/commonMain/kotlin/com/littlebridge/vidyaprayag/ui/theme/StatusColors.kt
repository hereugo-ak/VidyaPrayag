/*
 * File: StatusColors.kt  (commonMain)
 * Module: ui.theme
 *
 * Single source of truth for the SEMANTIC status colors used across the admin
 * (school) dashboards — warning/amber, info/blue, critical/rose, success, and
 * WhatsApp brand green. Previously these were sprinkled as raw Color(0xFF…)
 * "magic" literals inside individual screens (a maintenance hazard and a
 * source of subtle inconsistency between screens).
 *
 * Centralising them here means:
 *   - every "warning" looks identical everywhere,
 *   - one edit re-themes the whole app,
 *   - no hardcoded hex values buried in screen code.
 *
 * Access via `StatusColors.warning`, `StatusColors.warningSoft`, etc.
 * These are intentionally fixed brand-tuned values (status semantics shouldn't
 * shift with light/dark), but soft variants are derived with alpha so they sit
 * correctly on any surface.
 */
package com.littlebridge.vidyaprayag.ui.theme

import androidx.compose.ui.graphics.Color

object StatusColors {
    /** Amber — "needs attention", delayed, moderate risk, below-target. */
    val warning = Color(0xFFF59E0B)
    val warningStrong = Color(0xFFCA8A04)
    val warningSoft = Color(0xFFFFF8E1)

    /** Blue — informational, "meeting expectations", neutral metrics. */
    val info = Color(0xFF3B82F6)
    val infoStrong = Color(0xFF2563EB)
    val infoLight = Color(0xFF60A5FA)

    /** Rose / red — critical, high-risk, errors that aren't M3 errorContainer. */
    val critical = Color(0xFFE11D48)
    val criticalSoft = Color(0xFFFFEBEE)
    val criticalSurface = Color(0xFFFEF2F2)
    val criticalBorder = Color(0xFFFEE2E2)

    /** Gold — highlights, AI insights, premium accents. */
    val gold = Color(0xFFFBBF24)
    val goldBright = Color(0xFFFACC15)

    /** Brand greens for third-party channels. */
    val whatsApp = Color(0xFF25D366)
}
