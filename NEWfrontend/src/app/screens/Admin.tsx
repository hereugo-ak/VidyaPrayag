import { useState, ReactNode } from "react";
import {
  Home, Users, ClipboardList, Megaphone, Settings as SettingsIcon, Bell, Search, ChevronRight,
  Plus, Download, Upload, Send, Calendar, FileText, MessageSquare, BookOpen, Wallet, GraduationCap, Filter,
  AlertCircle, Check, Clock, X, ChevronDown,
} from "lucide-react";
import {
  PhoneFrame, VBottomNav, VBackHeader, VCard, VBadge, VTag, VAvatar, VButton, VInput,
  VStatusDot, VProgressBar, VProgressRing, VTopTabs, VComingSoon, VDivider, Label, VEmptyState,
} from "../components/v/primitives";
import { PEWSPreview } from "../components/v/mockups";
import { VBars, VSparkline, VLegendDot } from "../components/v/charts";
import {
  school, classes, teachers, students, classAttendanceGrid, syllabusCoverage,
  recentTeacherActivity, pendingActions, announcements, messagesInbox, notifications, feeBreakdown, feeHistory,
} from "../lib/mock";

const navItems = [
  { id: "home", label: "Home", icon: <Home size={22} /> },
  { id: "people", label: "People", icon: <Users size={22} /> },
  { id: "records", label: "Records", icon: <ClipboardList size={22} /> },
  { id: "comms", label: "Comms", icon: <Megaphone size={22} />, badge: 2 },
  { id: "settings", label: "Settings", icon: <SettingsIcon size={22} /> },
];

export function AdminApp({ onExit }: { onExit: () => void }) {
  const [tab, setTab] = useState("home");
  const [detail, setDetail] = useState<null | { kind: "student" | "teacher" | "announcement"; id: string }>(null);

  if (detail?.kind === "student") return <StudentDetail id={detail.id} onBack={() => setDetail(null)} />;
  if (detail?.kind === "teacher") return <TeacherDetail id={detail.id} onBack={() => setDetail(null)} />;
  if (detail?.kind === "announcement") return <AnnouncementDetail id={detail.id} onBack={() => setDetail(null)} dark />;

  return (
    <PhoneFrame dark className="dark">
      <div className="flex-1 overflow-y-auto pb-4">
        {tab === "home" && <AdminHome onExit={onExit} />}
        {tab === "people" && <People onOpenStudent={(id) => setDetail({ kind: "student", id })} onOpenTeacher={(id) => setDetail({ kind: "teacher", id })} />}
        {tab === "records" && <Records />}
        {tab === "comms" && <Comms onOpenAnnouncement={(id) => setDetail({ kind: "announcement", id })} />}
        {tab === "settings" && <SettingsScreen />}
      </div>
      <VBottomNav dark items={navItems} value={tab} onChange={setTab} />
    </PhoneFrame>
  );
}

