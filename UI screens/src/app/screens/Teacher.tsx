import { useState } from "react";
import { Home, ListChecks, Users, User, Bell, Check, Clock, AlertCircle, ChevronRight, Plus, BookOpen, Edit3 } from "lucide-react";
import {
  PhoneFrame, VBottomNav, VCard, VBadge, VAvatar, VButton, VInput, VTopTabs, VProgressBar, Label, VBackHeader, VTag, VDivider,
} from "../components/v/primitives";
import { students, timetableToday, teachers } from "../lib/mock";

const navItems = [
  { id: "home", label: "Home", icon: <Home size={22} /> },
  { id: "update", label: "Update", icon: <ListChecks size={22} />, badge: 1 },
  { id: "classes", label: "My Classes", icon: <Users size={22} /> },
  { id: "profile", label: "Profile", icon: <User size={22} /> },
];

export function TeacherApp({ onExit }: { onExit: () => void }) {
  const [tab, setTab] = useState("home");
  const [classDetail, setClassDetail] = useState<string | null>(null);

  if (classDetail) return <ClassDetail id={classDetail} onBack={() => setClassDetail(null)} />;

  return (
    <PhoneFrame dark className="dark">
      <div className="flex-1 overflow-y-auto pb-4">
        {tab === "home" && <TeacherHome onExit={onExit} />}
        {tab === "update" && <Update />}
        {tab === "classes" && <MyClasses onOpen={setClassDetail} />}
        {tab === "profile" && <Profile onExit={onExit} />}
      </div>
      <VBottomNav dark items={navItems} value={tab} onChange={setTab} />
    </PhoneFrame>
  );
}

function TeacherHome({ onExit }: { onExit: () => void }) {
  const me = teachers[1];
  return (
    <div className="px-5 pt-6 pb-6 space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <Label dark>Good morning</Label>
          <h1 className="mt-1">Priya</h1>
          <p style={{ color: "var(--text-dark-2)", fontSize: 13 }}>Friday, 5 June 2026</p>
        </div>
        <div className="flex items-center gap-2">
          <button className="w-10 h-10 rounded-full flex items-center justify-center" style={{ background: "rgba(245,245,243,0.06)" }}><Bell size={18} /></button>
          <button onClick={onExit}><VAvatar name={me.name} size={40} /></button>
        </div>
      </div>

      <div>
        <h3 className="mb-2">Today's tasks</h3>
        <div className="space-y-2">
          <TaskCard tone="success" title="Class 10-A attendance" sub="Marked at 9:12 AM • 28 / 32 present" cta="View details" icon={<Check size={18} />} />
          <TaskCard tone="warning" title="Syllabus update pending" sub="You haven't logged Period 2 — Mathematics" cta="Update now" icon={<AlertCircle size={18} />} />
          <TaskCard tone="arctic" title="Class 10-A Unit Test 2" sub="Marks not entered yet • 23 students" cta="Enter marks" icon={<ListChecks size={18} />} />
          <TaskCard tone="neutral" title="4 students haven't submitted yesterday's HW" sub="Mathematics – Algebra worksheet" cta="View" icon={<Clock size={18} />} />
        </div>
      </div>

      <div>
        <h3 className="mb-2">Today's periods</h3>
        <div className="-mx-5 px-5 flex gap-2 overflow-x-auto no-scrollbar">
          {timetableToday.map((p, i) => (
            <div key={p.period} className="min-w-[150px] rounded-[12px] p-3"
              style={{ background: i === 2 ? "var(--arctic)" : "rgba(245,245,243,0.06)", color: i === 2 ? "var(--void)" : "var(--cloud)" }}>
              <Label dark={i !== 2}>Period {p.period}</Label>
              <div style={{ fontWeight: 700, marginTop: 4 }}>{p.subject}</div>
              <div className="font-mono" style={{ fontSize: 11, opacity: 0.8 }}>{p.time} • {p.class}</div>
            </div>
          ))}
        </div>
      </div>

      <div>
        <h3 className="mb-2">Recent activity</h3>
        <VCard dark>
          {[
            ["Marked Class 9-A attendance", "9:12 AM"],
            ["Entered Class 10-A Math UT2 marks", "Yesterday 3:40 PM"],
            ["Updated Class 9-A syllabus — Ch 4", "Yesterday 11:20 AM"],
            ["Assigned homework — Class 10-A", "Wed 4:10 PM"],
          ].map(([what, when], i) => (
            <div key={i} className="py-2.5 flex items-center justify-between" style={{ borderTop: i ? "1px solid var(--border-dark-1)" : "none" }}>
              <div style={{ fontSize: 13 }}>{what}</div>
              <div style={{ fontSize: 11, color: "var(--text-dark-3)" }}>{when}</div>
            </div>
          ))}
        </VCard>
      </div>
    </div>
  );
}

