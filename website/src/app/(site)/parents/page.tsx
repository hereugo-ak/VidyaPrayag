import type { Metadata } from "next";
import { ParentsHero } from "@/components/parents/ParentsHero";
import { ParentsPillars } from "@/components/parents/ParentsPillars";
import { ParentsAppPreview } from "@/components/parents/ParentsAppPreview";

export const metadata: Metadata = {
  title: "For Parents",
  description:
    "Enroll+ for parents, the calm, real-time window into your child's school day. Attendance the moment it's marked, results the day they're published, fees without the queue, and messages from the people who actually teach your child. Parents portal in Enroll+ app, coming soon.",
  alternates: { canonical: "/parents" },
};

/**
 * /parents, the dedicated, ultra-premium parents experience. It mirrors the
 * site's editorial system (lavender canvas, single-bloom auroras, restrained
 * motion) but tells the parent story on its own page rather than as a homepage
 * section. Composition: a parent-first hero with the coming-soon download
 * control, the four-pillar feature block, then the ultra-premium "Parents
 * portal in Enroll+ app" preview. Intentionally NO repetitive trust-band and
 * NO duplicated download CTA, the homepage parent section has been removed in
 * favour of this page.
 */
export default function ParentsPage() {
  return (
    <>
      <ParentsHero />
      <ParentsPillars />
      <ParentsAppPreview />
    </>
  );
}
