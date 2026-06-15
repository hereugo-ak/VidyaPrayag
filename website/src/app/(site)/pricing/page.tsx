import type { Metadata } from "next";
import { SectionHeading } from "@/components/ui/SectionHeading";
import { Reveal, RevealGroup, RevealItem } from "@/components/ui/Reveal";
import { Button } from "@/components/ui/Button";

export const metadata: Metadata = {
  title: "Pricing — Enroll+",
  description:
    "Simple, per-school pricing for Enroll+. Talk to us for a quote tailored to your size and board — onboarding and parent access are always included.",
  alternates: { canonical: "/pricing" },
};

/**
 * Pricing — honest layout. We do NOT invent price points; the platform's
 * commercial terms are quoted per school. The page presents what every plan
 * includes and routes commercial questions to a real contact path.
 */
const INCLUDED = [
  "Unlimited teachers & staff logins",
  "Unlimited parent connections",
  "Attendance, marks & fees ledger",
  "Segmented announcements & messaging",
  "Analytics on real entries",
  "Admissions pipeline & PTM scheduling",
  "Web onboarding + mobile apps",
  "One shared backend — no data silos",
];

const TIERS = [
  {
    name: "Single school",
    blurb: "Everything one institution needs, on web and mobile.",
    note: "Quoted per school",
    cta: { label: "Onboard your school", href: "/onboarding", variant: "primary" as const },
    highlight: true,
  },
  {
    name: "Group / trust",
    blurb: "Multiple campuses under one administration with consolidated reporting.",
    note: "Let's talk volume",
    cta: { label: "Talk to us", href: "/onboarding", variant: "secondary" as const },
    highlight: false,
  },
];

export default function PricingPage() {
  return (
    <div className="pt-28 md:pt-32">
      <section className="shell">
        <SectionHeading
          eyebrow="Pricing"
          title="Priced per school, not per headache."
          lede="We tailor a quote to your size and board so you only pay for what fits. Onboarding, parent access and the full feature set are always included — no per-parent fees, no surprise add-ons."
          align="center"
        />
      </section>

      <section className="shell py-16">
        <RevealGroup className="mx-auto grid max-w-4xl gap-6 md:grid-cols-2">
          {TIERS.map((t) => (
            <RevealItem
              key={t.name}
              className={`flex flex-col rounded-xl2 border p-8 ${
                t.highlight
                  ? "border-accent/40 bg-white/80 shadow-card"
                  : "border-navy/10 bg-white/55"
              }`}
            >
              <h3 className="text-xl font-extrabold tracking-tight text-navy-deep">{t.name}</h3>
              <p className="mt-2 text-sm leading-relaxed text-ink-2">{t.blurb}</p>
              <p className="mt-6 font-mono text-sm font-medium text-accent">{t.note}</p>
              <div className="mt-8">
                <Button href={t.cta.href} variant={t.cta.variant} size="lg" className="w-full">
                  {t.cta.label}
                </Button>
              </div>
            </RevealItem>
          ))}
        </RevealGroup>

        <Reveal className="mx-auto mt-12 max-w-3xl rounded-xl2 border border-navy/8 bg-lavender-soft/60 p-8">
          <h3 className="text-sm font-bold uppercase tracking-wide text-ink-2">
            Included in every plan
          </h3>
          <ul className="mt-5 grid gap-3 sm:grid-cols-2">
            {INCLUDED.map((f) => (
              <li key={f} className="flex items-start gap-2.5 text-sm text-ink-2">
                <svg
                  className="mt-0.5 h-4 w-4 shrink-0 text-teal-deep"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M5 13l4 4L19 7" />
                </svg>
                {f}
              </li>
            ))}
          </ul>
        </Reveal>

        <Reveal className="mt-10 text-center">
          <p className="text-sm text-ink-3">
            Have a specific question about pricing for your board or size?{" "}
            <a href="/onboarding" className="font-semibold text-accent hover:underline">
              Start onboarding
            </a>{" "}
            and we&apos;ll be in touch.
          </p>
        </Reveal>
      </section>
    </div>
  );
}
