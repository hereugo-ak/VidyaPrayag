"use client";

import { Suspense } from "react";
import { DashboardWorkspace } from "@/components/admin/DashboardWorkspace";

/**
 * Command Center — the school-admin home, a multi-tab WORKSPACE (not a single
 * scrolling page). The composition and all data wiring live in
 * DashboardWorkspace; this route is only the Suspense boundary that
 * useSearchParams (inside useDashboardUrlState) requires under the App Router.
 */
export default function DashboardPage() {
  return (
    <Suspense fallback={<div className="h-40" />}>
      <DashboardWorkspace />
    </Suspense>
  );
}
