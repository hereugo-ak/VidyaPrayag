import { TEACHER_FEATURES } from "@/lib/content";
import { SectionHeading } from "../ui/SectionHeading";
import { Photo } from "../ui/Photo";
import { PHOTOS } from "@/lib/images";
import { RevealGroup, RevealItem, Reveal } from "../ui/Reveal";

export function ForTeachers() {
  return (
    <section id="teachers" className="scroll-mt-24 py-24 md:py-32">
      <div className="shell grid items-center gap-14 lg:grid-cols-[1.05fr_1fr]">
        <div>
          <SectionHeading
            eyebrow="For teachers"
            title={
              <>
                Less admin. <span className="text-accent">More teaching.</span>
              </>
            }
            lede="Teachers see only the classes they teach, mark attendance in seconds, enter marks once, and track homework and syllabus, all from a single, focused workspace."
          />

          <RevealGroup className="mt-10 grid gap-x-8 gap-y-7 sm:grid-cols-2">
            {TEACHER_FEATURES.map((f) => (
              <RevealItem key={f.title}>
                <h3 className="text-[15px] font-bold tracking-tight text-navy-deep">
                  {f.title}
                </h3>
                <p className="mt-2 text-sm leading-relaxed text-ink-2">{f.body}</p>
              </RevealItem>
            ))}
          </RevealGroup>
        </div>

        <Reveal>
          <Photo
            photo={PHOTOS.teachers}
            ratio="5/4"
            sizes="(max-width: 1024px) 100vw, 48vw"
            className="shadow-card"
          />
        </Reveal>
      </div>
    </section>
  );
}
