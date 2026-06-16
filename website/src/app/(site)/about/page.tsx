import type { Metadata } from "next";
import { ABOUT } from "@/lib/content";
import { PHOTOS } from "@/lib/images";
import { SectionHeading } from "@/components/ui/SectionHeading";
import { Reveal, RevealGroup, RevealItem } from "@/components/ui/Reveal";
import { Photo } from "@/components/ui/Photo";
import { Button } from "@/components/ui/Button";

export const metadata: Metadata = {
  title: "About — Enroll+",
  description:
    "Why we built Enroll+: one source of truth for a school's office, teachers and parents. Real-time where it matters, premium by subtraction, and never a fabricated number.",
  alternates: { canonical: "/about" },
};

export default function AboutPage() {
  return (
    <div className="pt-28 md:pt-32">
      {/* Hero — editorial opening */}
      <section className="shell">
        <SectionHeading
          eyebrow={ABOUT.hero.eyebrow}
          title={ABOUT.hero.title}
          lede={ABOUT.hero.lede}
        />
      </section>

      {/* Lead photograph — real institutional imagery */}
      <section className="shell mt-14 md:mt-16">
        <Reveal>
          <Photo
            photo={PHOTOS.building}
            ratio="16/9"
            priority
            sizes="(max-width: 1024px) 100vw, 1100px"
          />
        </Reveal>
      </section>

      {/* Story — long-form, alternating headings */}
      <section className="shell py-20 md:py-28">
        <div className="mx-auto max-w-3xl space-y-16">
          {ABOUT.story.map((block) => (
            <Reveal key={block.heading}>
              <h2 className="display text-2xl leading-[1.12] sm:text-3xl">
                {block.heading}
              </h2>
              <div className="mt-5 space-y-5">
                {block.paragraphs.map((p, i) => (
                  <p
                    key={i}
                    className="text-lg leading-relaxed text-ink-2"
                  >
                    {p}
                  </p>
                ))}
              </div>
            </Reveal>
          ))}
        </div>
      </section>

      {/* Honest numbers */}
      <section className="scroll-mt-24 bg-white/40 py-20 md:py-28">
        <div className="shell">
          <SectionHeading
            eyebrow="Honest numbers"
            title="What we can actually claim."
            lede="No customer counts we don't have, no revenue we won't quote. These are the capabilities the platform genuinely ships."
          />
          <RevealGroup className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {ABOUT.numbers.map((n) => (
              <RevealItem
                key={n.label}
                className="rounded-xl2 border border-navy/8 bg-white/60 p-7"
              >
                <p className="display text-4xl text-navy-deep">{n.value}</p>
                <p className="mt-3 text-sm font-bold tracking-tight text-navy-deep">
                  {n.label}
                </p>
                <p className="mt-1.5 text-sm leading-relaxed text-ink-3">
                  {n.note}
                </p>
              </RevealItem>
            ))}
          </RevealGroup>
        </div>
      </section>

      {/* Principles */}
      <section className="shell py-20 md:py-28">
        <SectionHeading
          eyebrow="What we believe"
          title="The principles the product is built on."
        />
        <RevealGroup className="mt-12 grid gap-6 md:grid-cols-2">
          {ABOUT.principles.map((p) => (
            <RevealItem
              key={p.title}
              className="rounded-xl2 border border-navy/8 bg-white/60 p-7"
            >
              <h3 className="text-lg font-extrabold tracking-tight text-navy-deep">
                {p.title}
              </h3>
              <p className="mt-3 text-sm leading-relaxed text-ink-2">
                {p.body}
              </p>
            </RevealItem>
          ))}
        </RevealGroup>
      </section>

      {/* Team — honest placeholder structure */}
      <section className="scroll-mt-24 bg-white/40 py-20 md:py-28">
        <div className="shell">
          <SectionHeading
            eyebrow="The team"
            title="Small team, large intent."
            lede={ABOUT.team.note}
          />
          <RevealGroup className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {ABOUT.team.roles.map((m) => (
              <RevealItem
                key={m.role}
                className="rounded-xl2 border border-navy/8 bg-white/60 p-7"
              >
                {/* Neutral avatar placeholder — no invented faces. */}
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-lavender-tint ring-1 ring-navy/8">
                  <span className="h-2 w-2 rounded-full bg-accent" />
                </div>
                <h3 className="mt-5 text-base font-bold tracking-tight text-navy-deep">
                  {m.role}
                </h3>
                <p className="mt-1.5 text-sm leading-relaxed text-ink-2">
                  {m.focus}
                </p>
              </RevealItem>
            ))}
          </RevealGroup>
        </div>
      </section>

      {/* CTA */}
      <section className="shell pb-24 pt-20 md:pt-28">
        <Reveal className="flex flex-col items-center gap-5 rounded-xl2 border border-navy/8 bg-lavender-soft/60 px-6 py-14 text-center">
          <h2 className="display text-2xl sm:text-3xl">
            Run your school on one source of truth.
          </h2>
          <p className="max-w-md text-ink-2">
            Onboarding happens on the web and takes minutes. Set it up once and
            your whole team comes online.
          </p>
          <Button href="/onboarding" size="lg">
            Onboard your school
          </Button>
        </Reveal>
      </section>
    </div>
  );
}
