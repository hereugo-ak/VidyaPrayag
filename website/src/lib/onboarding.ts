/**
 * Onboarding wizard data model + validation, mirroring the EXACT server contract
 * in server/.../feature/onboarding/OnboardingRouting.kt and AuthRouting.kt.
 *
 * Step order (server `nextStepAfter`): REGISTER → BASIC → BRANDING → ACADEMIC → REVIEW.
 * The REGISTER step is the website-only account-creation step that calls
 * /api/v1/auth/register-school; the four following steps map 1:1 to the server's
 * obStepType values and POST /api/v1/onboarding/submit.
 */

export type WizardStep = "REGISTER" | "BASIC" | "BRANDING" | "ACADEMIC" | "REVIEW";

export const WIZARD_STEPS: { id: WizardStep; label: string; short: string }[] = [
  { id: "REGISTER", label: "Create your admin account", short: "Account" },
  { id: "BASIC", label: "Institutional basics", short: "Basics" },
  { id: "BRANDING", label: "Branding & identity", short: "Branding" },
  { id: "ACADEMIC", label: "Academic structure", short: "Academics" },
  { id: "REVIEW", label: "Review & launch", short: "Launch" },
];

// ─── Option sets (match server defaults / accepted values) ────────────────────
export const BOARD_OPTIONS = [
  { value: "CBSE", label: "CBSE" },
  { value: "ICSE", label: "ICSE / CISCE" },
  { value: "UP_STATE", label: "UP State Board" },
  { value: "STATE", label: "Other State Board" },
  { value: "IB", label: "International Baccalaureate" },
  { value: "CAMBRIDGE", label: "Cambridge (CAIE)" },
  { value: "OTHER", label: "Other" },
];

export const MEDIUM_OPTIONS = [
  { value: "English", label: "English" },
  { value: "Hindi", label: "Hindi" },
  { value: "Bilingual", label: "Bilingual (English + Hindi)" },
  { value: "Other", label: "Other" },
];

export const GENDER_OPTIONS = [
  { value: "co_ed", label: "Co-educational" },
  { value: "boys", label: "Boys only" },
  { value: "girls", label: "Girls only" },
];

// ─── Wizard state ─────────────────────────────────────────────────────────────
export interface RegisterData {
  adminName: string;
  email: string;
  password: string;
  schoolName: string;
}

export interface BasicData {
  board: string;
  medium: string;
  school_gender: string;
  contact_email: string;
  contact_phone: string;
  city: string;
  district: string;
  state: string;
  pincode: string;
  full_address: string;
}

export interface BrandingData {
  brand_color: string;
  logo_url: string;
}

export interface ClassRow {
  name: string;
  sections: string; // comma-separated, parsed on submit
  subjects: string; // comma-separated subject names
}

export interface AcademicData {
  classes: ClassRow[];
}

export interface WizardState {
  step: WizardStep;
  register: RegisterData;
  basic: BasicData;
  branding: BrandingData;
  academic: AcademicData;
}

export const INITIAL_STATE: WizardState = {
  step: "REGISTER",
  register: { adminName: "", email: "", password: "", schoolName: "" },
  basic: {
    board: "CBSE",
    medium: "English",
    school_gender: "co_ed",
    contact_email: "",
    contact_phone: "",
    city: "",
    district: "",
    state: "Uttar Pradesh",
    pincode: "",
    full_address: "",
  },
  branding: { brand_color: "#6C5CE0", logo_url: "" },
  academic: {
    classes: [
      { name: "Grade 1", sections: "A", subjects: "English, Mathematics, EVS" },
      { name: "Grade 2", sections: "A", subjects: "English, Mathematics, EVS" },
    ],
  },
};

export const STORAGE_KEY = "enrollplus.onboarding.v1";

// ─── Validation (client mirrors server-enforced required fields) ──────────────
export type Errors<T> = Partial<Record<keyof T, string>>;

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateRegister(d: RegisterData): Errors<RegisterData> {
  const e: Errors<RegisterData> = {};
  if (!d.adminName.trim()) e.adminName = "Your name is required.";
  if (!d.email.trim()) e.email = "A work email is required.";
  else if (!EMAIL_RE.test(d.email.trim())) e.email = "Enter a valid email address.";
  if (!d.password) e.password = "Choose a password.";
  else if (d.password.length < 8) e.password = "Password must be at least 8 characters.";
  if (!d.schoolName.trim()) e.schoolName = "Your school's name is required.";
  return e;
}

export function validateBasic(d: BasicData): Errors<BasicData> {
  const e: Errors<BasicData> = {};
  if (!d.board) e.board = "Select your board.";
  if (!d.medium) e.medium = "Select the medium of instruction.";
  if (!d.school_gender) e.school_gender = "Select the school type.";
  if (d.contact_email && !EMAIL_RE.test(d.contact_email.trim()))
    e.contact_email = "Enter a valid email address.";
  if (!d.city.trim()) e.city = "City is required.";
  if (!d.state.trim()) e.state = "State is required.";
  return e;
}

export function validateAcademic(d: AcademicData): string | null {
  const real = d.classes.filter((c) => c.name.trim());
  if (real.length === 0) return "Add at least one class to continue.";
  return null;
}

export function hasErrors<T>(e: Errors<T>): boolean {
  return Object.keys(e).length > 0;
}

// ─── Payload builders (exact server keys) ─────────────────────────────────────
export function basicPayload(d: BasicData): Record<string, unknown> {
  return {
    school_name: "", // already set at register; server keeps existing if blank
    board: d.board,
    medium: d.medium,
    school_gender: d.school_gender,
    contact_email: d.contact_email,
    contact_phone: d.contact_phone,
    city: d.city,
    district: d.district || d.city,
    state: d.state,
    pincode: d.pincode,
    full_address: d.full_address,
  };
}

export function brandingPayload(d: BrandingData): Record<string, unknown> {
  return {
    brand_color: d.brand_color,
    logo_url: d.logo_url,
  };
}

function slugCode(name: string, i: number): string {
  const s = name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
  return s ? s : `class-${i + 1}`;
}

export function academicPayload(d: AcademicData): Record<string, unknown> {
  const classes = d.classes
    .filter((c) => c.name.trim())
    .map((c, i) => ({
      code: slugCode(c.name, i),
      name: c.name.trim(),
      sections: c.sections
        .split(",")
        .map((s) => s.trim())
        .filter(Boolean)
        .map((s) => s.toUpperCase()),
      subjects: c.subjects
        .split(",")
        .map((s) => s.trim())
        .filter(Boolean)
        .map((sub) => ({
          sub_name: sub,
          sub_code: sub.slice(0, 4).toUpperCase().replace(/\s/g, ""),
        })),
    }));
  return { classes };
}
