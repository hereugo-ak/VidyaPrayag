import type { Metadata } from "next";
import {
  SCHOOL_FEATURES,
  PARENT_FEATURES,
  TEACHER_FEATURES,
} from "@/lib/content";
import { PHOTOS } from "@/lib/images";
import { SectionHeading } from "@/components/ui/SectionHeading";
import { PageHeader } from "@/components/ui/PageHeader";
import { Reveal, RevealGroup, RevealItem } from "@/components/ui/Reveal";
import { Photo } from "@/components/ui/Photo";

export const metadata: Metadata = {
  title: "Features | Enroll+",
  description:
    "Everything Enroll+ does for schools, teachers and parents: one roster, targeted broadcasts, attendance and marks that roll up automatically, analytics built on real entries, admissions and PTMs.",
  alternates: { canonical: "/features" },
};

export default function FeaturesPage() {
  return (
    <div>
      {/* Distinct page header */}
      <PageHeader
        eyebrow="The platform"
        crumbs={[{ label: "Home", href: "/" }, { label: "Product" }]}
        title={
          <>
            One system. <span className="text-accent">Three points of view.</span>
          </>
        }
        lede="Enroll+ models a school the way it actually runs, then gives admins, teachers and parents the exact slice they need. Every capability below maps to a real screen in the product."
      />

      {/* For Schools */}
      <section id="schools" className="shell scroll-mt-24 py-20 md:py-28">
        <SectionHeading eyebrow="For schools" title="Run the whole institution from one desk." />
        <RevealGroup className="mt-12 grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {SCHOOL_FEATURES.map((f) => (
            <RevealItem
              key={f.key}
              className="rounded-xl2 border border-navy/8 bg-white/60 p-7"
            >
              <h3 className="text-lg font-extrabold tracking-tight text-navy-deep">{f.title}</h3>
              <p className="mt-3 text-sm leading-relaxed text-ink-2">{f.body}</p>
              <ul className="mt-5 space-y-1.5">
                {f.points.map((p) => (
                  <li key={p} className="flex items-center gap-2 text-sm text-ink-2">
                    <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-accent" />
                    {p}
                  </li>
                ))}
              </ul>
            </RevealItem>
          ))}
        </RevealGroup>
      </section>

      {/* For Teachers */}
      <section id="teachers" className="scroll-mt-24 bg-white/40 py-20 md:py-28">
        <div className="shell grid items-center gap-12 lg:grid-cols-2">
          <Reveal>
            <Photo photo={PHOTOS.teachers} ratio="4/3" />
          </Reveal>
          <div>
            <SectionHeading eyebrow="For teachers" title="Less admin. More teaching." />
            <RevealGroup className="mt-8 space-y-6">
              {TEACHER_FEATURES.map((f) => (
                <RevealItem key={f.title}>
                  <h3 className="text-base font-bold tracking-tight text-navy-deep">{f.title}</h3>
                  <p className="mt-1.5 text-sm leading-relaxed text-ink-2">{f.body}</p>
                </RevealItem>
              ))}
            </RevealGroup>
          </div>
        </div>
      </section>

      {/* For Parents */}
      <section id="parents" className="scroll-mt-24 py-20 md:py-28">
        <div className="shell grid items-center gap-12 lg:grid-cols-2">
          <div className="order-2 lg:order-1">
            <SectionHeading eyebrow="For parents" title="Your child's school, in your pocket." />
            <RevealGroup className="mt-8 space-y-6">
              {PARENT_FEATURES.map((f) => (
                <RevealItem key={f.title}>
                  <h3 className="text-base font-bold tracking-tight text-navy-deep">{f.title}</h3>
                  <p className="mt-1.5 text-sm leading-relaxed text-ink-2">{f.body}</p>
                </RevealItem>
              ))}
            </RevealGroup>
          </div>
          <Reveal className="order-1 lg:order-2">
            <Photo photo={PHOTOS.parents} ratio="4/3" />
          </Reveal>
        </div>
      </section>
    </div>
  );
}