// ─── Admin Home ──────────────────────────────────────────────────────────────
function AdminHome({ onExit }: { onExit: () => void }) {
  return (
    <div className="px-5 pt-6 pb-6 space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full inline-flex items-center justify-center" style={{ background: "var(--arctic)" }}>
            <GraduationCap size={18} color="#080808" />
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: 14 }}>{school.shortName}</div>
            <div style={{ fontSize: 11, color: "var(--text-dark-2)" }}>Day {school.dayOfYear} of {school.totalDays}</div>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button className="relative w-10 h-10 rounded-full inline-flex items-center justify-center" style={{ background: "rgba(245,245,243,0.06)" }}>
            <Bell size={18} />
            <span className="absolute top-2 right-2.5 rounded-full" style={{ width: 6, height: 6, background: "var(--danger)" }} />
          </button>
          <button onClick={onExit}><VAvatar name="Admin Office" size={36} /></button>
        </div>
      </div>

      <div>
        <h1>Good afternoon</h1>
        <p style={{ color: "var(--text-dark-2)" }}>Here's how Saraswati Vidya Mandir is doing today.</p>
      </div>

      {/* Glance cards (horizontal scroll) */}
      <div className="-mx-5 px-5 flex gap-3 overflow-x-auto no-scrollbar">
        <GlanceCard title="Attendance today" big="87%" sub="274 present / 316 total" ring={87} cta="View details" />
        <GlanceCard title="Pending from teachers" big="3" sub="haven't marked yet" chips={["Mr. Pillai", "Ms. Bose", "+1"]} cta="Send reminder" />
        <GlanceCard title="Fee collection" big="₹ 24,500" sub="₹ 2,18,400 outstanding" bar={42} cta="Fee dashboard" />
        <GlanceCard title="Upcoming" big="PTM" sub="Class 10 — Tomorrow 10 AM" cta="View calendar" />
      </div>

      {/* Class attendance grid */}
      <VCard dark>
        <div className="flex items-center justify-between mb-3">
          <h3>Attendance by class</h3>
          <span style={{ fontSize: 11, color: "var(--text-dark-2)" }}>Today</span>
        </div>
        <div className="grid grid-cols-3 gap-2.5">
          {classAttendanceGrid.map((c) => {
            const tone = c.pct >= 80 ? "rgba(200,222,255,0.30)" : c.pct >= 60 ? "rgba(255,212,163,0.45)" : "rgba(255,173,168,0.45)";
            const fg = c.pct >= 80 ? "#0a3a76" : c.pct >= 60 ? "#7a3f00" : "#7a1c18";
            return (
              <div key={c.id} className="rounded-[10px] p-3" style={{ background: tone, color: fg }}>
                <div style={{ fontSize: 11, fontWeight: 600 }}>{c.name} {c.section}</div>
                <div className="font-mono mt-1" style={{ fontSize: 18, fontWeight: 500 }}>{c.pct}%</div>
              </div>
            );
          })}
        </div>
      </VCard>

      {/* Syllabus coverage */}
      <VCard dark>
        <div className="flex items-center justify-between mb-3">
          <h3>Syllabus coverage</h3>
          <button style={{ fontSize: 12, color: "var(--arctic)" }}>Full report</button>
        </div>
        <div className="space-y-3">
          {syllabusCoverage.map((s) => (
            <div key={s.class}>
              <div className="flex items-center justify-between mb-1">
                <span style={{ fontSize: 13 }}>{s.class}</span>
                <span className="font-mono" style={{ fontSize: 12 }}>{s.pct}%</span>
              </div>
              <VProgressBar dark value={s.pct} tone={s.pct < 70 ? "warning" : "arctic"} />
            </div>
          ))}
        </div>
      </VCard>

      {/* Subject performance — last week */}
      <VCard dark>
        <div className="flex items-center justify-between mb-3">
          <div>
            <h3>Subject performance</h3>
            <div style={{ fontSize: 11, color: "var(--text-dark-2)" }}>Class 10 averages · last 7 days</div>
          </div>
          <div className="text-right">
            <div className="font-mono" style={{ fontSize: 18, fontWeight: 600 }}>78%</div>
            <VSparkline values={[72, 70, 74, 71, 75, 76, 78]} width={84} height={28} />
          </div>
        </div>
        <VBars data={[
          { label: "Math", value: 74 },
          { label: "Sci", value: 81 },
          { label: "Eng", value: 86 },
          { label: "Hin", value: 72 },
          { label: "Soc", value: 69 },
          { label: "Today", value: 78 },
        ]} />
        <div className="mt-3 flex gap-4">
          <VLegendDot color="rgba(60,185,169,0.45)" label="Week" />
          <VLegendDot color="var(--teal-deep)" label="Today" value="78%" />
        </div>
      </VCard>

      {/* Teacher activity */}
      <VCard dark>
        <h3 className="mb-2">Teacher activity</h3>
        <div className="space-y-3">
          {recentTeacherActivity.map((a, i) => (
            <div key={i} className="flex items-start gap-3">
              <VAvatar name={a.who} size={32} />
              <div className="flex-1">
                <div style={{ fontSize: 13 }}><span style={{ fontWeight: 600 }}>{a.who}</span> {a.what}</div>
                <div style={{ fontSize: 11, color: "var(--text-dark-3)" }}>{a.when}</div>
              </div>
            </div>
          ))}
        </div>
      </VCard>

      {/* PEWS — Predictive Early Warning */}
      <div>
        <h3 className="mb-2">Early-warning radar</h3>
        <VComingSoon title="PEWS — Predictive Early Warning" description="Combines attendance, marks, fee status and behavioural signals to surface at-risk students before exam season." preview={<PEWSPreview />} />
      </div>

      {/* Pending actions */}
      <div>
        <h3 className="mb-2">Pending actions</h3>
        <div className="space-y-2">
          {pendingActions.map((p, i) => (
            <VCard dark key={i}>
              <div className="flex items-start gap-3">
                <AlertCircle size={18} style={{ color: "var(--warning-ink)" }} className="mt-0.5" />
                <div className="flex-1">
                  <div style={{ fontSize: 13, fontWeight: 600 }}>{p.title}</div>
                  <div style={{ fontSize: 11, color: "var(--text-dark-2)" }}>{p.sub}</div>
                </div>
                <VButton size="sm" variant="secondary" dark>{p.cta}</VButton>
              </div>
            </VCard>
          ))}
        </div>
      </div>
    </div>
  );
}

function GlanceCard({ title, big, sub, ring, bar, chips, cta }: { title: string; big: string; sub: string; ring?: number; bar?: number; chips?: string[]; cta: string }) {
  return (
    <VCard dark className="min-w-[230px]">
      <Label dark>{title}</Label>
      <div className="mt-2 flex items-end justify-between">
        <div>
          <div className="font-mono" style={{ fontSize: 28, fontWeight: 500, lineHeight: 1 }}>{big}</div>
          <div style={{ fontSize: 11, color: "var(--text-dark-2)", marginTop: 4 }}>{sub}</div>
        </div>
        {ring !== undefined && <VProgressRing value={ring} size={56} />}
      </div>
      {bar !== undefined && <div className="mt-3"><VProgressBar dark value={bar} /></div>}
      {chips && (
        <div className="mt-3 flex gap-1.5 flex-wrap">
          {chips.map((c) => <span key={c} className="px-2 py-1 rounded-full" style={{ background: "rgba(245,245,243,0.06)", fontSize: 11 }}>{c}</span>)}
        </div>
      )}
      <button className="mt-3 inline-flex items-center gap-1" style={{ fontSize: 12, color: "var(--arctic)" }}>{cta} <ChevronRight size={14} /></button>
    </VCard>
  );
}

