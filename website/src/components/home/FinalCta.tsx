import { Reveal } from "../ui/Reveal";
import { Button } from "../ui/Button";
import { Photo } from "../ui/Photo";
import { PHOTOS } from "@/lib/images";

/**
 * Final CTA — the page's single strongest conversion prompt. Routes to the
 * web-only onboarding wizard. One photograph, one headline, two actions.
 */
export function FinalCta() {
  return (
    <section className="pb-24 pt-8 md:pb-32">
      <div className="shell">
        <Reveal className="relative overflow-hidden rounded-[2rem] bg-navy-deep px-6 py-16 text-white sm:px-12 md:px-16 md:py-24">
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

          <div className="relative max-w-2xl">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent-soft">
              Ready when you are
            </p>
            <h2 className="mt-4 text-3xl font-extrabold leading-[1.06] tracking-tighter sm:text-4xl md:text-5xl">
              Modernise your school in an afternoon.
            </h2>
            <p className="mt-5 max-w-xl text-lg leading-relaxed text-white/75">
              Set up your admin account, branding and academic structure right here on
              the web. Your teachers and parents come online the same day.
            </p>
            <div className="mt-9 flex flex-col gap-3 sm:flex-row sm:items-center">
              <Button href="/onboarding" size="lg" variant="secondary">
                Onboard your school
              </Button>
              <Button href="/features" size="lg" variant="ghost" className="text-white hover:bg-white/10">
                Explore the platform
              </Button>
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
