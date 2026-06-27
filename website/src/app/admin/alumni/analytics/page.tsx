"use client";

import useSWR from "swr";
import { adminApi } from "@/lib/admin/client";
import { Card, EmptyState, FadeIn } from "@/components/admin/Primitives";
import { IconAlumni } from "@/components/admin/icons";
import type { AlumniAnalyticsDto } from "@/lib/admin/types";

export default function AlumniAnalyticsPage() {
  const { data: analytics, error, isLoading } = useSWR<AlumniAnalyticsDto>("alumni-analytics", () => adminApi.alumniAnalytics());

  return (
    <div className="space-y-5">
      <FadeIn>
        <div className="flex items-center gap-3">
          <IconAlumni className="text-navy-deep" />
          <h1 className="text-2xl font-bold text-navy-deep">Alumni Analytics</h1>
        </div>
      </FadeIn>

      {isLoading && <Card><div className="p-8 text-center text-ink-2">Loading analytics…</div></Card>}
      {error && <Card><div className="p-8 text-center text-red-500">{String(error)}</div></Card>}
      {!analytics && !isLoading && !error && (
        <Card><EmptyState title="No analytics data" hint="Analytics will appear once alumni records exist." /></Card>
      )}

      {analytics && (
        <FadeIn>
          {/* Overview stats */}
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard label="Total Alumni" value={analytics.totalAlumni} />
            <StatCard label="Active (90 days)" value={analytics.activeAlumni} />
            <StatCard label="Pending Verifications" value={analytics.pendingVerifications} />
            <StatCard label="Engagement Rate" value={`${(analytics.engagementRate * 100).toFixed(1)}%`} />
            <StatCard label="Total Donations" value={`₹${analytics.totalDonations.toLocaleString("en-IN")}`} />
            <StatCard label="Donation Count" value={analytics.donationCount} />
            <StatCard label="Active Campaigns" value={analytics.activeCampaigns} />
            <StatCard label="Active Mentorships" value={analytics.activeMentorships} />
          </div>

          {/* Breakdowns */}
          <div className="mt-5 grid gap-5 lg:grid-cols-3">
            <BreakdownCard title="By Graduation Year" data={analytics.byGraduationYear} />
            <BreakdownCard title="By Profession" data={analytics.byProfession} />
            <BreakdownCard title="By City" data={analytics.byCity} />
          </div>

          {/* Mentorship requests */}
          {analytics.mentorshipRequestsPending > 0 && (
            <div className="mt-5">
              <Card>
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="text-lg font-bold text-navy-deep">Pending Mentorship Requests</h2>
                    <p className="text-sm text-ink-2">Students waiting for alumni mentor matching</p>
                  </div>
                  <div className="text-3xl font-bold text-amber-600">{analytics.mentorshipRequestsPending}</div>
                </div>
              </Card>
            </div>
          )}
        </FadeIn>
      )}
    </div>
  );
}

function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <Card>
      <div className="text-sm text-ink-2">{label}</div>
      <div className="mt-1 text-2xl font-bold text-navy-deep">{value}</div>
    </Card>
  );
}

function BreakdownCard({ title, data }: { title: string; data: Record<string, number> }) {
  const entries = Object.entries(data).sort((a, b) => b[1] - a[1]);
  const max = Math.max(...entries.map((e) => e[1]), 1);

  return (
    <Card>
      <h2 className="mb-4 text-lg font-bold text-navy-deep">{title}</h2>
      {entries.length === 0 ? (
        <p className="text-sm text-ink-2">No data</p>
      ) : (
        <div className="space-y-3">
          {entries.slice(0, 10).map(([key, count]) => (
            <div key={key}>
              <div className="flex justify-between text-sm">
                <span className="text-navy-deep">{key}</span>
                <span className="font-semibold text-ink-2">{count}</span>
              </div>
              <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-navy/5">
                <div className="h-full rounded-full bg-navy-deep/60" style={{ width: `${(count / max) * 100}%` }} />
              </div>
            </div>
          ))}
          {entries.length > 10 && <p className="text-xs text-ink-2">+ {entries.length - 10} more</p>}
        </div>
      )}
    </Card>
  );
}