// ─── People ──────────────────────────────────────────────────────────────────
function People({ onOpenStudent, onOpenTeacher }: { onOpenStudent: (id: string) => void; onOpenTeacher: (id: string) => void }) {
  const [tab, setTab] = useState<"Students" | "Teachers">("Students");
  return (
    <div className="px-5 pt-6 pb-6">
      <h1 className="mb-4">People</h1>
      <VTopTabs dark tabs={["Students", "Teachers"]} value={tab} onChange={(v) => setTab(v as any)} />
      <div className="mt-4 flex gap-2">
        <div className="flex-1">
          <VInput dark placeholder={`Search ${tab.toLowerCase()}`} icon={<Search size={16} />} />
        </div>
        <button className="w-12 h-12 rounded-[10px] flex items-center justify-center" style={{ background: "rgba(245,245,243,0.06)", border: "1px solid var(--border-dark-2)" }}>
          <Filter size={18} />
        </button>
      </div>
      <div className="mt-3 flex gap-2 flex-wrap">
        {(tab === "Students" ? ["All classes", "Class 10-A", "Class 9-A", "Status"] : ["All subjects", "Active 7d", "Inactive"]).map((c, i) => (
          <VTag dark key={c} active={i === 0}>{c} <ChevronDown size={12} className="inline ml-1 -mt-0.5" /></VTag>
        ))}
      </div>

      <div className="mt-4 space-y-2">
        {tab === "Students" && students.map((s) => (
          <button key={s.id} onClick={() => onOpenStudent(s.id)} className="w-full">
            <VCard dark padded>
              <div className="flex items-center gap-3">
                <VAvatar name={s.name} size={42} />
                <div className="flex-1 text-left">
                  <div style={{ fontWeight: 600 }}>{s.name}</div>
                  <div className="font-mono" style={{ fontSize: 11, color: "var(--text-dark-2)" }}>Roll {s.roll} • {s.class}</div>
                </div>
                <div className="text-right">
                  <div className="flex items-center gap-1.5 justify-end">
                    <VStatusDot tone={s.pews === "ok" ? "success" : s.pews === "warn" ? "warning" : "danger"} />
                    <span className="font-mono" style={{ fontSize: 13 }}>{s.attendance}%</span>
                  </div>
                  <div style={{ fontSize: 10, color: "var(--text-dark-3)" }}>Attendance</div>
                </div>
              </div>
            </VCard>
          </button>
        ))}
        {tab === "Teachers" && teachers.map((t) => (
          <button key={t.id} onClick={() => onOpenTeacher(t.id)} className="w-full">
            <VCard dark padded>
              <div className="flex items-center gap-3">
                <VAvatar name={t.name} size={42} />
                <div className="flex-1 text-left">
                  <div className="flex items-center gap-2">
                    <span style={{ fontWeight: 600 }}>{t.name}</span>
                    <VStatusDot tone={t.active ? "success" : "warning"} />
                  </div>
                  <div style={{ fontSize: 11, color: "var(--text-dark-2)" }}>{t.subjects.join(" • ")} · {t.classes.length} classes</div>
                </div>
                <div className="text-right" style={{ fontSize: 11, color: "var(--text-dark-3)" }}>{t.lastActive}</div>
              </div>
            </VCard>
          </button>
        ))}
      </div>

      <button className="fixed right-6 bottom-24 w-14 h-14 rounded-full flex items-center justify-center" style={{ background: "var(--arctic)", color: "var(--void)" }}>
        <Plus size={22} />
      </button>
    </div>
  );
}

