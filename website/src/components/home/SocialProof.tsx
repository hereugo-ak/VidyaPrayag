import { SOCIAL_PROOF } from "@/lib/content";
import { RevealGroup, RevealItem } from "../ui/Reveal";

/**
 * Social proof — honest platform facts (not customer vanity metrics). Rendered
 * as a quiet, evenly-divided band: tabular figures, hairline separators between
 * cells on desktop, a tracked label and a soft note. Premium by restraint.
 */
export function SocialProof() {
  return (
    <section className="border-y border-navy/8 bg-white/40 py-12">
      <div className="shell">
        <RevealGroup className="grid grid-cols-2 gap-y-8 md:grid-cols-4 md:gap-y-0 md:divide-x md:divide-navy/8">
          {SOCIAL_PROOF.map((item, i) => (
            <RevealItem
              key={item.label}
              className={`text-center md:text-left ${i === 0 ? "md:pr-8" : "md:px-8"} ${
                i === SOCIAL_PROOF.length - 1 ? "md:pl-8 md:pr-0" : ""
              }`}
            >
              <p className="nums font-mono text-[2.1rem] font-medium leading-none tracking-tight text-navy-deep">
                {item.value}
              </p>
              <p className="mt-2 text-sm font-bold text-ink">{item.label}</p>
              <p className="mt-0.5 text-[12px] leading-snug text-ink-3">{item.note}</p>
            </RevealItem>
          ))}
        </RevealGroup>
      </div>
    </section>
  );
}
