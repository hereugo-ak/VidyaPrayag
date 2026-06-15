import Link from "next/link";

/**
 * Enroll+ wordmark — a custom mark (no AI image, no stock logo). The "+" is a
 * teal accent tile, the rest is navy. Tight tracking, weight contrast.
 */
export function Logo({ className = "" }: { className?: string }) {
  return (
    <Link href="/" className={`group inline-flex items-center gap-2.5 ${className}`} aria-label="Enroll+ home">
      <span className="relative inline-flex h-8 w-8 items-center justify-center rounded-[0.6rem] bg-navy-deep">
        <span className="absolute inset-0 rounded-[0.6rem] ring-1 ring-inset ring-white/10" />
        <span className="font-extrabold text-white text-lg leading-none">E</span>
        <span className="absolute -bottom-1 -right-1 inline-flex h-4 w-4 items-center justify-center rounded-[0.35rem] bg-teal text-white text-[11px] font-bold leading-none shadow-sm">
          +
        </span>
      </span>
      <span className="text-[18px] font-extrabold tracking-tighter text-navy-deep">
        Enroll<span className="text-teal-deep">+</span>
      </span>
    </Link>
  );
}
