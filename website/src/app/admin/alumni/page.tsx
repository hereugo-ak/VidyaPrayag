"use client";

import { useState } from "react";
import Link from "next/link";
import useSWR from "swr";
import { adminApi } from "@/lib/admin/client";
import { ApiError } from "@/lib/api";
import type { AlumniDto, AlumniCampaignDto, AlumniDonationDto, AlumniAnalyticsDto, AlumniMentorshipDto, AlumniMentorshipRequestDto } from "@/lib/admin/types";
import { Card, EmptyState, FadeIn, Badge } from "@/components/admin/Primitives";
import { IconAlumni, IconSearch, IconPlus, IconCheck, IconClose } from "@/components/admin/icons";

type Tab = "directory" | "pending" | "campaigns" | "donations" | "mentorship" | "analytics";

export default function AlumniPage() {
  const [tab, setTab] = useState<Tab>("directory");
  const [q, setQ] = useState("");

  return (
    <div className="space-y-5">
      <FadeIn>
        <div className="flex items-center gap-3">
          <IconAlumni className="text-navy-deep" />
          <h1 className="text-2xl font-bold text-navy-deep">Alumni Management</h1>
        </div>
      </FadeIn>

      {/* Tab switch */}
      <div className="inline-flex rounded-xl border border-navy/10 bg-white/70 p-1">
        {(["directory", "pending", "campaigns", "donations", "mentorship", "analytics"] as Tab[]).map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => setTab(t)}
            className={`rounded-lg px-4 py-2 text-[13.5px] font-semibold capitalize transition-colors duration-200 ${
              tab === t ? "bg-navy-deep text-white" : "text-ink-2 hover:text-navy-deep"
            }`}
          >
            {t}
          </button>
        ))}
      </div>

      {tab === "directory" && <DirectoryTab q={q} setQ={setQ} />}
      {tab === "pending" && <PendingTab />}
      {tab === "campaigns" && <CampaignsTab />}
      {tab === "donations" && <DonationsTab />}
      {tab === "mentorship" && <MentorshipTab />}
      {tab === "analytics" && <AnalyticsTab />}
    </div>
  );
}

// ─────────────────────────── Directory ───────────────────────────