function TaskCard({ tone, title, sub, cta, icon }: { tone: "success" | "warning" | "arctic" | "neutral"; title: string; sub: string; cta: string; icon: any }) {
  const map: Record<string, string> = { success: "var(--success)", warning: "var(--warning)", arctic: "var(--arctic)", neutral: "rgba(245,245,243,0.15)" };
  return (
    <VCard dark>
      <div className="flex items-start gap-3">
        <div className="w-9 h-9 rounded-full flex items-center justify-center shrink-0" style={{ background: map[tone], color: "#080808" }}>{icon}</div>
        <div className="flex-1">
          <div style={{ fontWeight: 600 }}>{title}</div>
          <div style={{ fontSize: 12, color: "var(--text-dark-2)" }}>{sub}</div>
        </div>
        <button style={{ fontSize: 12, color: "var(--arctic)", fontWeight: 600 }}>{cta}</button>
      </div>
    </VCard>
  );
}

function Update() {
  const [tab, setTab] = useState("Attendance");
  return (
    <div className="px-5 pt-6 pb-6">
      <h1 className="mb-4">Update</h1>
      <VTopTabs dark tabs={["Attendance", "Marks", "Syllabus", "Homework"]} value={tab} onChange={setTab} />
      <div className="mt-4 space-y-3">
        {tab === "Attendance" && <AttendanceFlow />}
        {tab === "Marks" && <MarksFlow />}
        {tab === "Syllabus" && <SyllabusFlow />}
        {tab === "Homework" && <HomeworkFlow />}
      </div>
    </div>
  );
}

