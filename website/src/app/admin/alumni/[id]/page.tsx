"use client";

import { use } from "react";
import useSWR from "swr";
import Link from "next/link";
import { adminApi } from "@/lib/admin/client";
import { Card, EmptyState, FadeIn, Badge } from "@/components/admin/Primitives";
import { IconAlumni } from "@/components/admin/icons";
import type { AlumniDto } from "@/lib/admin/types";

export default function AlumniDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const { data: alumni, error, isLoading } = useSWR<AlumniDto>(`alumni-${id}`, () => adminApi.alumniGet(id));

  return (
    <div className="space-y-5">
      <FadeIn>
        <Link href="/admin/alumni" className="text-sm font-semibold text-ink-2 hover:text-navy-deep">
          ← Back to Alumni
        </Link>
      </FadeIn>

      {isLoading && <Card><div className="p-8 text-center text-ink-2">Loading…</div></Card>}
      {error && <Card><div className="p-8 text-center text-red-500">{String(error)}</div></Card>}

      {alumni && (
        <FadeIn>
          <div className="flex items-center gap-3">
            <IconAlumni className="text-navy-deep" />
            <h1 className="text-2xl font-bold text-navy-deep">{alumni.name}</h1>
            <Badge tone={alumni.verificationStatus === "verified" ? "success" : alumni.verificationStatus === "pending" ? "warning" : "danger"}>
              {alumni.verificationStatus}
            </Badge>
            {alumni.isFeatured && <Badge tone="accent">★ Featured</Badge>}
          </div>

          <div className="mt-5 grid gap-5 lg:grid-cols-2">
            {/* Profile */}
            <Card>
              <h2 className="mb-4 text-lg font-bold text-navy-deep">Profile</h2>
              <dl className="space-y-3">
                <div className="flex justify-between">
                  <dt className="text-ink-2">Graduation Year</dt>
                  <dd className="font-semibold text-navy-deep">{alumni.graduationYear}</dd>
                </div>
                {alumni.lastClass && (
                  <div className="flex justify-between">
                    <dt className="text-ink-2">Last Class</dt>
                    <dd className="font-semibold text-navy-deep">{alumni.lastClass}</dd>
                  </div>
                )}
                {alumni.currentProfession && (
                  <div className="flex justify-between">
                    <dt className="text-ink-2">Profession</dt>
                    <dd className="font-semibold text-navy-deep">{alumni.currentProfession}</dd>
                  </div>
                )}
                {alumni.company && (
                  <div className="flex justify-between">
                    <dt className="text-ink-2">Company</dt>
                    <dd className="font-semibold text-navy-deep">{alumni.company}</dd>
                  </div>
                )}
                {alumni.city && (
                  <div className="flex justify-between">
                    <dt className="text-ink-2">City</dt>
                    <dd className="font-semibold text-navy-deep">{alumni.city}</dd>
                  </div>
                )}
                {alumni.email && (
                  <div className="flex justify-between">
                    <dt className="text-ink-2">Email</dt>
                    <dd className="font-semibold text-navy-deep">{alumni.email}</dd>
                  </div>
                )}
                {alumni.phone && (
                  <div className="flex justify-between">
                    <dt className="text-ink-2">Phone</dt>
                    <dd className="font-semibold text-navy-deep">{alumni.phone}</dd>
                  </div>
                )}
                {alumni.linkedinUrl && (
                  <div className="flex justify-between">
                    <dt className="text-ink-2">LinkedIn</dt>
                    <dd><a href={alumni.linkedinUrl} target="_blank" rel="noreferrer" className="font-semibold text-blue-600 hover:underline">Profile</a></dd>
                  </div>
                )}
                {alumni.skills && (
                  <div className="flex justify-between">
                    <dt className="text-ink-2">Skills</dt>
                    <dd className="text-right font-semibold text-navy-deep">{alumni.skills}</dd>
                  </div>
                )}
                {alumni.achievements && (
                  <div className="flex justify-between">
                    <dt className="text-ink-2">Achievements</dt>
                    <dd className="text-right font-semibold text-navy-deep">{alumni.achievements}</dd>
                  </div>
                )}
                {alumni.isMentor && (
                  <div className="flex justify-between">
                    <dt className="text-ink-2">Mentor</dt>
                    <dd className="font-semibold text-navy-deep">Yes{alumni.mentorExpertise ? ` — ${alumni.mentorExpertise}` : ""}</dd>
                  </div>
                )}
              </dl>
              <div className="mt-4 flex gap-2">
                <Badge tone={alumni.showEmail ? "success" : "neutral"}>Email {alumni.showEmail ? "visible" : "hidden"}</Badge>
                <Badge tone={alumni.showPhone ? "success" : "neutral"}>Phone {alumni.showPhone ? "visible" : "hidden"}</Badge>
                <Badge tone={alumni.showLinkedin ? "success" : "neutral"}>LinkedIn {alumni.showLinkedin ? "visible" : "hidden"}</Badge>
              </div>
            </Card>

            {/* Career History */}
            <Card>
              <h2 className="mb-4 text-lg font-bold text-navy-deep">Career History</h2>
              {alumni.careerHistory && alumni.careerHistory.length > 0 ? (
                <div className="space-y-4">
                  {alumni.careerHistory.map((job) => (
                    <div key={job.id} className="border-l-2 border-navy/10 pl-4">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-navy-deep">{job.jobTitle}</span>
                        {job.isCurrent && <Badge tone="success">Current</Badge>}
                      </div>
                      <div className="text-sm text-ink-2">{job.company}{job.industry ? ` · ${job.industry}` : ""}</div>
                      <div className="text-xs text-ink-2">
                        {job.startDate ?? "—"} → {job.isCurrent ? "Present" : (job.endDate ?? "—")}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <EmptyState title="No career history" hint="Alumni hasn't added career details yet." />
              )}
            </Card>
          </div>

          {/* Donations for this alumni */}
          <div className="mt-5">
            <AlumniDonations alumniId={id} />
          </div>
        </FadeIn>
      )}
    </div>
  );
}

function AlumniDonations({ alumniId }: { alumniId: string }) {
  const { data: donations, isLoading } = useSWR(`alumni-donations-${alumniId}`, () => adminApi.alumniDonations(undefined, alumniId));

  return (
    <Card>
      <h2 className="mb-4 text-lg font-bold text-navy-deep">Donations</h2>
      {isLoading && <div className="p-4 text-center text-ink-2">Loading…</div>}
      {donations && donations.length === 0 && <EmptyState title="No donations" hint="This alumni hasn't made any donations yet." />}
      {donations && donations.length > 0 && (
        <div className="space-y-3">
          {donations.map((d) => (
            <div key={d.id} className="flex items-center justify-between rounded-xl bg-navy/[0.02] px-4 py-3">
              <div>
                <div className="font-semibold text-navy-deep">₹{d.amount.toLocaleString("en-IN")}</div>
                <div className="text-sm text-ink-2">{d.donationDate}{d.campaignTitle ? ` · ${d.campaignTitle}` : ""}</div>
              </div>
              <div className="flex items-center gap-2">
                {d.is80gEligible && <Badge tone="success">80G</Badge>}
                {d.receiptNumber && <Badge tone="neutral">{d.receiptNumber}</Badge>}
              </div>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}
