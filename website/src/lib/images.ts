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
  // Hero, a real, lively classroom: school children working together around a
  // table. IMAGE AUDIT (2026-06): the previous id (1541339907198) was a
  // graduation cap-throwing scene, NOT a classroom, so it misrepresented the
  // hero claim. Replaced with a true in-class learning moment.
  hero: {
    src: unsplash("1588072432836-e10032774350", 2000), // school children working at a table
    alt: "School children working together on activities at a classroom table",
    blur: BLUR_DATA,
  } satisfies Photo,

  // For Schools, an institutional classroom moment. IMAGE AUDIT (2026-06): the
  // previous id (1577896851231) showed a chalkboard with a sensitive,
  // off-topic message ("If someone in your family has cancer"), which read as
  // jarring and off-brand. Replaced with a teacher leading young students in a
  // classroom, which is exactly what "the admin office, rebuilt" sits beside.
  schools: {
    src: unsplash("1588075592446-265fd1e6e76f", 1400), // teacher reading to a class
    alt: "A teacher leading a group of school children in a classroom",
    blur: BLUR_DATA,
  } satisfies Photo,

  // For Parents, the school day parents get a window into. IMAGE AUDIT
  // (2026-06): the previous id (1543269865) showed adults collaborating in a
  // cafe, not a school context. Replaced with a real, bright classroom during
  // the school day, the exact thing the parent app surfaces in real time.
  parents: {
    src: unsplash("1571260899304-425eee4c7efc", 1400), // bright classroom, school day
    alt: "Students in a bright classroom during the school day",
    blur: BLUR_DATA,
  } satisfies Photo,

  // For Teachers, a teacher leading a class. IMAGE AUDIT (2026-06): verified to
  // depict exactly its claim (a teacher at the front of a K-12 classroom).
  teachers: {
    src: unsplash("1509062522246-3755977927d7", 1400), // teacher leading a class
    alt: "A teacher leading a lesson at the front of a classroom",
    blur: BLUR_DATA,
  } satisfies Photo,

  // School building exterior, institutional credibility.
  building: {
    src: unsplash("1562774053-701939374585", 1600), // school/university building
    alt: "The facade of a school building on a clear day",
    blur: BLUR_DATA,
  } satisfies Photo,

  // Onboarding side panel, focused study / desk.
  onboarding: {
    src: unsplash("1503676260728-1c00da094a0b", 1200), // desk, notebook, study
    alt: "A workspace with notebooks ready for setting up a school",
    blur: BLUR_DATA,
  } satisfies Photo,

  // Secondary learning scene for the login + features pages. IMAGE AUDIT
  // (2026-06): verified to depict students collaborating with laptops, which
  // matches its claim.
  collaboration: {
    src: unsplash("1522202176988-66273c2fd55f", 1400), // students collaborating
    alt: "Students collaborating together on a project",
    blur: BLUR_DATA,
  } satisfies Photo,

  // Parents page hero, a real parent-and-child moment, warm and bright. Used on
  // /parents to anchor the page in the actual relationship the app serves. NOT
  // AI-generated, real Unsplash photography of a parent reading with their child.
  parentsHero: {
    src: unsplash("1503454537195-1dcabb73ffb9", 1800), // parent reading with child, warm light
    alt: "A parent reading with their child in warm afternoon light",
    blur: BLUR_DATA,
  } satisfies Photo,

  // Parents page secondary, a calm portrait of a family at home checking a
  // phone together, supports the "their school day, in your pocket" promise.
  parentsAtHome: {
    src: unsplash("1581578731548-c64695cc6952", 1600), // mother and child looking at phone
    alt: "A parent and child looking at a phone together at home",
    blur: BLUR_DATA,
  } satisfies Photo,

  // Parents page atmosphere, a real campus exterior at golden hour, available
  // for muted background bands on the parents page if needed.
  parentsCampus: {
    src: unsplash("1490127252417-7c393f993ee4", 2000), // school exterior, calm light
    alt: "A school building at golden hour",
    blur: BLUR_DATA,
  } satisfies Photo,

  // Footer onboarding band, a real, light, muted campus exterior: an
  // institutional building above a wide, calm plaza of negative space. It sits
  // quietly behind the closing CTA (heavy light overlay), reinforcing that
  // Enroll+ is for whole schools, never competing with the copy.
  footerOnboarding: {
    src: unsplash("1592280771190-3e2e4d571952", 2000), // modern school/university campus
    alt: "A modern school campus building overlooking a quiet open plaza",
    blur: BLUR_DATA,
  } satisfies Photo,
} as const;

export type PhotoKey = keyof typeof PHOTOS;
