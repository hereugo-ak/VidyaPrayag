import { PageHeader } from "../ui/PageHeader";

/**
 * Shared shell for the long-form legal pages (privacy, terms, cookies). Each
 * gets its own distinct PageHeader (eyebrow + breadcrumb + title + "last
 * updated") then a tight prose body. Typography stays calm; headings on the
 * navy ink scale.
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
    <>
      <PageHeader
        eyebrow="Legal"
        crumbs={[{ label: "Home", href: "/" }, { label: title }]}
        title={title}
        lede={`Last updated: ${updated}. Written to reflect exactly what the platform stores, nothing here is boilerplate.`}
      />

      <div className="shell pb-24 pt-12 md:pt-16">
        <div className="legal max-w-prose space-y-10 text-ink-2">{children}</div>
      </div>
    </>
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