// ─── Student Detail ──────────────────────────────────────────────────────────
function StudentDetail({ id, onBack }: { id: string; onBack: () => void }) {
  const s = students.find((x) => x.id === id) || students[0];
  const [tab, setTab] = useState("Overview");
  return (
    <PhoneFrame dark className="dark">
      <VBackHeader dark title="Student" onBack={onBack} action={<button><FileText size={18} /></button>} />
      <div className="flex-1 overflow-y-auto">
        <div className="px-5 pt-5 pb-4">
          <div className="flex items-center gap-4">
            <VAvatar name={s.name} size={64} />
            <div>
              <h2 style={{ color: "var(--cloud)" }}>{s.name}</h2>
              <div style={{ fontSize: 12, color: "var(--text-dark-2)" }}>Class {s.class.replace(/(\d+)/, "$1 - ")} • Roll {s.roll}</div>
              <div className="mt-1 font-mono" style={{ fontSize: 11, color: "var(--text-dark-3)" }}>Adm SVM-2024-{100 + parseInt(s.roll)}</div>
            </div>
          </div>
          <div className="mt-5 grid grid-cols-3 gap-2">
            <Stat label="Attendance" value={`${s.attendance}%`} />
            <Stat label="Last marks" value={`${s.lastMarks}%`} />
            <Stat label="Dues" value={s.fees ? `₹${s.fees}` : "₹0"} tone={s.fees ? "danger" : "success"} />
          </div>
        </div>
        <div className="px-5">
          <VTopTabs dark tabs={["Overview", "Attendance", "Marks", "Fees", "Notes"]} value={tab} onChange={setTab} />
        </div>
        <div className="px-5 py-4 space-y-3">
          {tab === "Overview" && (
            <>
              <VCard dark>
                <Label dark>Parent</Label>
                <div className="mt-2" style={{ fontWeight: 600 }}>{s.parentName}</div>
                <div className="font-mono" style={{ fontSize: 12, color: "var(--text-dark-2)" }}>{s.parentMobile}</div>
                <div className="mt-2 flex gap-2">
                  <VBadge tone="success">Verified</VBadge>
                  <VBadge tone="arctic">WhatsApp opt-in</VBadge>
                </div>
              </VCard>
              <VCard dark>
                <Label dark>Personal</Label>
                <div className="mt-2 grid grid-cols-2 gap-3" style={{ fontSize: 13 }}>
                  <Field label="DOB" value={s.dob} />
                  <Field label="Gender" value={s.gender === "M" ? "Male" : "Female"} />
                  <Field label="Blood group" value="—" />
                  <Field label="Admission yr" value="2024" />
                </div>
              </VCard>
            </>
          )}
          {tab === "Attendance" && <AttendanceHeat dark />}
          {tab === "Marks" && (
            <VCard dark>
              {["Mathematics", "Science", "English", "Hindi"].map((sub, i) => (
                <div key={sub} className={i ? "pt-3 mt-3" : ""} style={{ borderTop: i ? "1px solid var(--border-dark-1)" : "none" }}>
                  <div className="flex items-center justify-between">
                    <span style={{ fontSize: 14, fontWeight: 600 }}>{sub}</span>
                    <span className="font-mono">{[74, 88, 81, 76][i]}%</span>
                  </div>
                  <VProgressBar dark value={[74, 88, 81, 76][i]} />
                </div>
              ))}
            </VCard>
          )}
          {tab === "Fees" && (
            <>
              <VCard dark>
                <Label dark>Outstanding</Label>
                <div className="font-mono mt-1" style={{ fontSize: 28, fontWeight: 500, color: s.fees ? "var(--danger)" : "var(--success)" }}>
                  ₹ {s.fees.toLocaleString()}
                </div>
                <VButton className="mt-3" full variant="secondary" dark>Send reminder</VButton>
              </VCard>
              {feeHistory.map((f) => <FeeRow key={f.receipt} f={f} dark />)}
            </>
          )}
          {tab === "Notes" && (
            <VCard dark>
              <Label dark>Internal note — Admin only</Label>
              <p className="mt-2" style={{ fontSize: 13, color: "var(--text-dark-2)" }}>
                {s.fees ? "Parent requested fee deferment until 25 Jun. Approved verbally." : "Excellent academic performance. Consider for science olympiad."}
              </p>
              <div className="mt-3" style={{ fontSize: 11, color: "var(--text-dark-3)" }}>Logged by Principal A. Verma • 4 Jun 2026</div>
            </VCard>
          )}
        </div>
      </div>
    </PhoneFrame>
  );
}

function Stat({ label, value, tone }: { label: string; value: string; tone?: "success" | "danger" }) {
  return (
    <div className="rounded-[12px] p-3" style={{ background: "rgba(245,245,243,0.06)" }}>
      <div style={{ fontSize: 10, color: "var(--text-dark-3)", textTransform: "uppercase", letterSpacing: "0.06em" }}>{label}</div>
      <div className="font-mono mt-1" style={{ fontSize: 16, fontWeight: 500, color: tone === "danger" ? "var(--danger)" : tone === "success" ? "var(--success)" : "inherit" }}>{value}</div>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div style={{ fontSize: 10, color: "var(--text-dark-3)", textTransform: "uppercase", letterSpacing: "0.06em" }}>{label}</div>
      <div className="mt-0.5">{value}</div>
    </div>
  );
}

function FeeRow({ f, dark }: { f: { date: string; amount: number; head: string; receipt: string }; dark?: boolean }) {
  return (
    <VCard dark={dark} padded>
      <div className="flex items-center justify-between">
        <div>
          <div style={{ fontSize: 13, fontWeight: 600 }}>{f.head}</div>
          <div className="font-mono" style={{ fontSize: 11, color: dark ? "var(--text-dark-2)" : "var(--text-light-2)" }}>{f.date} • {f.receipt}</div>
        </div>
        <div className="text-right">
          <div className="font-mono" style={{ fontSize: 15 }}>₹ {f.amount.toLocaleString()}</div>
          <button style={{ fontSize: 11, color: "var(--arctic)", textDecoration: "underline" }}>Receipt</button>
        </div>
      </div>
    </VCard>
  );
}

