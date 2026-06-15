/**
 * Curated REAL photographs from Unsplash (license permits commercial use, no
 * attribution required). Every URL points at the Unsplash image CDN with sizing
 * params. NOTHING here is AI-generated, illustrated, or a stock-vector.
 *
 * Each entry carries a tiny base64 `blur` placeholder (a 8px solid lavender-ish
 * tone) so next/image can render `placeholder="blur"` with zero pop-in and no
 * layout shift while the real photo streams in.
 *
 * To swap a photo: replace the Unsplash photo id in the URL. Keep the aspect
 * ratios consistent with where it's used (the <Photo> component enforces ratio).
 */

export interface Photo {
  src: string;
  alt: string;
  blur: string;
}

// A neutral lavender 1x1 GIF used as the universal blur seed (keeps the bundle
// tiny, avoids per-image base64 bloat, and matches the page's lavender canvas).
const BLUR_DATA =
  "data:image/gif;base64,R0lGODlhAQABAIAAAOTk+gAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==";

function unsplash(id: string, w = 1600): string {
  return `https://images.unsplash.com/photo-${id}?auto=format&fit=crop&w=${w}&q=80`;
}

export const PHOTOS = {
  // Hero — a real, bright classroom with students; editorial, on-location feel.
  hero: {
    src: unsplash("1523050854058-8df90110c9f1", 2000), // university hall / students
    alt: "Students seated in a bright lecture hall during a class session",
    blur: BLUR_DATA,
  } satisfies Photo,

  // For Schools — administrative / institutional environment.
  schools: {
    src: unsplash("1577896851231-70ef18881754", 1400), // teacher at desk with students
    alt: "A teacher working with students around a table in a school",
    blur: BLUR_DATA,
  } satisfies Photo,

  // For Parents — a parent and child together.
  parents: {
    src: unsplash("1543269865-cbf427effbad", 1400), // people studying together
    alt: "A parent reviewing schoolwork with their child at home",
    blur: BLUR_DATA,
  } satisfies Photo,

  // For Teachers — a teacher at the board / leading a class.
  teachers: {
    src: unsplash("1509062522246-3755977927d7", 1400), // classroom chalkboard
    alt: "A teacher leading a lesson at the front of a classroom",
    blur: BLUR_DATA,
  } satisfies Photo,

  // School building exterior — institutional credibility.
  building: {
    src: unsplash("1562774053-701939374585", 1600), // school/university building
    alt: "The facade of a school building on a clear day",
    blur: BLUR_DATA,
  } satisfies Photo,

  // Onboarding side panel — focused study / desk.
  onboarding: {
    src: unsplash("1503676260728-1c00da094a0b", 1200), // desk, notebook, study
    alt: "A workspace with notebooks ready for setting up a school",
    blur: BLUR_DATA,
  } satisfies Photo,

  // Secondary classroom for features page.
  collaboration: {
    src: unsplash("1522202176988-66273c2fd55f", 1400), // students collaborating
    alt: "Students collaborating together on a project",
    blur: BLUR_DATA,
  } satisfies Photo,
} as const;

export type PhotoKey = keyof typeof PHOTOS;
