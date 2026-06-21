import Image from "next/image";
import type { Photo as PhotoData } from "@/lib/images";

const RATIOS: Record<string, string> = {
  "16/9": "aspect-[16/9]",
  "4/3": "aspect-[4/3]",
  "3/2": "aspect-[3/2]",
  "1/1": "aspect-square",
  "5/4": "aspect-[5/4]",
  "4/5": "aspect-[4/5]",
};

/**
 * Photo, enforces an aspect ratio, lazy-loads with a blur placeholder, and
 * never stretches the image (object-cover inside a fixed-ratio frame).
 * Always real photography (URLs come from lib/images.ts).
 */
export function Photo({
  photo,
  ratio = "4/3",
  priority = false,
  sizes = "(max-width: 768px) 100vw, 50vw",
  className = "",
  rounded = "rounded-xl2",
}: {
  photo: PhotoData;
  ratio?: keyof typeof RATIOS | (string & {});
  priority?: boolean;
  sizes?: string;
  className?: string;
  rounded?: string;
}) {
  const ratioCls = RATIOS[ratio] ?? "aspect-[4/3]";
  return (
    <div
      className={`relative ${ratioCls} ${rounded} overflow-hidden bg-lavender-tint ring-1 ring-navy/5 ${className}`}
    >
      <Image
        src={photo.src}
        alt={photo.alt}
        fill
        sizes={sizes}
        priority={priority}
        placeholder="blur"
        blurDataURL={photo.blur}
        className="object-cover"
      />
    </div>
  );
}
