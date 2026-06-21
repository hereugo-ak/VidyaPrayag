"use client";

import { useState } from "react";
import { mutate } from "swr";
import { useLeaveRequests } from "@/lib/admin/hooks";
import { adminApi } from "@/lib/admin/client";
import { ApiError } from "@/lib/api";
import type { LeaveRequestDto } from "@/lib/admin/types";
import { pct } from "@/lib/admin/format";
import { Card, CardHeader, EmptyState, FadeIn, Skeleton, Badge, Avatar } from "@/components/admin/Primitives";
import { StatTile } from "@/components/admin/StatTile";
import { Toolbar, AdminButton } from "@/components/admin/Toolbar";
import { IconLeave, IconCheck, IconClose } from "@/components/admin/icons";

const STATUS_FILTERS = [
  { value: "Pending", label: "Pending" },
  { value: "Approved", label: "Approved" },
  { value: "Rejected", label: "Rejected" },
];

function statusTone(s: string): "success" | "warning" | "danger" | "neutral" {
  const v = s.toLowerCase();
  if (v === "approved") return "success";
  if (v === "rejected") return "danger";
  if (v === "pending") return "warning";
  return "neutral";
}

export default function LeavePage() {
  const [status, setStatus] = useState("Pending");
  const [q, setQ] = useState("");
  const cacheKey = ["leave", "", status] as const;
  const { data, isLoading } = useLeaveRequests(undefined, status);

  const requests = (data?.requests ?? []).filter((r) => {
    const needle = q.trim().toLowerCase();
    return !needle || r.requester_name.toLowerCase().includes(needle) || r.reason.toLowerCase().includes(needle);
  });

  const [busyId, setBusyId] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  async function setLeave(r: LeaveRequestDto, next: "Approved" | "Rejected") {
    setErr(null);
    setBusyId(r.id);
    try {
      await adminApi.setLeaveStatus(r.id, next);
      await mutate(cacheKey);
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Could not update this request.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-3">
        <FadeIn>
          <StatTile label="Approval rate" value={pct(data?.approval_rate ?? 0)} caption="Recent decisions" loading={isLoading && !data} accent />
        </FadeIn>
        <FadeIn delay={0.04}>
          <StatTile label="This week" value={(data?.weekly_count ?? 0).toLocaleString("en-IN")} caption="Requests received" loading={isLoading && !data} />
        </FadeIn>
        <FadeIn delay={0.08}>
          <StatTile label="Showing" value={(requests.length ?? 0).toLocaleString("en-IN")} caption={`${status} requests`} loading={isLoading && !data} />
        </FadeIn>
      </div>

      <FadeIn delay={0.06}>
        <Card>
          <div className="border-b border-navy/8 p-4">
            <Toolbar
              query={q}
              onQuery={setQ}
              placeholder="Search name or reason…"
              filters={STATUS_FILTERS}
              active={status}
              onFilter={(v) => setStatus(v || "Pending")}
            />
          </div>

          {err && <p className="px-5 pt-3 text-[13px] font-medium text-danger">{err}</p>}

          {isLoading && !data ? (
            <div className="space-y-2 p-4">
              {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-16" />)}
            </div>
          ) : requests.length === 0 ? (
            <EmptyState
              icon={<IconLeave width={26} height={26} />}
              title={`No ${status.toLowerCase()} requests`}
              hint="Leave requests from parents and teachers appear here for review."
            />
          ) : (
            <ul className="divide-y divide-navy/6">
              {requests.map((r) => (
                <li key={r.id} className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex items-start gap-3">
                    <Avatar name={r.requester_name} size={36} />
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="font-semibold text-navy-deep">{r.requester_name}</p>
                        <Badge tone="neutral">{r.requester_role}</Badge>
                        <Badge tone={statusTone(r.status)}>{r.status}</Badge>
                      </div>
                      <p className="mt-0.5 text-[13px] text-ink-2">{r.reason}</p>
                      <p className="mt-0.5 text-[12px] text-ink-3">{r.date_range}</p>
                    </div>
                  </div>

                  {r.status.toLowerCase() === "pending" && (
                    <div className="flex shrink-0 gap-2">
                      <AdminButton variant="ghost" onClick={() => setLeave(r, "Rejected")} disabled={busyId === r.id}>
                        <IconClose width={15} height={15} /> Reject
                      </AdminButton>
                      <AdminButton onClick={() => setLeave(r, "Approved")} disabled={busyId === r.id}>
                        <IconCheck width={15} height={15} /> Approve
                      </AdminButton>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </Card>
      </FadeIn>
    </div>
  );
}
