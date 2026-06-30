"use client";

import { useState } from "react";
import useSWR from "swr";
import { adminApi } from "@/lib/admin/client";
import { Card, EmptyState, FadeIn, Badge } from "@/components/admin/Primitives";
import { IconAlumni } from "@/components/admin/icons";
import type { AlumniCampaignDto, AlumniDonationDto } from "@/lib/admin/types";

export default function CampaignsPage() {
  const { data: campaigns, error, isLoading, mutate } = useSWR<AlumniCampaignDto[]>("alumni-campaigns", () => adminApi.alumniCampaigns());
  const [selectedId, setSelectedId] = useState<string | null>(null);

  return (
    <div className="space-y-5">
      <FadeIn>
        <div className="flex items-center gap-3">
          <IconAlumni className="text-navy-deep" />
          <h1 className="text-2xl font-bold text-navy-deep">Donation Campaigns</h1>
        </div>
      </FadeIn>

      {isLoading && <Card><div className="p-8 text-center text-ink-2">Loading campaigns…</div></Card>}
      {error && <Card><div className="p-8 text-center text-red-500">{String(error)}</div></Card>}

      {campaigns && campaigns.length === 0 && (
        <Card><EmptyState title="No campaigns yet" hint="Create a donation campaign from the Alumni tab to engage your alumni network." /></Card>
      )}

      {campaigns && campaigns.length > 0 && (
        <div className="grid gap-5 lg:grid-cols-2">
          {campaigns.map((c) => {
            const progress = c.targetAmount > 0 ? Math.min(100, Math.round((c.amountRaised / c.targetAmount) * 100)) : 0;
            return (
              <Card key={c.id} hover>
                <div className="flex items-start justify-between">
                  <div>
                    <h2 className="text-lg font-bold text-navy-deep">{c.title}</h2>
                    {c.description && <p className="mt-1 text-sm text-ink-2">{c.description}</p>}
                  </div>
                  <Badge tone={c.status === "active" ? "success" : c.status === "paused" ? "warning" : "neutral"}>
                    {c.status}
                  </Badge>
                </div>

                <div className="mt-4">
                  <div className="flex justify-between text-sm">
                    <span className="font-semibold text-navy-deep">₹{c.amountRaised.toLocaleString("en-IN")}</span>
                    <span className="text-ink-2">of ₹{c.targetAmount.toLocaleString("en-IN")}</span>
                  </div>
                  <div className="mt-2 h-2 overflow-hidden rounded-full bg-navy/5">
                    <div className="h-full rounded-full bg-navy-deep transition-all duration-500" style={{ width: `${progress}%` }} />
                  </div>
                  <div className="mt-2 flex items-center justify-between text-xs text-ink-2">
                    <span>{c.donorCount} donors</span>
                    <span>{progress}% funded</span>
                  </div>
                </div>

                {c.targetBatchYear && (
                  <div className="mt-3"><Badge tone="accent">Batch {c.targetBatchYear}</Badge></div>
                )}

                <div className="mt-4 flex gap-2">
                  <button
                    type="button"
                    onClick={() => setSelectedId(selectedId === c.id ? null : c.id)}
                    className="rounded-lg bg-navy/5 px-3 py-1.5 text-sm font-semibold text-navy-deep hover:bg-navy/10"
                  >
                    {selectedId === c.id ? "Hide donations" : "View donations"}
                  </button>
                  {c.status === "active" && (
                    <button
                      type="button"
                      onClick={async () => { await adminApi.alumniCampaignUpdate(c.id, "paused"); mutate(); }}
                      className="rounded-lg bg-amber-50 px-3 py-1.5 text-sm font-semibold text-amber-700 hover:bg-amber-100"
                    >
                      Pause
                    </button>
                  )}
                  {c.status === "paused" && (
                    <button
                      type="button"
                      onClick={async () => { await adminApi.alumniCampaignUpdate(c.id, "active"); mutate(); }}
                      className="rounded-lg bg-green-50 px-3 py-1.5 text-sm font-semibold text-green-700 hover:bg-green-100"
                    >
                      Resume
                    </button>
                  )}
                </div>

                {selectedId === c.id && <CampaignDonations campaignId={c.id} />}
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}

function CampaignDonations({ campaignId }: { campaignId: string }) {
  const { data: donations, isLoading } = useSWR<AlumniDonationDto[]>(`campaign-donations-${campaignId}`, () => adminApi.alumniDonations(campaignId));

  if (isLoading) return <div className="mt-4 p-4 text-center text-sm text-ink-2">Loading donations…</div>;
  if (!donations || donations.length === 0) return <div className="mt-4 p-4 text-center text-sm text-ink-2">No donations for this campaign yet.</div>;

  return (
    <div className="mt-4 space-y-2">
      <h3 className="text-sm font-bold text-navy-deep">Donations</h3>
      {donations.map((d) => (
        <div key={d.id} className="flex items-center justify-between rounded-lg bg-navy/[0.02] px-3 py-2">
          <div>
            <div className="text-sm font-semibold text-navy-deep">{d.alumniName}</div>
            <div className="text-xs text-ink-2">{d.donationDate}</div>
          </div>
          <div className="flex items-center gap-2">
            <span className="font-semibold text-navy-deep">₹{d.amount.toLocaleString("en-IN")}</span>
            {d.is80gEligible && <Badge tone="success">80G</Badge>}
          </div>
        </div>
      ))}
    </div>
  );
}
