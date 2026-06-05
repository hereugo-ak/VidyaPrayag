import { useEffect, useState } from "react";
import { motion } from "motion/react";
import { ArrowRight, ShieldCheck, Phone, Lock, User, Check, Upload, GraduationCap, Eye, Mail } from "lucide-react";
import { VLogo, VButton, VInput, VCard, PhoneFrame, VTag, VBadge, Label, VAvatar } from "../components/v/primitives";

// ─── Splash ──────────────────────────────────────────────────────────────────
// Premium hero splash. Logo mirrors the Login brand panel (teal glass cube
// with white bridge mark). Animated CTAs reveal after the mark assembles.
export function Splash({ onDone }: { onDone: () => void }) {
  return (
    <div className="flex flex-col mx-auto relative overflow-hidden" style={{ minHeight: "100vh", maxWidth: 440, background: "var(--lavender)" }}>
      {/* Teal hero — same proportions as Login brand panel */}
      <div className="relative overflow-hidden flex flex-col items-center justify-center px-6" style={{ background: "var(--teal)", paddingTop: 80, paddingBottom: 90, minHeight: 440 }}>
        {/* ambient noise + glow */}
        <div className="absolute inset-0 opacity-30" style={{ background: "radial-gradient(circle at 50% 30%, rgba(255,255,255,0.25), transparent 55%)" }} />
        <svg className="absolute top-12 left-10 opacity-30" width="56" height="38" viewBox="0 0 48 32" fill="none">
          <path d="M6 24 Q12 14 22 18 Q28 8 38 14 Q46 14 44 24 Z" stroke="white" strokeWidth="1.5" />
        </svg>
        <svg className="absolute bottom-20 right-10 opacity-25" width="46" height="32" viewBox="0 0 48 32" fill="none">
          <path d="M6 24 Q12 14 22 18 Q28 8 38 14 Q46 14 44 24 Z" stroke="white" strokeWidth="1.5" />
        </svg>

        {/* Logo cube — identical to Login */}
        <motion.div
          initial={{ scale: 0.82, opacity: 0, y: 10 }}
          animate={{ scale: 1, opacity: 1, y: 0 }}
          transition={{ type: "spring", stiffness: 240, damping: 22 }}
          className="w-[160px] h-[160px] rounded-[28px] flex items-center justify-center mb-7 relative"
          style={{ background: "rgba(255,255,255,0.16)", backdropFilter: "blur(8px)", border: "1px solid rgba(255,255,255,0.18)" }}>
          {/* halo */}
          <motion.div className="absolute inset-0 rounded-[28px]" initial={{ opacity: 0 }} animate={{ opacity: [0, 0.6, 0] }} transition={{ duration: 2.4, repeat: Infinity, ease: "easeInOut", delay: 0.6 }} style={{ boxShadow: "0 0 0 12px rgba(255,255,255,0.10)" }} />
          <svg width="100" height="100" viewBox="0 0 56 56" fill="none">
            <motion.path initial={{ pathLength: 0 }} animate={{ pathLength: 1 }} transition={{ duration: 0.9, delay: 0.25 }}
              d="M12 32 Q28 12 44 32" stroke="#ffffff" strokeWidth="3" strokeLinecap="round" fill="none" />
            <motion.path initial={{ pathLength: 0 }} animate={{ pathLength: 1 }} transition={{ duration: 0.5, delay: 0.85 }}
              d="M10 40 H46" stroke="#ffffff" strokeWidth="3.5" strokeLinecap="round" />
            <motion.g initial={{ opacity: 0 }} animate={{ opacity: 0.85 }} transition={{ delay: 1.15, duration: 0.35 }}>
              <path d="M18 32 V40 M28 22 V40 M38 32 V40" stroke="rgba(255,255,255,0.78)" strokeWidth="1.6" strokeLinecap="round" />
              <circle cx="12" cy="32" r="2.6" fill="#ffffff" />
              <circle cx="44" cy="32" r="2.6" fill="#ffffff" />
              <circle cx="28" cy="22" r="2.4" fill="var(--navy)" />
            </motion.g>
          </svg>
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 1.0, duration: 0.5 }}
          style={{ color: "#ffffff", fontSize: 30, fontWeight: 800, letterSpacing: "-0.02em" }}>
          VidyaSetu
        </motion.h1>
        <motion.p
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 1.25, duration: 0.45 }}
          style={{ color: "rgba(255,255,255,0.92)", fontSize: 15, marginTop: 8, textAlign: "center", maxWidth: 280 }}>
          Bridging gaps for a glorious future
        </motion.p>
      </div>

      {/* Sheet with CTAs */}
      <motion.div
        initial={{ y: 80, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ type: "spring", stiffness: 220, damping: 28, delay: 1.45 }}
        className="flex-1 px-6 pt-10 pb-8 -mt-8 flex flex-col"
        style={{ background: "var(--lavender)", borderTopLeftRadius: 32, borderTopRightRadius: 32, boxShadow: "0 -10px 32px rgba(0,0,0,0.06)" }}>

        {/* social proof strip */}
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 1.7, duration: 0.4 }} className="flex items-center justify-center gap-2 mb-6">
          <div className="flex -space-x-2">
            {["#A8E6CF", "#FFD4A3", "#C8DEFF"].map((c, i) => (
              <div key={i} className="w-6 h-6 rounded-full border-2" style={{ background: c, borderColor: "var(--lavender)" }} />
            ))}
          </div>
          <span style={{ fontSize: 12, color: "var(--ink-2)" }}>
            <span style={{ fontWeight: 700, color: "var(--ink)" }}>240+ schools</span> · 38k parents
          </span>
        </motion.div>

        <motion.h2
          initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 1.6, duration: 0.45 }}
          className="text-center"
          style={{ color: "var(--navy)" }}>
          Welcome aboard
        </motion.h2>
        <motion.p
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 1.75, duration: 0.4 }}
          className="text-center mt-2 mb-7"
          style={{ color: "var(--ink-2)", fontSize: 14 }}>
          Sign in to connect with your child's school, or explore as a new family.
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 1.85, duration: 0.45 }}
          className="space-y-3 mt-auto">
          <VButton full size="lg" tone="teal" onClick={onDone}>
            Get started <ArrowRight size={17} />
          </VButton>
          <VButton full size="lg" tone="navy" variant="secondary" onClick={onDone}>
            I already have an account
          </VButton>
          <p className="text-center mt-3" style={{ fontSize: 11, color: "var(--ink-3)" }}>
            By continuing you agree to our <span style={{ color: "var(--teal-deep)", fontWeight: 600 }}>Terms</span> & <span style={{ color: "var(--teal-deep)", fontWeight: 600 }}>Privacy Policy</span>
          </p>
        </motion.div>
      </motion.div>
    </div>
  );
}

