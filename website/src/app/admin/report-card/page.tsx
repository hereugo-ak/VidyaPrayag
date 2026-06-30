"use client";

import { ReportCardWorkspace } from "@/components/admin/reportcard/ReportCardWorkspace";
import { Topbar } from "@/components/admin/Topbar";

export default function ReportCardPage() {
  return (
    <div className="space-y-6">
      <Topbar title="Report Cards" />
      <div className="px-1">
        <ReportCardWorkspace />
      </div>
    </div>
  );
}
