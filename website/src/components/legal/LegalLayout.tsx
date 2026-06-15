import { Reveal } from "../ui/Reveal";

/**
 * Shared shell for the long-form legal pages (privacy, terms). Keeps measure
 * tight (prose width), typography calm, and headings on the navy ink scale.
 */
export function LegalLayout({
  title,
  updated,
  children,
}: {
  title: string;
  updated: string;
  children: React.ReactNode;
}) {
  return (
    <div className="shell pb-24 pt-28 md:pt-32">
      <Reveal className="max-w-prose">
        <p className="eyebrow mb-3">Legal</p>
        <h1 className="display text-4xl leading-[1.05]">{title}</h1>
        <p className="mt-3 text-sm text-ink-3">Last updated: {updated}</p>
      </Reveal>

      <div className="legal mt-12 max-w-prose space-y-10 text-ink-2">
        {children}
      </div>
    </div>
  );
}

export function LegalSection({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section>
      <h2 className="text-lg font-extrabold tracking-tight text-navy-deep">{title}</h2>
      <div className="mt-3 space-y-3 text-[15px] leading-relaxed [&_a]:font-semibold [&_a]:text-accent hover:[&_a]:underline [&_code]:rounded [&_code]:bg-navy/5 [&_code]:px-1.5 [&_code]:py-0.5 [&_code]:font-mono [&_code]:text-[13px] [&_code]:text-navy-deep [&_li]:ml-1 [&_strong]:font-semibold [&_strong]:text-navy-deep [&_ul]:list-disc [&_ul]:space-y-1.5 [&_ul]:pl-5">
        {children}
      </div>
    </section>
  );
}
