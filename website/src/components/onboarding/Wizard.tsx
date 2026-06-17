"use client";

import { useEffect, useReducer, useState } from "react";
import { useRouter } from "next/navigation";
import { AnimatePresence, motion } from "framer-motion";
import {
  INITIAL_STATE,
  STORAGE_KEY,
  WIZARD_STEPS,
  type WizardState,
  type WizardStep,
  type RegisterData,
  type BasicData,
  type BrandingData,
  type AcademicData,
  type StudentsData,
  type ClassRow,
  validateRegister,
  validateBasic,
  validateAcademic,
  hasErrors,
  basicPayload,
  brandingPayload,
  academicPayload,
  parseStudentCsvPreview,
} from "@/lib/onboarding";
import {
  registerSchool,
  submitOnboardingStep,
  importStudentsCsv,
  ApiError,
} from "@/lib/api";
import { saveAuth, loadAuth } from "@/lib/auth";
import { Stepper } from "./Stepper";
import { Button } from "../ui/Button";
import { Photo } from "../ui/Photo";
import { PHOTOS } from "@/lib/images";
import {
  RegisterStep,
  BasicStep,
  BrandingStep,
  AcademicStep,
  StudentsStep,
  ReviewStep,
} from "./steps";

// ─── Reducer ──────────────────────────────────────────────────────────────────
type Action =
  | { type: "HYDRATE"; state: WizardState }
  | { type: "GOTO"; step: WizardStep }
  | { type: "SET_REGISTER"; patch: Partial<RegisterData> }
  | { type: "SET_BASIC"; patch: Partial<BasicData> }
  | { type: "SET_BRANDING"; patch: Partial<BrandingData> }
  | { type: "SET_ACADEMIC"; classes: ClassRow[] }
  | { type: "SET_STUDENTS"; patch: Partial<StudentsData> };

function reducer(state: WizardState, action: Action): WizardState {
  switch (action.type) {
    case "HYDRATE":
      return action.state;
    case "GOTO":
      return { ...state, step: action.step };
    case "SET_REGISTER":
      return { ...state, register: { ...state.register, ...action.patch } };
    case "SET_BASIC":
      return { ...state, basic: { ...state.basic, ...action.patch } };
    case "SET_BRANDING":
      return { ...state, branding: { ...state.branding, ...action.patch } };
    case "SET_ACADEMIC":
      return { ...state, academic: { classes: action.classes } };
    case "SET_STUDENTS":
      return { ...state, students: { ...state.students, ...action.patch } };
    default:
      return state;
  }
}

function stepIndex(s: WizardStep): number {
  return WIZARD_STEPS.findIndex((x) => x.id === s);
}