// ─── Teacher Detail ──────────────────────────────────────────────────────────
function TeacherDetail({ id, onBack }: { id: string; onBack: () => void }) {
  const t = teachers.find((x) => x.id === id) || teachers[0];
  const [tab, setTab] = useState("Profile");
  return (
    <PhoneFrame dark className="dark">
      <VBackHeader dark title="Teacher" onBack={onBack} />
      <div className="flex-1 overflow-y-auto px-5 py-4">
        <div className="flex items-center gap-4">
          <VAvatar name={t.name} size={64} />
          <div>
            <h2 style={{ color: "var(--cloud)" }}>{t.name}</h2>
            <div className="font-mono" style={{ fontSize: 12, color: "var(--text-dark-2)" }}>{t.username}</div>
            <div className="mt-1 flex gap-1.5">{t.subjects.map((s) => <VBadge key={s} tone="arctic">{s}</VBadge>)}</div>
          </div>
        </div>
        <div className="mt-5">
          <VTopTabs dark tabs={["Profile", "Activity", "Classes"]} value={tab} onChange={setTab} />
        </div>
        <div className="mt-4 space-y-3">
          {tab === "Profile" && (
            <VCard dark>
              <Field label="Mobile" value="+91 98XXX 12121" />
              <div className="h-3" />
              <Field label="Employee ID" value={`EMP-0${t.id.slice(1)}`} />
              <div className="h-3" />
              <Field label="Joined" value="14 Apr 2022" />
              <div className="h-3" />
              <Field label="Class teacher of" value="10-A" />
              <div className="mt-4 flex gap-2">
                <VButton variant="secondary" dark size="sm">Reset credentials</VButton>
                <VButton variant="destructive" size="sm">Deactivate</VButton>
              </div>
            </VCard>
          )}
          {tab === "Activity" && (
            <>
              <VCard dark>
                <Label dark>Update frequency — last 30 days</Label>
                <div className="mt-3 grid grid-cols-15 gap-1" style={{ gridTemplateColumns: "repeat(15,1fr)" }}>
                  {Array.from({ length: 30 }, (_, i) => {
                    const v = (i * 7) % 4;
                    const bg = v === 0 ? "rgba(245,245,243,0.06)" : v === 1 ? "rgba(200,222,255,0.20)" : v === 2 ? "rgba(200,222,255,0.45)" : "var(--arctic)";
                    return <div key={i} style={{ aspectRatio: "1", background: bg, borderRadius: 3 }} />;
                  })}
                </div>
              </VCard>
              <VCard dark>
                <Label dark>Recent updates</Label>
                <div className="mt-3 space-y-3">
                  {[1, 2, 3, 4, 5].map((i) => (
                    <div key={i} className="flex items-start gap-2">
                      <BookOpen size={14} className="mt-1 opacity-70" />
                      <div style={{ fontSize: 13 }}>
                        Updated <b>Class 10-A Chemistry</b> — Ch. 6 Periodic Table
                        <div style={{ fontSize: 11, color: "var(--text-dark-3)" }}>{i}d ago</div>
                      </div>
                    </div>
                  ))}
                </div>
              </VCard>
            </>
          )}
          {tab === "Classes" && (
            <div className="grid grid-cols-2 gap-2">
              {t.classes.map((c) => (
                <VCard dark key={c} padded>
                  <div style={{ fontWeight: 600 }}>{c}</div>
                  <div style={{ fontSize: 11, color: "var(--text-dark-2)" }}>{32 + (c.charCodeAt(0) % 6)} students</div>
                </VCard>
              ))}
            </div>
          )}
        </div>
      </div>
    </PhoneFrame>
  );
}

// ─── Records ─────────────────────────────────────────────────────────────────
function Records() {
  const [tab, setTab] = useState("Attendance");
  return (
    <div className="px-5 pt-6 pb-6">
      <h1 className="mb-4">Records</h1>
      <VTopTabs dark tabs={["Attendance", "Marks", "Syllabus", "Fee", "Documents"]} value={tab} onChange={setTab} />
      <div className="mt-4 space-y-3">
        {tab === "Attendance" && <RecordsAttendance />}
        {tab === "Marks" && <RecordsMarks />}
        {tab === "Syllabus" && <RecordsSyllabus />}
        {tab === "Fee" && <RecordsFee />}
        {tab === "Documents" && <RecordsDocs />}
      </div>
    </div>
  );
}