function AttendanceFlow() {
  return (
    <>
      <div className="grid grid-cols-2 gap-2">
        <VInput dark defaultValue="Class 10-A" />
        <VInput dark defaultValue="Today, 5 Jun" />
      </div>
      <VButton full variant="secondary" dark>Mark all present</VButton>
      <VCard dark>
        {students.filter((s) => s.class === "10A").slice(0, 6).map((s, i) => (
          <div key={s.id} className="py-2.5 flex items-center gap-3" style={{ borderTop: i ? "1px solid var(--border-dark-1)" : "none" }}>
            <VAvatar name={s.name} size={32} />
            <div className="flex-1">
              <div style={{ fontSize: 13, fontWeight: 600 }}>{s.name}</div>
              <div className="font-mono" style={{ fontSize: 11, color: "var(--text-dark-3)" }}>Roll {s.roll}</div>
            </div>
            <div className="flex gap-1">
              {["Present", "Absent", "Late"].map((m) => {
                const sel = (s.today === "present" && m === "Present") || (s.today === "absent" && m === "Absent") || (s.today === "late" && m === "Late");
                const tone = m === "Present" ? "var(--success)" : m === "Absent" ? "var(--danger)" : "var(--warning)";
                return (
                  <button key={m} className="px-2.5 py-1.5 rounded-full" style={{ fontSize: 11, fontWeight: 600, background: sel ? tone : "rgba(245,245,243,0.06)", color: sel ? "var(--void)" : "var(--text-dark-3)" }}>
                    {m[0]}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </VCard>
      <div className="sticky bottom-2">
        <VCard dark>
          <div className="flex items-center justify-between">
            <div>
              <div style={{ fontSize: 12, color: "var(--text-dark-2)" }}>28 marked • 4 remaining</div>
              <VProgressBar dark value={87} />
            </div>
            <VButton size="lg" tone="lavender" stateful successLabel="Submitted">Submit</VButton>
          </div>
        </VCard>
      </div>
    </>
  );
}

function MarksFlow() {
  return (
    <>
      <div className="grid grid-cols-2 gap-2">
        <VInput dark defaultValue="Class 10-A" />
        <VInput dark defaultValue="Mathematics" />
      </div>
      <VCard dark>
        <Label dark>Assessment</Label>
        <div className="mt-1 flex items-center justify-between">
          <div>
            <div style={{ fontWeight: 600 }}>Unit Test 2 — Trigonometry</div>
            <div className="font-mono" style={{ fontSize: 11, color: "var(--text-dark-2)" }}>02 Jun • Max 100</div>
          </div>
          <VButton size="sm" variant="secondary" dark>Edit</VButton>
        </div>
      </VCard>
      <VCard dark>
        {students.filter((s) => s.class === "10A").slice(0, 6).map((s, i) => (
          <div key={s.id} className="py-2.5 flex items-center gap-3" style={{ borderTop: i ? "1px solid var(--border-dark-1)" : "none" }}>
            <VAvatar name={s.name} size={30} />
            <div className="flex-1" style={{ fontSize: 13 }}>{s.name}</div>
            <input className="w-20 px-3 py-1.5 rounded-md font-mono text-right" style={{ background: "rgba(245,245,243,0.06)", border: "1px solid var(--border-dark-2)", color: "var(--cloud)" }} defaultValue={[78, 71, 88, 65, 92, 74][i]} />
          </div>
        ))}
        <div className="pt-3 mt-3 flex items-center justify-between" style={{ borderTop: "1px solid var(--border-dark-1)" }}>
          <span style={{ fontSize: 12, color: "var(--text-dark-2)" }}>Class avg (live)</span>
          <span className="font-mono">68</span>
        </div>
      </VCard>
      <VButton full size="lg" tone="lavender" stateful successLabel="Saved">Save marks</VButton>
    </>
  );
}

function SyllabusFlow() {
  return (
    <>
      <div className="grid grid-cols-2 gap-2">
        <VInput dark defaultValue="Class 10-A" />
        <VInput dark defaultValue="Mathematics" />
      </div>
      <VCard dark>
        <Label dark>Log today's progress</Label>
        <div className="mt-3 space-y-3">
          <VInput dark label="Chapter" defaultValue="Ch 8 — Trigonometric Identities" />
          <VInput dark label="Topics covered" placeholder="Pythagorean identities, sum-of-angles…" />
          <VInput dark label="Homework given" placeholder="Exercise 8.3, Q 4–14" />
          <VInput dark label="Teaching note (admin only)" placeholder="Class struggled with topic X" />
        </div>
      </VCard>
      <VCard dark>
        <Label dark>Parent notification preview</Label>
        <div className="mt-2 rounded-[10px] p-3" style={{ background: "rgba(245,245,243,0.06)", fontSize: 13 }}>
          Class 10-A covered <b>Trigonometric Identities</b> in Mathematics today. Homework: Exercise 8.3, Q 4–14.
        </div>
      </VCard>
      <VButton full size="lg" tone="lavender" stateful successLabel="Logged">Log & notify parents</VButton>
    </>
  );
}

function HomeworkFlow() {
  return (
    <>
      <VCard dark>
        <div className="flex items-center justify-between">
          <div>
            <div style={{ fontWeight: 600 }}>Algebra worksheet</div>
            <div style={{ fontSize: 11, color: "var(--text-dark-2)" }}>Mathematics • Class 10-A • Due 6 Jun</div>
          </div>
          <VBadge tone="warning">28 / 32 submitted</VBadge>
        </div>
        <VProgressBar dark value={88} />
      </VCard>
      <VCard dark>
        <div className="flex items-center justify-between">
          <div>
            <div style={{ fontWeight: 600 }}>Reading — Ch 4 The Postmaster</div>
            <div style={{ fontSize: 11, color: "var(--text-dark-2)" }}>English • Class 9-A • Due 7 Jun</div>
          </div>
          <VBadge tone="arctic">Just assigned</VBadge>
        </div>
      </VCard>
      <VButton full size="lg" tone="lavender"><Plus size={16} /> Assign new homework</VButton>
    </>
  );
}

function MyClasses({ onOpen }: { onOpen: (id: string) => void }) {
  const me = teachers[1];
  return (
    <div className="px-5 pt-6 pb-6">
      <h1 className="mb-4">My Classes</h1>
      <div className="grid grid-cols-2 gap-2">
        {me.classes.map((c) => (
          <button key={c} onClick={() => onOpen(c)}>
            <VCard dark className="text-left">
              <div style={{ fontWeight: 700, fontSize: 18 }}>{c.slice(0, -1)}-{c.slice(-1)}</div>
              <div style={{ fontSize: 11, color: "var(--text-dark-2)" }}>Mathematics</div>
              <div className="mt-3 flex items-center justify-between">
                <div>
                  <div className="font-mono" style={{ fontSize: 18 }}>{32}</div>
                  <div style={{ fontSize: 10, color: "var(--text-dark-3)" }}>Students</div>
                </div>
                <div className="text-right">
                  <div className="font-mono" style={{ fontSize: 18, color: "var(--arctic)" }}>92%</div>
                  <div style={{ fontSize: 10, color: "var(--text-dark-3)" }}>Today</div>
                </div>
              </div>
            </VCard>
          </button>
        ))}
      </div>
    </div>
  );
}

function ClassDetail({ id, onBack }: { id: string; onBack: () => void }) {
  return (
    <PhoneFrame dark className="dark">
      <VBackHeader dark title={`Class ${id.slice(0, -1)} - ${id.slice(-1)}`} onBack={onBack} action={<button><Edit3 size={16} /></button>} />
      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-3">
        <div className="grid grid-cols-3 gap-2">
          <div className="rounded-[10px] p-3 text-center" style={{ background: "rgba(245,245,243,0.06)" }}>
            <div className="font-mono" style={{ fontSize: 18 }}>32</div>
            <div style={{ fontSize: 10, color: "var(--text-dark-3)" }}>Students</div>
          </div>
          <div className="rounded-[10px] p-3 text-center" style={{ background: "rgba(245,245,243,0.06)" }}>
            <div className="font-mono" style={{ fontSize: 18 }}>92%</div>
            <div style={{ fontSize: 10, color: "var(--text-dark-3)" }}>Today</div>
          </div>
          <div className="rounded-[10px] p-3 text-center" style={{ background: "rgba(245,245,243,0.06)" }}>
            <div className="font-mono" style={{ fontSize: 18 }}>74</div>
            <div style={{ fontSize: 10, color: "var(--text-dark-3)" }}>UT2 avg</div>
          </div>
        </div>
        <VButton full variant="secondary" dark>Message class parents</VButton>
        <VCard dark>
          {students.filter((s) => s.class === id).map((s, i) => (
            <div key={s.id} className="py-2.5 flex items-center gap-3" style={{ borderTop: i ? "1px solid var(--border-dark-1)" : "none" }}>
              <VAvatar name={s.name} size={32} />
              <div className="flex-1">
                <div style={{ fontSize: 13, fontWeight: 600 }}>{s.name}</div>
                <div className="font-mono" style={{ fontSize: 11, color: "var(--text-dark-3)" }}>Roll {s.roll}</div>
              </div>
              <div className="font-mono" style={{ fontSize: 13 }}>{s.attendance}%</div>
            </div>
          ))}
        </VCard>
      </div>
    </PhoneFrame>
  );
}

function Profile({ onExit }: { onExit: () => void }) {
  const me = teachers[1];
  return (
    <div className="px-5 pt-6 pb-6">
      <div className="text-center">
        <VAvatar name={me.name} size={88} />
        <h2 className="mt-3" style={{ color: "var(--cloud)" }}>{me.name}</h2>
        <div className="font-mono" style={{ fontSize: 12, color: "var(--text-dark-2)" }}>{me.username}</div>
        <div className="mt-2 flex justify-center gap-1.5">
          {me.subjects.map((s) => <VBadge key={s} tone="arctic">{s}</VBadge>)}
        </div>
      </div>
      <div className="mt-6 space-y-2">
        {[
          ["Personal details", "Mobile, email, photo"],
          ["Notification preferences", "Push, WhatsApp, quiet hours"],
          ["Change password", "Last changed 26 days ago"],
          ["Help & support", "Contact VidyaSetu"],
        ].map(([t, s]) => (
          <VCard dark key={t}>
            <div className="flex items-center justify-between">
              <div>
                <div style={{ fontWeight: 600 }}>{t}</div>
                <div style={{ fontSize: 11, color: "var(--text-dark-2)" }}>{s}</div>
              </div>
              <ChevronRight size={16} className="opacity-50" />
            </div>
          </VCard>
        ))}
        <VButton full variant="ghost" dark onClick={onExit}>Log out</VButton>
      </div>
    </div>
  );
}
