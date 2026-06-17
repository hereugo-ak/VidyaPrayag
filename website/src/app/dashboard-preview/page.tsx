"use client";

/**
 * DESIGN PREVIEW HARNESS — dev-only, NOT linked in nav, robots-noindex.
 *
 * Renders the real admin chrome + dashboard workspace against fixture data
 * (previewFixtures) injected through SWR's `fallback`, with a seeded fake
 * session so the auth-gated components mount. This exists purely so the
 * dashboard composition can be designed and screenshot-reviewed without a live
 * Ktor backend. Production uses /admin/dashboard with the real authed hooks.
 */

import { Suspense, useEffect, useState } from "react";
import { SWRConfig } from "swr";
import { AdminAuthProvider } from "@/lib/admin/session";
import { Sidebar } from "@/components/admin/Sidebar";
import { Topbar } from "@/components/admin/Topbar";
import { previewFallback } from "@/lib/admin/previewFixtures";
import { DashboardWorkspace } from "@/components/admin/DashboardWorkspace";

export default function DashboardPreviewPage() {
  const [seeded, setSeeded] = useState(false);

  useEffect(() => {
    // Seed a fake admin session so useAdminAuth() resolves a name for the greeting.
    window.localStorage.setItem(
      "enrollplus.admin.v1",
      JSON.stringify({
        token: "preview",
        refreshToken: "preview",
        userId: "preview",
        name: "Rakesh Nair",
        role: "school_admin",
      }),
    );
    setSeeded(true);
  }, []);

  if (!seeded) return null;

  return (
    <SWRConfig
      value={{
        fallback: previewFallback(),
        // Don't hit the network in the harness; fixtures are the source.
        fetcher: async () => {
          throw new Error("preview: no network");
        },
        revalidateOnFocus: false,
        revalidateOnReconnect: false,
        revalidateIfStale: false,
        shouldRetryOnError: false,
      }}
    >
      <AdminAuthProvider>
        <div className="admin-canvas min-h-screen">
          <Sidebar open={false} onClose={() => {}} />
          <div className="lg:pl-[264px]">
            <Topbar title="Dashboard" onMenu={() => {}} />
            <main className="mx-auto w-full max-w-[1340px] px-4 py-6 md:px-8 md:py-9">
              <Suspense fallback={<div className="h-40" />}>
                <DashboardWorkspace />
              </Suspense>
            </main>
          </div>
        </div>
      </AdminAuthProvider>
    </SWRConfig>
  );
}