// Premium CTA with hover/press micro-states and a subtle sheen sweep.
function SplashCTA({ primary, children, onClick }: { primary?: boolean; children: React.ReactNode; onClick?: () => void }) {
  const [hover, setHover] = useState(false);
  return (
    <motion.button
      whileHover={{ y: -1 }}
      whileTap={{ scale: 0.98, y: 0 }}
      onClick={onClick}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      className="w-full py-4 rounded-[14px] relative overflow-hidden inline-flex items-center justify-center gap-2"
      style={{
        background: primary ? "var(--navy)" : "#ffffff",
        color: primary ? "#ffffff" : "var(--navy)",
        fontSize: 16, fontWeight: 600,
        border: primary ? "none" : "1px solid rgba(38,35,77,0.12)",
        boxShadow: primary
          ? (hover ? "0 14px 28px -8px rgba(38,35,77,0.45)" : "0 8px 20px -6px rgba(38,35,77,0.30)")
          : "0 2px 6px rgba(38,35,77,0.06)",
        transition: "box-shadow 200ms ease",
      }}>
      {primary && (
        <motion.div
          aria-hidden
          className="absolute inset-y-0"
          initial={{ x: "-120%" }}
          animate={{ x: hover ? "220%" : "-120%" }}
          transition={{ duration: 0.9, ease: "easeInOut" }}
          style={{ width: "60%", background: "linear-gradient(90deg, transparent, rgba(255,255,255,0.25), transparent)", transform: "skewX(-20deg)" }}
        />
      )}
      <span className="relative inline-flex items-center gap-2">{children}</span>
    </motion.button>
  );
}

