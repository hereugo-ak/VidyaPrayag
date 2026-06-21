import { Reveal } from "./Reveal";

/**
 * Section heading with an eyebrow label + tight display title + optional lede.
 * Heading hierarchy is legible via weight contrast + negative tracking, never
 * size alone.
 */
export function SectionHeading({
  eyebrow,
  title,
  lede,
  align = "left",
  className = "",
}: {
  eyebrow?: string;
  title: React.ReactNode;
  lede?: React.ReactNode;
  align?: "left" | "center";
  className?: string;
}) {
  const alignCls = align === "center" ? "text-center mx-auto" : "text-left";
  return (
    <Reveal className={`${alignCls} max-w-2xl ${className}`}>
      {eyebrow ? <p className="eyebrow mb-3">{eyebrow}</p> : null}
      <h2 className="display text-3xl leading-[1.08] sm:text-4xl md:text-[2.75rem]">
        {title}
      </h2>
      {lede ? (
        <p className="mt-5 text-lg leading-relaxed text-ink-2 max-w-prose">
          {lede}
        </p>
      ) : null}
    </Reveal>
  );
}
