import { HOW_IT_WORKS } from "@/lib/content";
import { SectionHeading } from "../ui/SectionHeading";
import { RevealGroup, RevealItem } from "../ui/Reveal";

export function HowItWorks() {
  return (
    <section id="how-it-works" className="scroll-mt-24 bg-white/40 py-24 md:py-32">
      <div className="shell">
        <SectionHeading
          eyebrow="How it works"
          title="From signup to live in three steps."
          lede="The same sequence the platform runs: onboard the school here on the web, provision your teachers, and let parents connect by roll number."
          align="center"
        />

        <RevealGroup className="mt-16 grid gap-8 md:grid-cols-3">
          {HOW_IT_WORKS.map((s, i) => (
            <RevealItem
              key={s.step}
              className="relative rounded-xl2 border border-navy/8 bg-lavender-soft/60 p-8"
            >
              <span className="font-mono text-sm font-medium text-accent">{s.step}</span>
              <h3 className="mt-4 text-xl font-extrabold tracking-tight text-navy-deep">
                {s.title}
              </h3>
              <p className="mt-3 text-sm leading-relaxed text-ink-2">{s.body}</p>
              {i < HOW_IT_WORKS.length - 1 && (
                <span
                  aria-hidden
                  className="absolute right-6 top-8 hidden text-ink-3/40 md:block"
                >
                  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M5 12h14M13 6l6 6-6 6" />
                  </svg>
                </span>
              )}
            </RevealItem>
          ))}
        </RevealGroup>
      </div>
    </section>
  );
}
