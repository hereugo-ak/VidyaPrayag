"use client";

import { TutorWorkspace } from "@/components/admin/tutor/TutorWorkspace";
import { Topbar } from "@/components/admin/Topbar";

export default function TutorPage() {
  return (
    <div className="space-y-6">
      <Topbar title="AI Tutor" />
      <div className="px-1">
        <TutorWorkspace />
      </div>
    </div>
  );
}
