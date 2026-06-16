import { Reveal } from "../ui/Reveal";
import { Button } from "../ui/Button";
import { Photo } from "../ui/Photo";
import { PHOTOS } from "@/lib/images";

/**
 * Final CTA — the page's single strongest conversion prompt. Ultra-premium by
 * restraint: a deep-navy slab, one low-contrast photograph wash, the brief's
 * single sanctioned lavender→navy veil, and one soft aurora bloom in the corner
 * to give the panel depth without noise. One headline, two actions. Routes to
 * the web-only onboarding wizard.
 */
export function FinalCta() {
  return (
    <section className="pb-24 pt-8 md:pb-32">
      <div className="shell">
        <Reveal className="relative overflow-hidden rounded-[2rem] bg-navy-deep px-6 py-16 text-white shadow-cta sm:px-12 md:px-16 md:py-24">
          {/* Subtle photo wash, kept low-contrast so the copy leads. */}
          <div className="pointer-events-none absolute inset-0 opacity-[0.14]">
            <Photo
              photo={PHOTOS.building}
              ratio="16/9"
              rounded="rounded-none"
              className="h-full w-full"
              sizes="100vw"
            />
          </div>
          {/* Lavender-to-navy veil over the photo (the brief's only sanctioned wash). */}
          <div
            aria-hidden
            className="pointer-events-none absolute inset-0"
            style={{
              background:
                "linear-gradient(120deg, rgba(26,24,56,0.92) 0%, rgba(38,35,77,0.86) 55%, rgba(108,92,224,0.42) 100%)",
            }}
          />
          {/* Corner aurora bloom — gives the slab depth, stays low-opacity. */}
          <div
            aria-hidden
            className="pointer-events-none absolute -right-24 -top-24 h-[360px] w-[360px] rounded-full opacity-60"
            style={{
              background:
                "radial-gradient(circle, rgba(108,92,224,0.45) 0%, rgba(108,92,224,0) 68%)",
            }}
          />
          {/* Hairline top edge — a single bright line that catches the eye. */}
          <div
            aria-hidden
            className="pointer-events-none absolute inset-x-0 top-0 h-px"
            style={{
              background:
                "linear-gradient(90deg, transparent, rgba(255,255,255,0.28), transparent)",
            }}
          />

          <div className="relative max-w-2xl">
            <p className="inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-accent-soft">
              <span className="h-1.5 w-1.5 rounded-full bg-accent-soft" />
              Ready when you are
            </p>
            <h2 className="mt-4 text-3xl font-extrabold leading-[1.06] tracking-tighter sm:text-4xl md:text-5xl">
              Modernise your school
              <br className="hidden sm:block" /> in an afternoon.
            </h2>
            <p className="mt-5 max-w-xl text-lg leading-relaxed text-white/75">
              Set up your admin account, branding and academic structure right here on
              the web. Your teachers and parents come online the same day.
            </p>
            <div className="mt-9 flex flex-col gap-3 sm:flex-row sm:items-center">
              <Button href="/onboarding" size="lg" variant="secondary">
                Onboard your school
              </Button>
              <Button
                href="/features"
                size="lg"
                variant="ghost"
                className="text-white hover:bg-white/10"
              >
                Explore the platform
              </Button>
            </div>
            <p className="mt-6 text-sm text-white/55">
              No credit card. No sales call. One source of truth from day one.
            </p>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