function DirectoryTab({ q, setQ }: { q: string; setQ: (v: string) => void }) {
  const { data, error, isLoading } = useSWR(
    ["alumni-list", q],
    () => adminApi.alumniList({ q: q || undefined, limit: 50 })
  );

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 rounded-xl border border-navy/10 bg-white px-3 py-2">
        <IconSearch className="text-ink-2" />
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Search by name, profession, company, city…"
          className="flex-1 bg-transparent text-[14px] text-ink outline-none placeholder:text-ink-2/50"
        />
      </div>

      {isLoading && <div className="h-40 animate-pulse rounded-2xl bg-navy/5" />}
      {error && <ErrorCard error={error} />}
      {data && (
        <>
          <p className="text-[13px] text-ink-2">{data.total} alumni</p>
          {data.alumni.length === 0 ? (
            <EmptyState title="No alumni found" hint="Try a different search or add alumni manually." />
          ) : (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {data.alumni.map((a) => (
                <AlumniCard key={a.id} alumni={a} />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}

function AlumniCard({ alumni }: { alumni: AlumniDto }) {
  return (
    <Link href={`/admin/alumni/${alumni.id}`}>
      <Card className="space-y-2" hover>
        <div className="flex items-start justify-between">
          <div>
            <p className="font-semibold text-ink">{alumni.name}</p>
            <p className="text-[13px] text-ink-2">Batch {alumni.graduationYear}</p>
          </div>
          {alumni.isFeatured && <Badge tone="accent">★ Featured</Badge>}
        </div>
        {alumni.currentProfession && (
          <p className="text-[13px] text-ink-2">
            {alumni.currentProfession}
            {alumni.company ? ` @ ${alumni.company}` : ""}
          </p>
        )}
        {alumni.city && <p className="text-[13px] text-ink-2">{alumni.city}</p>}
        {alumni.isMentor && <Badge tone="success">Mentor{alumni.mentorExpertise ? ` — ${alumni.mentorExpertise}` : ""}</Badge>}
        {alumni.verificationStatus !== "approved" && (
          <Badge tone="warning">{alumni.verificationStatus}</Badge>
        )}
      </Card>
    </Link>
  );
}

// ─────────────────────────── Pending ───────────────────────────

function PendingTab() {
  const { data, error, isLoading, mutate } = useSWR("alumni-pending", () => adminApi.alumniPending());

  const handleAction = async (id: string, action: "approve" | "decline") => {
    try {
      await adminApi.alumniVerify(id, action);
      mutate();
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div className="space-y-4">
      {isLoading && <div className="h-40 animate-pulse rounded-2xl bg-navy/5" />}
      {error && <ErrorCard error={error} />}
      {data && data.length === 0 && <EmptyState title="No pending verifications" hint="All alumni registrations have been reviewed." />}
      {data && data.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2">
          {data.map((a) => (
            <Card key={a.id} className="space-y-3">
              <div>
                <p className="font-semibold text-ink">{a.name}</p>
                <p className="text-[13px] text-ink-2">Batch {a.graduationYear}</p>
                {a.email && <p className="text-[13px] text-ink-2">{a.email}</p>}
                {a.phone && <p className="text-[13px] text-ink-2">{a.phone}</p>}
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => handleAction(a.id, "approve")}
                  className="inline-flex items-center gap-1.5 rounded-lg bg-green-600 px-3 py-1.5 text-[13px] font-semibold text-white hover:bg-green-700"
                >
                  <IconCheck className="h-4 w-4" /> Approve
                </button>
                <button
                  onClick={() => handleAction(a.id, "decline")}
                  className="inline-flex items-center gap-1.5 rounded-lg bg-red-600 px-3 py-1.5 text-[13px] font-semibold text-white hover:bg-red-700"
                >
                  <IconClose className="h-4 w-4" /> Decline
                </button>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

// ─────────────────────────── Campaigns ───────────────────────────

function CampaignsTab() {
  const { data, error, isLoading } = useSWR("alumni-campaigns", () => adminApi.alumniCampaigns());

  return (
    <div className="space-y-4">
      {isLoading && <div className="h-40 animate-pulse rounded-2xl bg-navy/5" />}
      {error && <ErrorCard error={error} />}
      {data && data.length === 0 && <EmptyState title="No campaigns yet" hint="Create a donation campaign to engage alumni." />}
      {data && data.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2">
          {data.map((c) => (
            <CampaignCard key={c.id} campaign={c} />
          ))}
        </div>
      )}
    </div>
  );
}

function CampaignCard({ campaign }: { campaign: AlumniCampaignDto }) {
  const progress = campaign.targetAmount > 0
    ? Math.round((campaign.amountRaised / campaign.targetAmount) * 100)
    : 0;
  return (
    <Card className="space-y-2">
      <p className="font-semibold text-ink">{campaign.title}</p>
      {campaign.description && <p className="text-[13px] text-ink-2 line-clamp-2">{campaign.description}</p>}
      <div className="flex items-center justify-between text-[13px] text-ink-2">
        <span>₹{campaign.amountRaised.toLocaleString()} / ₹{campaign.targetAmount.toLocaleString()} ({progress}%)</span>
        <span>{campaign.donorCount} donors</span>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-navy/10">
        <div className="h-full rounded-full bg-navy-deep transition-all" style={{ width: `${Math.min(progress, 100)}%` }} />
      </div>
      <Badge tone={campaign.status === "active" ? "success" : "warning"}>{campaign.status}</Badge>
    </Card>
  );
}

// ─────────────────────────── Donations ───────────────────────────

function DonationsTab() {
  const { data, error, isLoading } = useSWR("alumni-donations", () => adminApi.alumniDonations());

  return (
    <div className="space-y-4">
      {isLoading && <div className="h-40 animate-pulse rounded-2xl bg-navy/5" />}
      {error && <ErrorCard error={error} />}
      {data && data.length === 0 && <EmptyState title="No donations recorded" hint="Log donations from the alumni detail view." />}
      {data && data.length > 0 && (
        <div className="overflow-hidden rounded-2xl border border-navy/10 bg-white">
          <table className="w-full text-left text-[13.5px]">
            <thead className="border-b border-navy/10 bg-navy/3">
              <tr>
                <th className="px-4 py-3 font-semibold text-ink-2">Alumni</th>
                <th className="px-4 py-3 font-semibold text-ink-2">Amount</th>
                <th className="px-4 py-3 font-semibold text-ink-2">Date</th>
                <th className="px-4 py-3 font-semibold text-ink-2">Campaign</th>
                <th className="px-4 py-3 font-semibold text-ink-2">80G</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-navy/5">
              {data.map((d) => (
                <tr key={d.id} className="hover:bg-navy/2">
                  <td className="px-4 py-3 font-medium text-ink">{d.alumniName}</td>
                  <td className="px-4 py-3 text-ink">₹{d.amount.toLocaleString()}</td>
                  <td className="px-4 py-3 text-ink-2">{d.donationDate}</td>
                  <td className="px-4 py-3 text-ink-2">{d.campaignTitle ?? "—"}</td>
                  <td className="px-4 py-3">
                    {d.is80gEligible ? (
                      <Badge tone="success">Yes · {d.receiptNumber ?? "Pending"}</Badge>
                    ) : (
                      <span className="text-ink-2">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

// ─────────────────────────── Analytics ───────────────────────────

function AnalyticsTab() {
  const { data, error, isLoading } = useSWR("alumni-analytics", () => adminApi.alumniAnalytics());

  return (
    <div className="space-y-4">
      {isLoading && <div className="h-40 animate-pulse rounded-2xl bg-navy/5" />}
      {error && <ErrorCard error={error} />}
      {data && <AnalyticsGrid data={data} />}
    </div>
  );
}

function AnalyticsGrid({ data }: { data: AlumniAnalyticsDto }) {
  const stats = [
    { label: "Total Alumni", value: data.totalAlumni },
    { label: "Active (90 days)", value: data.activeAlumni },
    { label: "Pending Verifications", value: data.pendingVerifications },
    { label: "Engagement Rate", value: `${(data.engagementRate).toFixed(1)}%` },
    { label: "Total Donations", value: `₹${data.totalDonations.toLocaleString()}` },
    { label: "Active Campaigns", value: data.activeCampaigns },
    { label: "Active Mentorships", value: data.activeMentorships },
    { label: "Pending Mentorship Requests", value: data.mentorshipRequestsPending },
  ];

  return (
    <>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((s) => (
          <Card key={s.label} className="space-y-1">
            <p className="text-[13px] text-ink-2">{s.label}</p>
            <p className="text-2xl font-bold text-navy-deep">{s.value}</p>
          </Card>
        ))}
      </div>

      {Object.keys(data.byGraduationYear).length > 0 && (
        <div>
          <h3 className="mb-2 text-[15px] font-semibold text-ink">By Graduation Year</h3>
          <div className="grid gap-2 sm:grid-cols-3 lg:grid-cols-6">
            {Object.entries(data.byGraduationYear).map(([year, count]) => (
              <Card key={year} className="flex items-center justify-between">
                <span className="text-[13px] text-ink-2">{year}</span>
                <span className="font-semibold text-ink">{count}</span>
              </Card>
            ))}
          </div>
        </div>
      )}

      {Object.keys(data.byProfession).length > 0 && (
        <div>
          <h3 className="mb-2 text-[15px] font-semibold text-ink">By Profession</h3>
          <div className="grid gap-2 sm:grid-cols-3 lg:grid-cols-4">
            {Object.entries(data.byProfession).map(([prof, count]) => (
              <Card key={prof} className="flex items-center justify-between">
                <span className="text-[13px] text-ink-2">{prof}</span>
                <span className="font-semibold text-ink">{count}</span>
              </Card>
            ))}
          </div>
        </div>
      )}

      {Object.keys(data.byCity).length > 0 && (
        <div>
          <h3 className="mb-2 text-[15px] font-semibold text-ink">By City</h3>
          <div className="grid gap-2 sm:grid-cols-3 lg:grid-cols-4">
            {Object.entries(data.byCity).map(([city, count]) => (
              <Card key={city} className="flex items-center justify-between">
                <span className="text-[13px] text-ink-2">{city}</span>
                <span className="font-semibold text-ink">{count}</span>
              </Card>
            ))}
          </div>
        </div>
      )}
    </>
  );
}

// ─────────────────────────── Mentorship ───────────────────────────

function MentorshipTab() {
  const { data: mentorships, error: mErr, isLoading: mLoading } = useSWR("alumni-mentorships", () => adminApi.alumniMentorships());
  const { data: requests, error: rErr, isLoading: rLoading } = useSWR("alumni-mentorship-requests", () => adminApi.alumniMentorshipRequests());

  return (
    <div className="space-y-6">
      {/* Active mentorships */}
      <div className="space-y-3">
        <h3 className="text-[15px] font-semibold text-ink">Active Mentorships</h3>
        {mLoading && <div className="h-32 animate-pulse rounded-2xl bg-navy/5" />}
        {mErr && <ErrorCard error={mErr} />}
        {mentorships && mentorships.length === 0 && <EmptyState title="No active mentorships" hint="Mentorships will appear here once alumni start mentoring students." />}
        {mentorships && mentorships.length > 0 && (
          <div className="grid gap-3 sm:grid-cols-2">
            {mentorships.map((m) => (
              <Card key={m.id} className="space-y-1">
                <div className="flex items-center justify-between">
                  <p className="font-semibold text-ink">{m.alumniName}</p>
                  <Badge tone={m.status === "active" ? "success" : "neutral"}>{m.status}</Badge>
                </div>
                <p className="text-[13px] text-ink-2">Mentoring: {m.studentName}</p>
                <p className="text-[13px] text-ink-2">Started: {m.startDate}</p>
                {m.sessionCount > 0 && <p className="text-[13px] text-ink-2">Sessions: {m.sessionCount}</p>}
                {m.notes && <p className="text-[13px] text-ink-2">{m.notes}</p>}
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Pending requests */}
      <div className="space-y-3">
        <h3 className="text-[15px] font-semibold text-ink">Mentorship Requests</h3>
        {rLoading && <div className="h-32 animate-pulse rounded-2xl bg-navy/5" />}
        {rErr && <ErrorCard error={rErr} />}
        {requests && requests.length === 0 && <EmptyState title="No mentorship requests" hint="Student requests for alumni mentorship will appear here." />}
        {requests && requests.length > 0 && (
          <div className="grid gap-3 sm:grid-cols-2">
            {requests.map((r) => (
              <Card key={r.id} className="space-y-1">
                <div className="flex items-center justify-between">
                  <p className="font-semibold text-ink">{r.alumniName}</p>
                  <Badge tone={r.status === "pending" ? "warning" : "neutral"}>{r.status}</Badge>
                </div>
                <p className="text-[13px] text-ink-2">From: {r.studentName}</p>
                <p className="text-[13px] text-ink-2">Requested by: {r.requestedByName}</p>
                {r.expertiseArea && <p className="text-[13px] text-ink-2">Expertise: {r.expertiseArea}</p>}
                {r.message && <p className="text-[13px] text-ink-2">{r.message}</p>}
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ─────────────────────────── Shared ───────────────────────────

function ErrorCard({ error }: { error: unknown }) {
  const msg = error instanceof ApiError ? error.message : "Something went wrong.";
  return <Card className="border-red-200 bg-red-50"><p className="text-[14px] text-red-700">{msg}</p></Card>;
}
