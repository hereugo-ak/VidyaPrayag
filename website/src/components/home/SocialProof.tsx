import { SOCIAL_PROOF } from "@/lib/content";
import { RevealGroup, RevealItem } from "../ui/Reveal";

export function SocialProof() {
  return (
    <section className="border-y border-navy/8 bg-white/40 py-10">
      <div className="shell">
        <RevealGroup className="grid grid-cols-2 gap-8 md:grid-cols-4">
          {SOCIAL_PROOF.map((item) => (
            <RevealItem key={item.label} className="text-center md:text-left">
              <p className="font-mono text-3xl font-medium text-navy-deep">
                {item.value}
              </p>
              <p className="mt-1 text-sm font-semibold text-ink">{item.label}</p>
              <p className="text-[12px] text-ink-3">{item.note}</p>
            </RevealItem>
          ))}
        </RevealGroup>
      </div>
    </section>
  );
}