function RecordsAttendance() {
  return (
    <>
      <div className="flex gap-2">
        <VInput dark icon={<Calendar size={16} />} defaultValue="5 Jun 2026" />
        <VInput dark icon={<GraduationCap size={16} />} defaultValue="Class 10-A" />
      </div>
      <VCard dark>
        <div className="flex items-center justify-between mb-3">
          <div><Label dark>Class 10-A</Label><div className="mt-1" style={{ fontWeight: 600 }}>28 / 32 present</div></div>
          <VButton variant="secondary" dark size="sm">Mark all present</VButton>
        </div>
        {students.slice(0, 6).map((s) => (
          <div key={s.id} className="py-2.5 flex items-center gap-3" style={{ borderTop: "1px solid var(--border-dark-1)" }}>
            <VAvatar name={s.name} size={32} />
            <div className="flex-1">
              <div style={{ fontSize: 13, fontWeight: 600 }}>{s.name}</div>
              <div className="font-mono" style={{ fontSize: 11, color: "var(--text-dark-3)" }}>Roll {s.roll}</div>
            </div>
            <div className="flex gap-1">
              {["P", "A", "L"].map((m, i) => {
                const active = (s.today === "present" && m === "P") || (s.today === "absent" && m === "A") || (s.today === "late" && m === "L");
                const tone = m === "P" ? "var(--success)" : m === "A" ? "var(--danger)" : "var(--warning)";
                return (
                  <button key={m} className="w-9 h-9 rounded-full flex items-center justify-center" style={{ background: active ? tone : "rgba(245,245,243,0.06)", color: active ? "var(--void)" : "var(--text-dark-3)", fontSize: 11, fontWeight: 700 }}>{m}</button>
                );
              })}
            </div>
          </div>
        ))}
      </VCard>
      <VButton full size="lg" stateful successLabel="Submitted"><Check size={16} /> Submit attendance</VButton>
    </>
  );
}

function RecordsMarks() {
  return (
    <>
      <div className="grid grid-cols-2 gap-2">
        <VInput dark defaultValue="Class 10-A" />
        <VInput dark defaultValue="Mathematics" />
      </div>
      <VCard dark>
        <div className="flex items-center justify-between mb-2">
          <div>
            <Label dark>Unit Test 2 — Trigonometry</Label>
            <div className="mt-1" style={{ fontSize: 13 }}>Max 100 • 02 Jun</div>
          </div>
          <VBadge tone="arctic">Live class avg 68</VBadge>
        </div>
        {students.slice(0, 5).map((s) => (
          <div key={s.id} className="py-2.5 flex items-center gap-3" style={{ borderTop: "1px solid var(--border-dark-1)" }}>
            <VAvatar name={s.name} size={30} />
            <div className="flex-1" style={{ fontSize: 13 }}>{s.name}</div>
            <input className="w-20 px-3 py-1.5 rounded-md font-mono text-right" style={{ background: "rgba(245,245,243,0.06)", border: "1px solid var(--border-dark-2)", color: "var(--cloud)" }} defaultValue={[78, 71, 88, 65, 92][students.indexOf(s) % 5]} />
            <button className="w-9 h-9 rounded-full text-xs font-bold" style={{ background: "rgba(245,245,243,0.06)" }}>AB</button>
          </div>
        ))}
      </VCard>
      <VButton full size="lg" stateful successLabel="Published">Save & publish</VButton>
    </>
  );
}

function RecordsSyllabus() {
  return (
    <>
      <div className="grid grid-cols-2 gap-2">
        <VInput dark defaultValue="Class 10-A" />
        <VInput dark defaultValue="Chemistry" />
      </div>
      {["Ch 1 — Chemical Reactions", "Ch 2 — Acids, Bases & Salts", "Ch 3 — Metals & Non-Metals", "Ch 4 — Carbon Compounds", "Ch 5 — Periodic Classification", "Ch 6 — Life Processes"].map((c, i) => (
        <VCard dark key={c}>
          <div className="flex items-center justify-between">
            <div>
              <div style={{ fontWeight: 600 }}>{c}</div>
              <div style={{ fontSize: 11, color: "var(--text-dark-2)" }}>{i < 4 ? `Covered ${10 + i} Apr — ${20 + i} May` : i === 4 ? "In progress" : "Not started"}</div>
            </div>
            <VBadge tone={i < 4 ? "success" : i === 4 ? "warning" : "neutral"}>{i < 4 ? "Done" : i === 4 ? "Active" : "Upcoming"}</VBadge>
          </div>
        </VCard>
      ))}
    </>
  );
}

function RecordsFee() {
  return (
    <>
      <VTopTabs dark tabs={["Structure", "Collections"]} value="Collections" onChange={() => {}} />
      <VCard dark>
        <Label dark>Outstanding overview</Label>
        <div className="font-mono mt-1" style={{ fontSize: 26 }}>₹ 2,18,400</div>
        <div className="mt-3 grid grid-cols-3 gap-2 text-center">
          {[["84", "Pending"], ["12", "Overdue"], ["220", "Paid"]].map(([n, l], i) => (
            <div key={l} className="rounded-[10px] py-2" style={{ background: "rgba(245,245,243,0.06)" }}>
              <div className="font-mono" style={{ fontSize: 16, color: i === 1 ? "var(--danger)" : "inherit" }}>{n}</div>
              <div style={{ fontSize: 10, color: "var(--text-dark-3)" }}>{l}</div>
            </div>
          ))}
        </div>
      </VCard>
      <VButton full variant="secondary" dark><Send size={14} /> Send reminders to 84 parents</VButton>
      {feeHistory.map((f) => <FeeRow key={f.receipt} f={f} dark />)}
    </>
  );
}