export function Wizard() {
  const router = useRouter();
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE);
  const [hydrated, setHydrated] = useState(false);
  const [token, setToken] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [showErrors, setShowErrors] = useState(false);
  const [importNotice, setImportNotice] = useState<string | null>(null);

  // ── Hydrate from localStorage once on mount (state persists across refresh) ──
  useEffect(() => {
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as WizardState;
        dispatch({ type: "HYDRATE", state: { ...INITIAL_STATE, ...parsed } });
      }
    } catch {
      /* ignore corrupt drafts */
    }
    const auth = loadAuth();
    if (auth?.token) setToken(auth.token);
    setHydrated(true);
  }, []);

  // ── Persist on every change (after hydration) ──
  useEffect(() => {
    if (!hydrated) return;
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      /* storage full / disabled, non-fatal */
    }
  }, [state, hydrated]);

  const goTo = (step: WizardStep) => {
    setServerError(null);
    setShowErrors(false);
    if (step !== "REVIEW") setImportNotice(null);
    dispatch({ type: "GOTO", step });
    if (typeof window !== "undefined") window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const back = () => {
    const idx = stepIndex(state.step);
    if (idx > 0) goTo(WIZARD_STEPS[idx - 1].id);
  };

  // ── Per-step "next" handlers (validate → call backend → advance) ──
  async function next() {
    setServerError(null);
    setSubmitting(true);
    try {
      switch (state.step) {
        case "REGISTER": {
          const errs = validateRegister(state.register);
          if (hasErrors(errs)) {
            setShowErrors(true);
            return;
          }
          // If we already registered (resumed draft with a token), skip re-register.
          if (!token) {
            const res = await registerSchool({
              name: state.register.adminName.trim(),
              identifier: state.register.email.trim().toLowerCase(),
              password: state.register.password,
              school_name: state.register.schoolName.trim(),
              device_info: { platform: "web" },
            });
            saveAuth(res);
            setToken(res.token);
          }
          goTo("BASIC");
          break;
        }
        case "BASIC": {
          const errs = validateBasic(state.basic);
          if (hasErrors(errs)) {
            setShowErrors(true);
            return;
          }
          if (token) await submitOnboardingStep(token, "BASIC", basicPayload(state.basic));
          goTo("BRANDING");
          break;
        }
        case "BRANDING": {
          if (token) await submitOnboardingStep(token, "BRANDING", brandingPayload(state.branding));
          goTo("ACADEMIC");
          break;
        }
        case "ACADEMIC": {
          const err = validateAcademic(state.academic);
          if (err) {
            setShowErrors(true);
            setServerError(err);
            return;
          }
          if (token) await submitOnboardingStep(token, "ACADEMIC", academicPayload(state.academic));
          goTo("STUDENTS");
          break;
        }
        case "STUDENTS": {
          // Importing students is optional, an empty CSV just advances.
          const csv = state.students.csv.trim();
          if (csv && token) {
            // Guard against an obviously malformed sheet (missing columns) so the
            // server doesn't reject the whole batch; the live preview already
            // surfaces these, this is the final gate.
            const { rows, errors } = parseStudentCsvPreview(csv);
            const importable = rows.filter(
              (r) => r.full_name && r.class_name && r.roll_number
            );
            if (importable.length === 0) {
              setServerError(
                errors[0] ??
                  "No valid students found in the CSV. Check the header row (full_name, class_name, roll_number)."
              );
              return;
            }
            const res = await importStudentsCsv(token, csv);
            setImportNotice(
              `Imported ${res.inserted} of ${res.total} students` +
                (res.failed > 0 ? ` · ${res.failed} skipped` : "")
            );
          }
          goTo("REVIEW");
          break;
        }
        case "REVIEW": {
          // Final submission stamps onboarded_at on the server.
          if (token) {
            await submitOnboardingStep(token, "REVIEW", {}, true);
          }
          // Clear the draft; auth token stays so success page can greet by name.
          window.localStorage.removeItem(STORAGE_KEY);
          router.push("/onboarding/success");
          break;
        }
      }
    } catch (e) {
      const msg =
        e instanceof ApiError
          ? e.message
          : "Something went wrong. Please try again.";
      setServerError(msg);
    } finally {
      setSubmitting(false);
    }
  }

  if (!hydrated) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <span className="text-sm text-ink-3">Loading your progress…</span>
      </div>
    );
  }

  const isFirst = state.step === "REGISTER";
  const isLast = state.step === "REVIEW";

  return (
    <div className="grid gap-10 lg:grid-cols-[1fr_minmax(0,2fr)]">
      {/* Side rail, photo + reassurance, sticky on desktop. */}
      <aside className="hidden lg:block">
        <div className="sticky top-28">
          <p className="eyebrow mb-3">Onboarding</p>
          <h1 className="display text-3xl leading-[1.1]">
            Set up your school.
          </h1>
          <p className="mt-4 text-sm leading-relaxed text-ink-2">
            Five short steps. Your progress is saved on this device automatically, close
            the tab and come back any time.
          </p>
          <Photo photo={PHOTOS.onboarding} ratio="4/5" className="mt-8" sizes="33vw" />
        </div>
      </aside>

      {/* Form column */}
      <div>
        <Stepper current={state.step} />

        <div className="mt-10 rounded-xl2 border border-navy/8 bg-white/70 p-6 sm:p-8 md:p-10">
          <AnimatePresence mode="wait">
            <motion.div
              key={state.step}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.28, ease: [0.16, 1, 0.3, 1] }}
            >
              {state.step === "REGISTER" && (
                <RegisterStep
                  data={state.register}
                  errors={showErrors ? validateRegister(state.register) : {}}
                  onChange={(patch) => dispatch({ type: "SET_REGISTER", patch })}
                />
              )}
              {state.step === "BASIC" && (
                <BasicStep
                  data={state.basic}
                  errors={showErrors ? validateBasic(state.basic) : {}}
                  onChange={(patch) => dispatch({ type: "SET_BASIC", patch })}
                />
              )}
              {state.step === "BRANDING" && (
                <BrandingStep
                  data={state.branding}
                  onChange={(patch) => dispatch({ type: "SET_BRANDING", patch })}
                />
              )}
              {state.step === "ACADEMIC" && (
                <AcademicStep
                  data={state.academic}
                  onChange={(classes) => dispatch({ type: "SET_ACADEMIC", classes })}
                />
              )}
              {state.step === "STUDENTS" && (
                <StudentsStep
                  data={state.students}
                  onChange={(patch) => dispatch({ type: "SET_STUDENTS", patch })}
                />
              )}
              {state.step === "REVIEW" && <ReviewStep state={state} onJump={goTo} />}
            </motion.div>
          </AnimatePresence>

          {serverError && (
            <p className="mt-6 rounded-xl border border-danger/30 bg-danger/5 px-4 py-3 text-sm font-medium text-danger">
              {serverError}
            </p>
          )}

          {importNotice && !serverError && (
            <p className="mt-6 rounded-xl border border-teal/30 bg-teal/5 px-4 py-3 text-sm font-medium text-teal-deep">
              {importNotice}
            </p>
          )}

          <div className="mt-8 flex items-center justify-between gap-4">
            <Button
              variant="ghost"
              onClick={back}
              disabled={isFirst || submitting}
              type="button"
            >
              ← Back
            </Button>
            <Button onClick={next} disabled={submitting} type="button" size="lg">
              {submitting
                ? "Working…"
                : isLast
                  ? "Launch my school"
                  : isFirst
                    ? "Create account & continue"
                    : state.step === "STUDENTS"
                      ? state.students.csv.trim()
                        ? "Import & continue"
                        : "Skip for now"
                      : "Continue"}
            </Button>
          </div>
        </div>

        <p className="mt-5 text-center text-xs text-ink-3">
          Already have an account?{" "}
          <a href="/login" className="font-semibold text-accent hover:underline">
            Sign in
          </a>
        </p>
      </div>
    </div>
  );
}
