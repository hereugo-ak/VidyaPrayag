import { PARENT_FEATURES } from "@/lib/content";
import { SectionHeading } from "../ui/SectionHeading";
import { Photo } from "../ui/Photo";
import { PHOTOS } from "@/lib/images";
import { RevealGroup, RevealItem, Reveal } from "../ui/Reveal";

export function ForParents() {
  return (
    <section id="parents" className="scroll-mt-24 bg-white/40 py-24 md:py-32">
      <div className="shell grid items-center gap-14 lg:grid-cols-[1fr_1.05fr]">
        <Reveal className="order-2 lg:order-1">
          <Photo
            photo={PHOTOS.parents}
            ratio="5/4"
            sizes="(max-width: 1024px) 100vw, 48vw"
            className="shadow-card"
          />
        </Reveal>

        <div className="order-1 lg:order-2">
          <SectionHeading
            eyebrow="For parents"
            title={
              <>
                Their school day, <span className="text-accent">in your pocket.</span>
              </>
            }
            lede="No more end-of-term surprises. Parents see attendance, results, fees and messages the moment they happen — straight from the people who entered them."
          />

          <RevealGroup className="mt-10 grid gap-x-8 gap-y-7 sm:grid-cols-2">
            {PARENT_FEATURES.map((f) => (
              <RevealItem key={f.title}>
                <h3 className="text-[15px] font-bold tracking-tight text-navy-deep">
                  {f.title}
                </h3>
                <p className="mt-2 text-sm leading-relaxed text-ink-2">{f.body}</p>
              </RevealItem>
            ))}
          </RevealGroup>
        </div>
      </div>
    </section>
  );
}