function RecordsDocs() {
  return (
    <>
      {[
        ["Circular — PTM Notice", "PDF • Uploaded 3 Jun", "All parents"],
        ["Half-Yearly Timetable", "PDF • Uploaded 1 Jun", "Class 9, 10, 12"],
        ["Holiday List 2025-26", "PDF • Uploaded 12 Apr", "All school"],
      ].map(([t, d, r]) => (
        <VCard dark key={t}>
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-[10px] flex items-center justify-center" style={{ background: "rgba(200,222,255,0.20)" }}><FileText size={18} /></div>
            <div className="flex-1">
              <div style={{ fontWeight: 600 }}>{t}</div>
              <div style={{ fontSize: 11, color: "var(--text-dark-2)" }}>{d}</div>
              <div className="mt-1"><VBadge tone="neutral">{r}</VBadge></div>
            </div>
            <button><Download size={18} /></button>
          </div>
        </VCard>
      ))}
      <VButton full variant="secondary" dark><Upload size={14} /> Upload document</VButton>
    </>
  );
}

// ─── Comms ───────────────────────────────────────────────────────────────────
function Comms({ onOpenAnnouncement }: { onOpenAnnouncement: (id: string) => void }) {
  const [tab, setTab] = useState("Announcements");
  return (
    <div className="px-5 pt-6 pb-6">
      <h1 className="mb-4">Communications</h1>
      <VTopTabs dark tabs={["Announcements", "Messages", "PTM", "Notifications"]} value={tab} onChange={setTab} />
      <div className="mt-4 space-y-3">
        {tab === "Announcements" && (
          <>
            <VButton full size="lg" tone="teal"><Plus size={16} /> Compose announcement</VButton>
            {announcements.map((a) => (
              <button key={a.id} onClick={() => onOpenAnnouncement(a.id)} className="block w-full text-left">
                <VCard dark>
                  <div style={{ fontWeight: 600 }}>{a.title}</div>
                  <div style={{ fontSize: 12, color: "var(--text-dark-2)", marginTop: 2 }}>{a.recipients} • {a.date}</div>
                  <div className="mt-2 flex items-center gap-2"><VBadge tone="arctic">Opens {a.opens}</VBadge></div>
                </VCard>
              </button>
            ))}
          </>
        )}
        {tab === "Messages" && (
          <>
            {messagesInbox.map((m) => (
              <VCard dark key={m.id}>
                <div className="flex items-start gap-3">
                  <VAvatar name={m.parent} size={36} />
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <span style={{ fontWeight: 600, fontSize: 13 }}>{m.parent}</span>
                      {m.overdue && <VBadge tone="danger">Overdue</VBadge>}
                    </div>
                    <div style={{ fontSize: 11, color: "var(--text-dark-3)" }}>{m.child}</div>
                    <div className="mt-1.5" style={{ fontSize: 13, color: "var(--text-dark-2)" }}>{m.preview}</div>
                    <div className="mt-1" style={{ fontSize: 11, color: "var(--text-dark-3)" }}>{m.time}</div>
                  </div>
                </div>
              </VCard>
            ))}
          </>
        )}
        {tab === "PTM" && (
          <>
            <VCard dark>
              <Label dark>Next PTM</Label>
              <div className="mt-1" style={{ fontWeight: 600 }}>Half-yearly PTM — Class 10</div>
              <div style={{ fontSize: 12, color: "var(--text-dark-2)" }}>Tomorrow, 10:00 AM — 1:00 PM • Physical</div>
              <div className="mt-3 flex items-center gap-3">
                <VProgressRing value={74} size={56} />
                <div>
                  <div style={{ fontWeight: 600 }}>47 of 65 slots booked</div>
                  <div style={{ fontSize: 11, color: "var(--text-dark-2)" }}>Bookings close in 18 hours</div>
                </div>
              </div>
            </VCard>
            <VButton full size="lg" tone="peach"><Plus size={16} /> Schedule new PTM</VButton>
            <VComingSoon dark title="Online PTM (video)" description="Hold the meeting inside the app over secure video. Releasing next quarter." />
          </>
        )}
        {tab === "Notifications" && (
          <>
            {notifications.slice(0, 6).map((n) => (
              <VCard dark key={n.id} padded>
                <div className="flex items-center justify-between">
                  <div style={{ fontSize: 13 }}>{n.title}</div>
                  <VBadge tone="success">Delivered</VBadge>
                </div>
                <div style={{ fontSize: 11, color: "var(--text-dark-3)" }}>{n.body} • {n.time}</div>
              </VCard>
            ))}
          </>
        )}
      </div>
    </div>
  );
}

