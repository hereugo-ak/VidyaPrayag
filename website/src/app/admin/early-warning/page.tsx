"use client";

import { PewsWorkspace } from "@/components/admin/pews/PewsWorkspace";

/**
 * /admin/early-warning — the web School-Admin home for PEWS (the Predictive
 * Early Warning System). Previously the web portal had NO PEWS surface at all:
 * an admin only saw the legacy rule-based early-warning card that dead-ended at
 * an announcement. This page brings the full loop to the web — risk bands, the
 * filterable at-risk cohort, per-student drill-down (signals + AI explanation +
 * interventions with Improved/No change/Dismiss), the effectiveness rollup, and
 * configuration — matching (and extending) the mobile admin screens.
 */
export default function EarlyWarningPage() {
  return <PewsWorkspace />;
}
