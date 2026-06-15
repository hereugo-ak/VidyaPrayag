import { SectionHeading } from "../ui/SectionHeading";
import { Reveal, RevealGroup, RevealItem } from "../ui/Reveal";

/**
 * Testimonials — REAL LAYOUT, intentionally empty slots.
 *
 * Per the brief: no fabricated quotes, schools, or logos. This section ships the
 * production-quality frame so that the moment we have a signed-off quote from a
 * real partner school, it drops straight in. Until then we present honest,
 * verifiable platform facts rather than fake social proof.
 */

const PILLARS = [
  {
    title: "Built around real school structure",
    body: "Classes, sections, subjects and the teacher-assignment graph are modelled exactly as a school runs — not bolted on.",
  },
  {
    title: "One backend, web and mobile",
    body: "The data you enter on the web onboarding is the same record your teachers and parents see in the app. No sync, no copies.",
  },
  {
    title: "Nothing fabricated",
    body: "Every analytic is computed from attendance and marks your own staff record. We never invent a number to fill a chart.",
  },
];

export function Testimonials() {
  return (
    <section className="py-24 md:py-32">
      <div className="shell">
        <SectionHeading
          eyebrow="Why schools choose us"
          title="We'd rather show you the product than borrow a quote."
          lede="Customer stories will live here once our first partner schools are ready to be named. Until then, here's what's actually true about the platform today."
          align="center"
        />

        <RevealGroup className="mt-16 grid gap-6 md:grid-cols-3">
          {PILLARS.map((p) => (
            <RevealItem
              key={p.title}
              className="flex flex-col rounded-xl2 border border-navy/8 bg-white/60 p-8"
            >
              <span aria-hidden className="text-3xl font-extrabold leading-none text-accent/30">
                &ldquo;
              </span>
              <h3 className="mt-4 text-lg font-extrabold tracking-tight text-navy-deep">
                {p.title}
              </h3>
              <p className="mt-3 text-sm leading-relaxed text-ink-2">{p.body}</p>
            </RevealItem>
          ))}
        </RevealGroup>

        {/* Honest placeholder strip for partner logos — empty until real ones exist. */}
        <Reveal className="mt-12">
          <p className="text-center text-xs font-medium uppercase tracking-[0.18em] text-ink-3/70">
            Partner schools — coming soon
          </p>
        </Reveal>
      </div>
    </section>
  );
}