// ─── Settings ────────────────────────────────────────────────────────────────
function SettingsScreen() {
  return (
    <div className="px-5 pt-6 pb-6 space-y-3">
      <h1 className="mb-2">Settings</h1>
      {[
        { icon: <GraduationCap size={18} />, title: "School profile", sub: "Logo, address, contact info" },
        { icon: <Calendar size={18} />, title: "Academic year", sub: "Currently 2025-26 • 173 days left" },
        { icon: <BookOpen size={18} />, title: "Classes & subjects", sub: "6 classes • 42 subjects" },
        { icon: <Users size={18} />, title: "Teacher management", sub: "8 teachers • download credentials" },
        { icon: <Wallet size={18} />, title: "Fee structure", sub: "Edit heads & amounts for next cycle" },
        { icon: <Bell size={18} />, title: "Notifications", sub: "Channels & quiet hours" },
        { icon: <Download size={18} />, title: "Data export", sub: "CSV / PDF / UDISE (Coming Soon)" },
        { icon: <SettingsIcon size={18} />, title: "Account", sub: "Admin email & password" },
      ].map((row) => (
        <VCard dark key={row.title}>
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-[10px] flex items-center justify-center" style={{ background: "rgba(245,245,243,0.06)" }}>{row.icon}</div>
            <div className="flex-1">
              <div style={{ fontWeight: 600 }}>{row.title}</div>
              <div style={{ fontSize: 11, color: "var(--text-dark-2)" }}>{row.sub}</div>
            </div>
            <ChevronRight size={16} className="opacity-50" />
          </div>
        </VCard>
      ))}
    </div>
  );
}

// ─── Announcement detail (shared, exported) ──────────────────────────────────
export function AnnouncementDetail({ id, onBack, dark }: { id: string; onBack: () => void; dark?: boolean }) {
  const a = announcements.find((x) => x.id === id) || announcements[0];
  return (
    <PhoneFrame dark={dark} className={dark ? "dark" : ""}>
      <VBackHeader dark={dark} title="Announcement" onBack={onBack} />
      <div className="flex-1 overflow-y-auto px-5 py-5">
        <h2 style={{ color: dark ? "var(--cloud)" : "var(--void)" }}>{a.title}</h2>
        <div className="mt-1" style={{ fontSize: 12, color: dark ? "var(--text-dark-2)" : "var(--text-light-2)" }}>{a.date} • Posted by School Administration</div>
        <div className="mt-4 flex gap-2"><VBadge tone="arctic">{a.recipients}</VBadge><VBadge tone="success">App + WhatsApp</VBadge></div>
        <p className="mt-4" style={{ fontSize: 14, lineHeight: 1.6, color: dark ? "var(--text-dark-2)" : "var(--text-light-2)" }}>{a.body}</p>
        {dark && (
          <VCard dark className="mt-4">
            <Label dark>Delivery</Label>
            <div className="mt-2 grid grid-cols-3 gap-2 text-center">
              <Stat label="Sent" value={a.opens.split(" / ")[1]} />
              <Stat label="Opened" value={a.opens.split(" / ")[0]} />
              <Stat label="Replied" value="9" />
            </div>
          </VCard>
        )}
      </div>
    </PhoneFrame>
  );
}

// ─── Reusable attendance heatmap ─────────────────────────────────────────────
export function AttendanceHeat({ dark }: { dark?: boolean }) {
  const days = Array.from({ length: 30 }, (_, i) => {
    const d = i + 1;
    if (d > 24) return { d, t: "future" };
    if ([1, 8, 15, 22].includes(d)) return { d, t: "holiday" };
    if ([5, 12].includes(d)) return { d, t: "absent" };
    if (d === 17) return { d, t: "late" };
    return { d, t: "present" };
  });
  const tone = (t: string) =>
    t === "present" ? "var(--arctic)" :
    t === "absent" ? "var(--danger)" :
    t === "late" ? "var(--warning)" :
    t === "holiday" ? (dark ? "rgba(245,245,243,0.06)" : "rgba(8,8,8,0.04)") : "transparent";
  return (
    <VCard dark={dark}>
      <div className="flex items-center justify-between mb-3">
        <Label dark={dark}>June 2026</Label>
        <span className="font-mono" style={{ fontSize: 13 }}>91%</span>
      </div>
      <div className="grid grid-cols-7 gap-1.5">
        {["S", "M", "T", "W", "T", "F", "S"].map((d, i) => (
          <div key={i} className="text-center" style={{ fontSize: 10, color: dark ? "var(--text-dark-3)" : "var(--text-light-3)" }}>{d}</div>
        ))}
        {days.map((d) => (
          <div key={d.d} className="aspect-square rounded-full flex items-center justify-center font-mono"
            style={{ background: tone(d.t), color: d.t === "present" ? "#0a3a76" : d.t === "absent" ? "#7a1c18" : d.t === "late" ? "#7a3f00" : dark ? "var(--text-dark-3)" : "var(--text-light-3)", fontSize: 11, border: d.t === "future" ? `1px solid ${dark ? "var(--border-dark-1)" : "var(--border-light-1)"}` : "none" }}>
            {d.d}
          </div>
        ))}
      </div>
      <div className="mt-4 flex justify-between text-center" style={{ fontSize: 11 }}>
        <Pill n={21} label="Present" tone="arctic" />
        <Pill n={2} label="Absent" tone="danger" />
        <Pill n={1} label="Late" tone="warning" />
        <Pill n={4} label="Holiday" tone="neutral" />
      </div>
    </VCard>
  );
}

function Pill({ n, label, tone }: { n: number; label: string; tone: "arctic" | "danger" | "warning" | "neutral" }) {
  return (
    <div>
      <div className="font-mono" style={{ fontSize: 16 }}>{n}</div>
      <div style={{ fontSize: 10, opacity: 0.6 }}>{label}</div>
    </div>
  );
}