// ─── Login (Figma-faithful, warm) ────────────────────────────────────────────
export function Login({ onLogin }: { onLogin: (portal: "admin" | "teacher" | "parent") => void }) {
  const [portal, setPortal] = useState<"parent" | "admin" | "teacher">("parent");
  const [otpSent, setOtpSent] = useState(false);

  return (
    <div className="flex flex-col mx-auto" style={{ minHeight: "100vh", maxWidth: 440, background: "var(--lavender)" }}>
      {/* Brand panel — teal hero matching the Figma import */}
      <div className="relative overflow-hidden flex flex-col items-center justify-center px-6" style={{ background: "var(--teal)", paddingTop: 56, paddingBottom: 72, minHeight: 360 }}>
        {/* abstract cloud decorations */}
        <svg className="absolute top-10 left-10 opacity-30" width="56" height="38" viewBox="0 0 48 32" fill="none">
          <path d="M6 24 Q12 14 22 18 Q28 8 38 14 Q46 14 44 24 Z" stroke="white" strokeWidth="1.5" />
        </svg>
        <svg className="absolute bottom-20 right-10 opacity-25" width="46" height="32" viewBox="0 0 48 32" fill="none">
          <path d="M6 24 Q12 14 22 18 Q28 8 38 14 Q46 14 44 24 Z" stroke="white" strokeWidth="1.5" />
        </svg>
        <motion.div initial={{ scale: 0.85, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={{ type: "spring", stiffness: 240, damping: 22 }}
          className="w-[160px] h-[160px] rounded-[28px] flex items-center justify-center mb-6" style={{ background: "rgba(255,255,255,0.16)", backdropFilter: "blur(8px)" }}>
          <svg width="100" height="100" viewBox="0 0 56 56" fill="none">
            <path d="M12 32 Q28 12 44 32" stroke="#ffffff" strokeWidth="3" strokeLinecap="round" fill="none" />
            <path d="M10 40 H46" stroke="#ffffff" strokeWidth="3.5" strokeLinecap="round" />
            <path d="M18 32 V40 M28 22 V40 M38 32 V40" stroke="rgba(255,255,255,0.75)" strokeWidth="1.6" strokeLinecap="round" />
            <circle cx="12" cy="32" r="2.6" fill="#ffffff" />
            <circle cx="44" cy="32" r="2.6" fill="#ffffff" />
            <circle cx="28" cy="22" r="2.2" fill="var(--navy)" />
          </svg>
        </motion.div>
        <h2 style={{ color: "#ffffff", fontSize: 22, fontWeight: 700, letterSpacing: "-0.01em" }}>
          Welcome to VidyaSetu. <span style={{ fontFamily: "'Noto Color Emoji', sans-serif" }}>👋</span>
        </h2>
        <p style={{ color: "rgba(255,255,255,0.92)", fontSize: 14, marginTop: 8, textAlign: "center" }}>Bridging gaps for a glorious future</p>
      </div>

      {/* Form sheet — rounded top, lavender bg, shadow */}
      <motion.div initial={{ y: 40, opacity: 0 }} animate={{ y: 0, opacity: 1 }} transition={{ type: "spring", stiffness: 260, damping: 30, delay: 0.1 }}
        className="flex-1 px-5 pt-12 pb-8 -mt-8"
        style={{ background: "var(--lavender)", borderTopLeftRadius: 32, borderTopRightRadius: 32, boxShadow: "0 -8px 30px rgba(0,0,0,0.05)" }}>

        {/* Portal selector tabs */}
        <div className="flex p-1 rounded-[12px] mb-6 relative" style={{ background: "var(--portal-tab-bg)" }}>
          {(["parent", "admin", "teacher"] as const).map((p) => (
            <button key={p} onClick={() => setPortal(p)} className="flex-1 py-2.5 rounded-[8px] transition-all relative z-10"
              style={{
                fontSize: 12, fontWeight: portal === p ? 700 : 600, textTransform: "capitalize",
                background: portal === p ? "#ffffff" : "transparent",
                color: portal === p ? "var(--teal-deep)" : "var(--ink-2)",
                boxShadow: portal === p ? "0 1px 2px rgba(0,0,0,0.05)" : "none",
              }}>
              {p}
            </button>
          ))}
        </div>

        <form className="space-y-5" onSubmit={(e) => { e.preventDefault(); if (portal === "parent") otpSent ? onLogin("parent") : setOtpSent(true); else onLogin(portal); }}>
          {portal === "parent" ? (
            <>
              <VInput label="Mobile number" placeholder="+91 98XXX XXXXX" icon={<Phone size={18} />} />
              {otpSent && <VInput label="OTP" placeholder="6-digit code" icon={<ShieldCheck size={18} />} />}
            </>
          ) : (
            <>
              <VInput
                label={portal === "admin" ? "Email or School ID" : "Teacher credential"}
                placeholder={portal === "admin" ? "office@svm.edu.in" : "SVM001.T07"}
                icon={portal === "admin" ? <Mail size={18} /> : <User size={18} />}
              />
              <div className="relative">
                <VInput label="Password" type="password" placeholder="••••••••" icon={<Lock size={18} />} />
                <button type="button" className="absolute right-4 top-[42px]" style={{ color: "var(--ink-3)" }}><Eye size={18} /></button>
                <div className="flex justify-end mt-1.5">
                  <a style={{ fontSize: 12, fontWeight: 600, color: "var(--teal-deep)" }}>Forgot Password?</a>
                </div>
              </div>
            </>
          )}

          <VButton type="submit" full size="lg" tone="teal">
            {portal === "parent" ? (otpSent ? "Verify & Continue" : "Send OTP") : "Sign In"} <ArrowRight size={16} />
          </VButton>
        </form>

        <div className="flex items-center justify-center pt-6 pb-2">
          <span style={{ fontSize: 14, color: "var(--ink-2)" }}>Not a member?</span>
          <a className="ml-2" style={{ fontSize: 14, fontWeight: 600, color: "var(--warm-orange)" }}>Register Now</a>
        </div>
      </motion.div>
    </div>
  );
}

// ─── Teacher first login ─────────────────────────────────────────────────────
export function TeacherFirstLogin({ onDone }: { onDone: () => void }) {
  return (
    <PhoneFrame dark>
      <div className="px-6 pt-10 pb-6 flex-1 flex flex-col">
        <Label dark>Welcome, Mr. Vikram</Label>
        <h1 className="mt-2">Set a new password</h1>
        <p className="mt-2" style={{ color: "var(--text-dark-2)", fontSize: 14 }}>
          For your security, choose a fresh password before continuing. You'll only do this once.
        </p>
        <div className="mt-8 space-y-4">
          <VInput dark label="Current temporary password" type="password" placeholder="••••••••" />
          <VInput dark label="New password" type="password" placeholder="At least 8 characters" />
          <VInput dark label="Confirm new password" type="password" placeholder="Re-enter" />
        </div>
        <div className="mt-auto pt-6 space-y-3">
          <VButton full size="lg" onClick={onDone}>Update & continue <ArrowRight size={16} /></VButton>
          <button className="w-full text-center" style={{ fontSize: 13, color: "var(--text-dark-3)" }}>Need help signing in?</button>
        </div>
      </div>
    </PhoneFrame>
  );
}

// ─── Parent child-link flow ──────────────────────────────────────────────────
export function ParentLinkChild({ onDone }: { onDone: () => void }) {
  const [step, setStep] = useState(1);
  return (
    <PhoneFrame>
      <div className="px-6 pt-10 pb-6 flex-1 flex flex-col">
        <Label>Step {step} of 3</Label>
        <div className="mt-2 flex gap-1.5">
          {[1, 2, 3].map((s) => (
            <div key={s} className="h-1 flex-1 rounded-full" style={{ background: s <= step ? "var(--arctic)" : "rgba(8,8,8,0.08)" }} />
          ))}
        </div>

        {step === 1 && (
          <>
            <h1 className="mt-6">Tell us about you</h1>
            <p style={{ color: "var(--text-light-2)", fontSize: 14 }}>So your child's school knows who to send updates to.</p>
            <div className="mt-6 space-y-4">
              <VInput label="Your full name" placeholder="e.g. Sneha Sharma" />
              <div>
                <Label>Preferred language</Label>
                <div className="flex gap-2 mt-2">
                  <VTag active>English</VTag><VTag>हिन्दी</VTag>
                </div>
              </div>
            </div>
          </>
        )}
        {step === 2 && (
          <>
            <h1 className="mt-6">Find your child's school</h1>
            <p style={{ color: "var(--text-light-2)", fontSize: 14 }}>Type the school name. We'll match it against schools using VidyaSetu.</p>
            <div className="mt-6 space-y-3">
              <VInput placeholder="Search by school name" />
              <VCard className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full inline-flex items-center justify-center" style={{ background: "var(--arctic)" }}>
                  <GraduationCap size={18} />
                </div>
                <div className="flex-1">
                  <div style={{ fontWeight: 600 }}>Saraswati Vidya Mandir</div>
                  <div style={{ fontSize: 12, color: "var(--text-light-2)" }}>Lucknow • CBSE • 0.6 km</div>
                </div>
                <VBadge tone="arctic">Match</VBadge>
              </VCard>
            </div>
          </>
        )}
        {step === 3 && (
          <>
            <h1 className="mt-6">Link your child</h1>
            <p style={{ color: "var(--text-light-2)", fontSize: 14 }}>Enter the roll or admission number assigned by the school.</p>
            <div className="mt-6 space-y-4">
              <VInput label="Roll / admission number" placeholder="e.g. 02" />
              <VCard>
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-full" style={{ background: "var(--arctic)" }} />
                  <div>
                    <div style={{ fontWeight: 600 }}>Riya Sharma</div>
                    <div style={{ fontSize: 12, color: "var(--text-light-2)" }}>Class 10 – A • Roll 02</div>
                  </div>
                  <Check className="ml-auto" size={18} style={{ color: "#155e3a" }} />
                </div>
              </VCard>
              <button className="text-center w-full" style={{ fontSize: 13, color: "#0a3a76", fontWeight: 600 }}>+ Add another child</button>
            </div>
          </>
        )}

        <div className="mt-auto pt-6">
          <VButton full size="lg" onClick={() => (step < 3 ? setStep(step + 1) : onDone())}>
            {step < 3 ? "Continue" : "Finish & open dashboard"} <ArrowRight size={16} />
          </VButton>
        </div>
      </div>
    </PhoneFrame>
  );
}

// ─── School Onboarding (6 step wizard) ───────────────────────────────────────
type OBClass = { name: string; sections: string[] };
type OBSubject = { id: string; name: string; code: string; type: string; classes: string[] };
type OBTeacher = { id: string; name: string; mobile: string; username: string; assignments: { subject: string; klass: string }[] };

export function SchoolOnboarding({ onDone }: { onDone: () => void }) {
  const [step, setStep] = useState(1);
  const titles = ["School identity", "Academic year", "Classes & sections", "Subjects", "Teachers", "Students"];
  const [classesBuilt, setClassesBuilt] = useState<OBClass[]>([
    { name: "Class 9", sections: ["A", "B"] },
    { name: "Class 10", sections: ["A", "B"] },
  ]);
  // Flattened "Class 9-A" style codes derived from classesBuilt
  const classCodes = classesBuilt.flatMap((c) => c.sections.map((s) => `${c.name.replace("Class ", "")}-${s}`));

  const [subjects, setSubjects] = useState<OBSubject[]>([
    { id: "s1", name: "Mathematics", code: "MAT001", type: "Core", classes: [] },
    { id: "s2", name: "Science", code: "SCI001", type: "Core", classes: [] },
    { id: "s3", name: "English", code: "ENG001", type: "Core", classes: [] },
    { id: "s4", name: "Hindi", code: "HIN001", type: "Language", classes: [] },
    { id: "s5", name: "Social Studies", code: "SOC001", type: "Core", classes: [] },
    { id: "s6", name: "Computer Apps", code: "COMP01", type: "Core", classes: [] },
  ]);

  const [teachers, setTeachers] = useState<OBTeacher[]>([
    { id: "t1", name: "Dr. Ramesh Sharma", mobile: "+91 98100 12121", username: "SVM.T01", assignments: [] },
    { id: "t2", name: "Mrs. Priya Iyer",   mobile: "+91 98100 13131", username: "SVM.T02", assignments: [] },
    { id: "t3", name: "Mr. Arjun Mehta",   mobile: "+91 98100 14141", username: "SVM.T03", assignments: [] },
  ]);

  // Auto-apply: when a subject is first created, it should target all classCodes by default.
  // Done lazily when step 4 mounts — keep classes already chosen, but seed empty ones.
  const seedSubjectClasses = () => {
    setSubjects((prev) => prev.map((s) => s.classes.length ? s : { ...s, classes: [...classCodes] }));
  };

  // Inline-add draft state for manual entries
  const [newClassName, setNewClassName] = useState("");
  const [newSubject, setNewSubject] = useState({ name: "", code: "", type: "Core" });
  const [newTeacher, setNewTeacher] = useState({ name: "", mobile: "" });

  // All slots that need a teacher
  const allSlots = subjects.flatMap((s) => s.classes.map((k) => ({ subject: s.name, klass: k })));
  const assignedSlots = teachers.flatMap((t) => t.assignments);
  const coveredCount = allSlots.filter((slot) =>
    assignedSlots.some((a) => a.subject === slot.subject && a.klass === slot.klass)
  ).length;
  const coverage = allSlots.length ? Math.round((coveredCount / allSlots.length) * 100) : 0;

  if (step > 6) {
    return (
      <PhoneFrame dark>
        <div className="flex-1 overflow-y-auto">
          {/* Quiet teal hero — single panel, no confetti */}
          <div className="relative overflow-hidden px-6 pt-16 pb-14 text-center"
            style={{ background: "linear-gradient(180deg, var(--teal) 0%, #2a8f80 100%)" }}>
            <div aria-hidden className="absolute inset-0 opacity-40"
              style={{ background: "radial-gradient(circle at 50% 30%, rgba(255,255,255,0.30), transparent 55%)" }} />

            <motion.div initial={{ scale: 0.7, opacity: 0 }} animate={{ scale: 1, opacity: 1 }}
              transition={{ duration: 0.55, type: "spring", stiffness: 200, damping: 18 }}
              className="relative inline-block">
              <div className="w-24 h-24 rounded-[26px] inline-flex items-center justify-center"
                style={{ background: "rgba(255,255,255,0.18)", backdropFilter: "blur(14px)", border: "1px solid rgba(255,255,255,0.30)" }}>
                <motion.div initial={{ scale: 0, rotate: -30 }} animate={{ scale: 1, rotate: 0 }} transition={{ delay: 0.4, type: "spring", stiffness: 300 }}>
                  <Check size={44} strokeWidth={3} color="#ffffff" />
                </motion.div>
              </div>
            </motion.div>

            <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.5 }}>
              <h1 className="mt-7" style={{ color: "#ffffff", letterSpacing: "-0.02em", fontSize: 30 }}>
                You're all set
              </h1>
              <p style={{ color: "rgba(255,255,255,0.88)", fontSize: 14, marginTop: 6 }}>
                Saraswati Vidya Mandir is live on VidyaSetu.
              </p>
            </motion.div>
          </div>

          {/* Body — lavender, clean cards */}
          <div className="px-5 pt-6 pb-8 space-y-4">
            {/* Single inline stat bar */}
            <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.65 }}>
              <VCard>
                <div className="flex items-stretch justify-between">
                  {[["6", "Classes"], ["8", "Teachers"], ["180", "Students"], ["156", "Parents"]].map(([n, l], i, arr) => (
                    <div key={l} className="flex-1 text-center" style={{ borderRight: i < arr.length - 1 ? "1px solid rgba(38,35,77,0.06)" : "none" }}>
                      <div className="font-mono" style={{ fontSize: 22, fontWeight: 700, color: "var(--navy)", lineHeight: 1 }}>{n}</div>
                      <div className="mt-1" style={{ fontSize: 10, color: "var(--ink-3)", fontWeight: 700, letterSpacing: "0.06em", textTransform: "uppercase" }}>{l}</div>
                    </div>
                  ))}
                </div>
              </VCard>
            </motion.div>

            {/* Quick-start tiles */}
            <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.75 }}>
              <Label>Get started</Label>
              <div className="mt-2 grid grid-cols-1 gap-2.5">
                {[
                  { icon: <Mail size={16} />, t: "Email teachers their credentials", s: "8 invites · sent in one tap" },
                  { icon: <Upload size={16} />, t: "Download parent invite pack", s: "Personalised PDF · 156 parents" },
                  { icon: <ShieldCheck size={16} />, t: "Review attendance permissions", s: "Class teachers only by default" },
                ].map((q) => (
                  <button key={q.t} className="w-full text-left rounded-[14px] p-3.5 transition-all hover:-translate-y-[1px]"
                    style={{ background: "var(--cloud-pure)", border: "1px solid var(--border-light-1)", boxShadow: "var(--shadow-light-1)" }}>
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-[10px] inline-flex items-center justify-center" style={{ background: "#dcf2ef", color: "#006a60" }}>
                        {q.icon}
                      </div>
                      <div className="flex-1">
                        <div style={{ fontSize: 13, fontWeight: 700, color: "var(--ink)" }}>{q.t}</div>
                        <div style={{ fontSize: 11, color: "var(--ink-3)" }}>{q.s}</div>
                      </div>
                      <ArrowRight size={16} style={{ color: "var(--ink-3)" }} />
                    </div>
                  </button>
                ))}
              </div>
            </motion.div>

            <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.9 }} className="pt-2">
              <VButton full size="lg" tone="teal" onClick={onDone}>
                Open dashboard <ArrowRight size={16} />
              </VButton>
              <div className="mt-3 text-center" style={{ fontSize: 11, color: "var(--ink-3)" }}>
                You can edit any of this later in Settings.
              </div>
            </motion.div>
          </div>
        </div>
      </PhoneFrame>
    );
  }

  return (
    <PhoneFrame dark>
      <div className="px-5 pt-6 pb-4">
        <Label dark>Onboarding</Label>
        <div className="mt-3 flex items-center gap-1.5">
          {titles.map((_, i) => (
            <div key={i} className="flex-1 h-1 rounded-full" style={{ background: i < step ? "var(--arctic)" : "rgba(245,245,243,0.08)" }} />
          ))}
        </div>
        <h2 className="mt-4" style={{ color: "var(--cloud)" }}>{titles[step - 1]}</h2>
        <p style={{ color: "var(--text-dark-2)", fontSize: 13 }}>Step {step} of 6</p>
      </div>

      <div className="px-5 pb-4 flex-1 overflow-y-auto space-y-4">
        {step === 1 && (
          <>
            <VInput dark label="Full legal name" placeholder="Saraswati Vidya Mandir" />
            <VInput dark label="Short name" placeholder="SVM" />
            <VInput dark label="Affiliation number" placeholder="UP/CBSE/2021/4421" />
            <div>
              <Label dark>Board</Label>
              <div className="mt-2 flex flex-wrap gap-2">
                {["CBSE", "ICSE", "UP State", "Other"].map((b, i) => <VTag dark key={b} active={i === 0}>{b}</VTag>)}
              </div>
            </div>
            <div>
              <Label dark>School type</Label>
              <div className="mt-2 flex flex-wrap gap-2">
                {["Government", "Private Aided", "Private Unaided", "Central"].map((b, i) => <VTag dark key={b} active={i === 2}>{b}</VTag>)}
              </div>
            </div>
            <VInput dark label="Principal's name" placeholder="Dr. Anita Verma" />
            <VInput dark label="Principal's mobile" placeholder="+91 98XXX XXXXX" />
          </>
        )}
        {step === 2 && (
          <>
            <div>
              <Label dark>Current academic year</Label>
              <div className="mt-2 flex gap-2">
                {["2025-26", "2026-27"].map((y, i) => <VTag dark key={y} active={i === 0}>{y}</VTag>)}
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <VInput dark label="Year starts" placeholder="01 Apr 2025" />
              <VInput dark label="Year ends" placeholder="31 Mar 2026" />
            </div>
            <div>
              <Label dark>Working days</Label>
              <div className="mt-2 flex gap-2"><VTag dark>Mon–Fri</VTag><VTag dark active>Mon–Sat</VTag></div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <VInput dark label="Start time" placeholder="08:00 AM" />
              <VInput dark label="End time" placeholder="02:00 PM" />
            </div>
            <VInput dark label="Periods per day" placeholder="8" />
          </>
        )}
        {step === 3 && (
          <>
            <VCard dark>
              <Label dark>Tip</Label>
              <p className="mt-1" style={{ fontSize: 12, color: "var(--text-dark-2)" }}>
                Pick the sections your school actually runs. Subjects and teachers in the next steps will only show these classes.
              </p>
            </VCard>
            {classesBuilt.map((c, idx) => (
              <VCard dark key={idx}>
                <div className="flex items-center justify-between mb-2">
                  <div style={{ fontWeight: 600 }}>{c.name}</div>
                  <span className="font-mono" style={{ fontSize: 11, color: "var(--ink-3)" }}>{c.sections.length} sections</span>
                </div>
                <div className="flex flex-wrap gap-1.5">
                  {["A", "B", "C", "D", "E", "F"].map((s) => {
                    const on = c.sections.includes(s);
                    return (
                      <button key={s} onClick={() => {
                        const next = [...classesBuilt];
                        next[idx] = { ...c, sections: on ? c.sections.filter((x) => x !== s) : [...c.sections, s].sort() };
                        setClassesBuilt(next);
                      }}>
                        <VTag dark active={on}>{s}</VTag>
                      </button>
                    );
                  })}
                </div>
              </VCard>
            ))}
            <VCard dark>
              <Label dark>Add class manually</Label>
              <div className="mt-2 flex gap-2">
                <input
                  value={newClassName}
                  onChange={(e) => setNewClassName(e.target.value)}
                  placeholder="e.g. Class 11, Nursery, KG"
                  className="flex-1 px-3 py-2.5 rounded-[10px] outline-none"
                  style={{ background: "var(--cream)", fontSize: 13, color: "var(--ink)" }}
                />
                <VButton tone="teal" size="sm" disabled={!newClassName.trim()}
                  onClick={() => { setClassesBuilt([...classesBuilt, { name: newClassName.trim(), sections: ["A"] }]); setNewClassName(""); }}>
                  Add
                </VButton>
              </div>
              <div className="mt-2 flex flex-wrap gap-1.5">
                {["Class 11", "Class 12", "Nursery", "LKG", "UKG"].map((q) => (
                  <button key={q} onClick={() => setClassesBuilt([...classesBuilt, { name: q, sections: ["A"] }])}>
                    <VTag dark>+ {q}</VTag>
                  </button>
                ))}
              </div>
            </VCard>
          </>
        )}
        {step === 4 && (
          <>
            <VCard dark>
              <div className="flex items-center justify-between">
                <div>
                  <Label dark>Subjects offered</Label>
                  <div className="mt-0.5" style={{ fontSize: 12, color: "var(--ink-3)" }}>Tap a subject's class chips to set where it's taught.</div>
                </div>
                <button onClick={seedSubjectClasses} style={{ fontSize: 12, fontWeight: 700, color: "var(--teal-deep)" }}>
                  Apply to all
                </button>
              </div>
            </VCard>
            {subjects.map((s, sIdx) => (
              <VCard dark key={s.id} padded>
                <div className="flex items-center justify-between mb-2.5">
                  <div>
                    <div style={{ fontWeight: 600 }}>{s.name}</div>
                    <div className="font-mono" style={{ fontSize: 11, color: "var(--ink-3)" }}>{s.code} · {s.type}</div>
                  </div>
                  <VBadge tone={s.classes.length === 0 ? "warning" : "success"}>
                    {s.classes.length === 0 ? "No classes" : `${s.classes.length} / ${classCodes.length}`}
                  </VBadge>
                </div>
                <div className="flex flex-wrap gap-1.5">
                  {classCodes.map((cc) => {
                    const on = s.classes.includes(cc);
                    return (
                      <button key={cc} onClick={() => {
                        const next = [...subjects];
                        next[sIdx] = { ...s, classes: on ? s.classes.filter((x) => x !== cc) : [...s.classes, cc] };
                        setSubjects(next);
                      }}>
                        <VTag dark active={on}>{cc}</VTag>
                      </button>
                    );
                  })}
                </div>
              </VCard>
            ))}
            <VCard dark>
              <Label dark>Add subject manually</Label>
              <div className="mt-2 grid grid-cols-2 gap-2">
                <input value={newSubject.name} onChange={(e) => setNewSubject({ ...newSubject, name: e.target.value })}
                  placeholder="Subject name" className="px-3 py-2.5 rounded-[10px] outline-none"
                  style={{ background: "var(--cream)", fontSize: 13, color: "var(--ink)" }} />
                <input value={newSubject.code} onChange={(e) => setNewSubject({ ...newSubject, code: e.target.value.toUpperCase() })}
                  placeholder="Code" className="px-3 py-2.5 rounded-[10px] outline-none font-mono"
                  style={{ background: "var(--cream)", fontSize: 13, color: "var(--ink)" }} />
              </div>
              <div className="mt-2 flex gap-1.5 flex-wrap">
                {["Core", "Language", "Practical", "Co-curricular"].map((t) => (
                  <button key={t} onClick={() => setNewSubject({ ...newSubject, type: t })}>
                    <VTag dark active={newSubject.type === t}>{t}</VTag>
                  </button>
                ))}
              </div>
              <VButton full className="mt-3" tone="teal" disabled={!newSubject.name.trim()}
                onClick={() => {
                  const code = newSubject.code.trim() || `SUB${(subjects.length+1).toString().padStart(2,"0")}`;
                  setSubjects([...subjects, { id: `s${subjects.length+1}`, name: newSubject.name.trim(), code, type: newSubject.type, classes: [...classCodes] }]);
                  setNewSubject({ name: "", code: "", type: "Core" });
                }}>
                + Add subject
              </VButton>
            </VCard>
          </>
        )}
        {step === 5 && (
          <>
            {/* Live coverage tracker */}
            <VCard dark>
              <div className="flex items-center justify-between mb-2">
                <div>
                  <Label dark>Teacher coverage</Label>
                  <div className="mt-0.5" style={{ fontSize: 12, color: "var(--ink-3)" }}>
                    {coveredCount} of {allSlots.length} subject × class slots assigned
                  </div>
                </div>
                <div className="font-mono" style={{ fontSize: 22, fontWeight: 700, color: coverage === 100 ? "var(--success-ink)" : coverage > 50 ? "var(--teal-deep)" : "var(--warning-ink)" }}>
                  {coverage}%
                </div>
              </div>
              <div className="h-2 rounded-full overflow-hidden" style={{ background: "var(--cream)" }}>
                <motion.div initial={{ width: 0 }} animate={{ width: `${coverage}%` }} transition={{ duration: 0.5, ease: "easeOut" }}
                  className="h-full" style={{ background: coverage === 100 ? "var(--success-ink)" : coverage > 50 ? "var(--teal-deep)" : "var(--warning)" }} />
              </div>
              {coverage < 100 && allSlots.length > 0 && (
                <div className="mt-2" style={{ fontSize: 11, color: "var(--warning-ink)" }}>
                  {allSlots.length - coveredCount} unassigned — keep adding assignments below.
                </div>
              )}
            </VCard>

            {teachers.map((t, tIdx) => (
              <VCard dark key={t.id}>
                <div className="flex items-center gap-3 mb-3">
                  <VAvatar name={t.name} size={40} />
                  <div className="flex-1">
                    <div style={{ fontWeight: 600 }}>{t.name}</div>
                    <div className="font-mono" style={{ fontSize: 11, color: "var(--ink-3)" }}>{t.username} · {t.mobile}</div>
                  </div>
                  <VBadge tone={t.assignments.length ? "arctic" : "neutral"}>{t.assignments.length} slots</VBadge>
                </div>

                {/* Assignment matrix — subjects rows, classes columns */}
                <div className="rounded-[10px] overflow-hidden" style={{ border: "1px solid rgba(38,35,77,0.08)" }}>
                  <div className="grid" style={{ gridTemplateColumns: `110px repeat(${classCodes.length}, 1fr)`, background: "var(--cream)" }}>
                    <div className="px-2.5 py-2" style={{ fontSize: 10, fontWeight: 700, color: "var(--ink-3)", textTransform: "uppercase", letterSpacing: "0.05em" }}>Subject</div>
                    {classCodes.map((cc) => (
                      <div key={cc} className="px-1 py-2 text-center font-mono" style={{ fontSize: 10, fontWeight: 700, color: "var(--ink-3)" }}>{cc}</div>
                    ))}
                  </div>
                  {subjects.filter((s) => s.classes.length > 0).map((s, rowI) => (
                    <div key={s.id} className="grid items-center" style={{ gridTemplateColumns: `110px repeat(${classCodes.length}, 1fr)`, borderTop: rowI ? "1px solid rgba(38,35,77,0.05)" : "none" }}>
                      <div className="px-2.5 py-2" style={{ fontSize: 12, fontWeight: 600, color: "var(--ink)" }}>{s.name}</div>
                      {classCodes.map((cc) => {
                        const inSubject = s.classes.includes(cc);
                        const mine = t.assignments.some((a) => a.subject === s.name && a.klass === cc);
                        const takenByOther = !mine && teachers.some((other) => other.id !== t.id && other.assignments.some((a) => a.subject === s.name && a.klass === cc));
                        return (
                          <button key={cc}
                            disabled={!inSubject || takenByOther}
                            onClick={() => {
                              const next = [...teachers];
                              next[tIdx] = {
                                ...t,
                                assignments: mine
                                  ? t.assignments.filter((a) => !(a.subject === s.name && a.klass === cc))
                                  : [...t.assignments, { subject: s.name, klass: cc }],
                              };
                              setTeachers(next);
                            }}
                            className="h-8 m-1 rounded-[6px] inline-flex items-center justify-center"
                            style={{
                              background: !inSubject ? "transparent"
                                : mine ? "var(--teal-deep)"
                                : takenByOther ? "var(--cream)"
                                : "rgba(60,185,169,0.10)",
                              color: mine ? "#ffffff" : takenByOther ? "var(--ink-3)" : "var(--teal-deep)",
                              fontSize: 11, fontWeight: 700,
                              border: inSubject && !mine && !takenByOther ? "1px dashed rgba(0,106,96,0.35)" : "none",
                              opacity: !inSubject ? 0.25 : 1,
                              cursor: !inSubject || takenByOther ? "not-allowed" : "pointer",
                            }}>
                            {mine ? "✓" : takenByOther ? "—" : inSubject ? "+" : ""}
                          </button>
                        );
                      })}
                    </div>
                  ))}
                </div>
                {t.assignments.length > 0 && (
                  <div className="mt-2.5 flex flex-wrap gap-1.5">
                    {t.assignments.map((a, i) => (
                      <span key={i} className="font-mono px-2 py-0.5 rounded-full" style={{ background: "rgba(60,185,169,0.15)", color: "var(--teal-deep)", fontSize: 11, fontWeight: 600 }}>
                        {a.subject.slice(0, 4)}·{a.klass}
                      </span>
                    ))}
                  </div>
                )}
              </VCard>
            ))}

            <VCard dark>
              <Label dark>Add teacher manually</Label>
              <div className="mt-2 grid grid-cols-2 gap-2">
                <input value={newTeacher.name} onChange={(e) => setNewTeacher({ ...newTeacher, name: e.target.value })}
                  placeholder="Full name" className="px-3 py-2.5 rounded-[10px] outline-none"
                  style={{ background: "var(--cream)", fontSize: 13, color: "var(--ink)" }} />
                <input value={newTeacher.mobile} onChange={(e) => setNewTeacher({ ...newTeacher, mobile: e.target.value })}
                  placeholder="+91 mobile" className="px-3 py-2.5 rounded-[10px] outline-none font-mono"
                  style={{ background: "var(--cream)", fontSize: 13, color: "var(--ink)" }} />
              </div>
              <div className="mt-2" style={{ fontSize: 11, color: "var(--ink-3)" }}>
                Username auto-generated as <span className="font-mono">SVM.T0{teachers.length+1}</span>. They'll receive a one-time password via SMS.
              </div>
              <VButton full className="mt-3" tone="teal" disabled={!newTeacher.name.trim()}
                onClick={() => {
                  setTeachers([...teachers, {
                    id: `t${teachers.length+1}`,
                    name: newTeacher.name.trim(),
                    mobile: newTeacher.mobile.trim() || "+91 98XXX XXXXX",
                    username: `SVM.T0${teachers.length+1}`,
                    assignments: [],
                  }]);
                  setNewTeacher({ name: "", mobile: "" });
                }}>
                + Add teacher
              </VButton>
            </VCard>
            <VButton full tone="sand"><Upload size={14} /> Import roster from CSV</VButton>
          </>
        )}
        {step === 6 && (
          <>
            <VCard dark className="text-center py-8">
              <Upload size={32} style={{ margin: "0 auto", opacity: 0.5 }} />
              <div className="mt-3" style={{ fontWeight: 600 }}>Drop your students CSV here</div>
              <div style={{ fontSize: 12, color: "var(--text-dark-2)" }}>or tap to browse</div>
              <VButton variant="secondary" dark size="sm" className="mt-4">Download template</VButton>
            </VCard>
            <VCard dark>
              <div className="flex items-center justify-between mb-2">
                <div style={{ fontWeight: 600 }}>Preview: 347 rows detected</div>
                <VBadge tone="success">Validated</VBadge>
              </div>
              <div className="space-y-2" style={{ fontSize: 12, color: "var(--text-dark-2)" }}>
                <div className="flex justify-between"><span>Valid records</span><span className="font-mono">344</span></div>
                <div className="flex justify-between"><span>Duplicate roll numbers</span><span className="font-mono" style={{ color: "var(--danger-ink)" }}>3</span></div>
                <div className="flex justify-between"><span>Parent accounts to create</span><span className="font-mono">312</span></div>
              </div>
            </VCard>
          </>
        )}
      </div>

      <div className="px-5 pb-6 pt-2 flex gap-2" style={{ borderTop: "1px solid var(--border-dark-1)" }}>
        {step > 1 && <VButton variant="ghost" dark onClick={() => setStep(step - 1)}>Back</VButton>}
        <VButton full size="lg" tone={step === 6 ? "teal" : "navy"} stateful={step === 6}
          successLabel="Setting up" onClick={() => setStep(step + 1)}>
          {step < 6 ? "Continue" : "Finish setup"} <ArrowRight size={16} />
        </VButton>
      </div>
    </PhoneFrame>
  );
}
