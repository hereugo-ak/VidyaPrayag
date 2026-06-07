import { useState } from "react";
import { Home, BookOpen, Wallet, Bell, ChevronDown, Check, X, Clock, GraduationCap, ChevronRight, MessageSquare, Send, Calendar as CalIcon } from "lucide-react";
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from "recharts";
import {
  PhoneFrame, VBottomNav, VCard, VBadge, VAvatar, VButton, VTopTabs, VProgressBar, Label, VComingSoon,
} from "../components/v/primitives";
import { AIReportCardPreview } from "../components/v/mockups";
import { VDonut, VSparkline, VLegendDot } from "../components/v/charts";
import { childForParent, siblings, timetableToday, marksHistory, subjectTrend, feeBreakdown, feeHistory, notifications, school } from "../lib/mock";
import { AttendanceHeat } from "./Admin";

const navItems = [
  { id: "home", label: "Home", icon: <Home size={22} /> },
  { id: "academics", label: "Academics", icon: <BookOpen size={22} /> },
  { id: "fees", label: "Fees", icon: <Wallet size={22} /> },
  { id: "activity", label: "Activity", icon: <Bell size={22} />, badge: 2 },
];

export function ParentApp({ onExit }: { onExit: () => void }) {
  const [tab, setTab] = useState("home");
  const [active, setActive] = useState(0);
  const child = siblings[active];

  return (
    <PhoneFrame>
      <ChildSwitcher active={active} onChange={setActive} child={child} onExit={onExit} />
      <div className="flex-1 overflow-y-auto pb-4">
        {tab === "home" && <ParentHome child={child} />}
        {tab === "academics" && <Academics />}
        {tab === "fees" && <Fees />}
        {tab === "activity" && <Activity />}
      </div>
      <VBottomNav items={navItems} value={tab} onChange={setTab} />
    </PhoneFrame>
  );
}

function ChildSwitcher({ active, onChange, child, onExit }: { active: number; onChange: (i: number) => void; child: typeof siblings[number]; onExit: () => void }) {
  const [open, setOpen] = useState(false);
  return (
    <div className="px-5 pt-5 pb-3" style={{ background: "var(--cloud-pure)", borderBottom: "1px solid var(--border-light-1)" }}>
      <div className="flex items-center justify-between">
        <button onClick={() => setOpen(!open)} className="flex items-center gap-3 px-2 py-1.5 rounded-full" style={{ background: "var(--cloud)" }}>
          <VAvatar name={child.name} size={32} />
          <div className="text-left">
            <div style={{ fontSize: 13, fontWeight: 700 }}>{child.name}</div>
            <div style={{ fontSize: 10, color: "var(--text-light-2)" }}>Class {child.class.replace(/(\d+)/, "$1-")} • {school.shortName}</div>
          </div>
          <ChevronDown size={14} className="opacity-50" />
        </button>
        <div className="flex items-center gap-2">
          <button className="relative w-9 h-9 rounded-full flex items-center justify-center" style={{ background: "var(--cloud)" }}>
            <Bell size={16} />
            <span className="absolute top-2 right-2 rounded-full" style={{ width: 6, height: 6, background: "var(--danger)" }} />
          </button>
          <button onClick={onExit}><VAvatar name="Sneha Sharma" size={32} /></button>
        </div>
      </div>
      {open && (
        <div className="mt-3 space-y-1.5">
          {siblings.map((s, i) => (
            <button key={s.id} onClick={() => { onChange(i); setOpen(false); }} className="w-full p-2 rounded-[10px] flex items-center gap-3" style={{ background: i === active ? "rgba(200,222,255,0.30)" : "var(--cloud)" }}>
              <VAvatar name={s.name} size={32} />
              <div className="text-left flex-1">
                <div style={{ fontSize: 13, fontWeight: 600 }}>{s.name}</div>
                <div style={{ fontSize: 10, color: "var(--text-light-2)" }}>Class {s.class.replace(/(\d+)/, "$1-")}</div>
              </div>
              {i === active && <Check size={16} style={{ color: "#0a3a76" }} />}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function ParentHome({ child }: { child: typeof childForParent }) {
  const status =
    child.today === "present" ? { tone: "success" as const, label: "Present today", icon: <Check size={14} /> } :
    child.today === "absent" ? { tone: "danger" as const, label: "Absent today", icon: <X size={14} /> } :
    { tone: "warning" as const, label: "Late today", icon: <Clock size={14} /> };
  return (
    <div className="px-5 pt-5 pb-6 space-y-4">
      <VCard padded={false} className="overflow-hidden">
        <div className="relative px-5 pt-5 pb-5" style={{ background: "linear-gradient(135deg, #f6f1ff 0%, #e8f7f3 100%)" }}>
          <div className="absolute -right-6 -top-6 w-32 h-32 rounded-full opacity-50" style={{ background: "radial-gradient(circle, rgba(60,185,169,0.35), transparent 65%)" }} />
          <div className="flex items-center gap-4 relative">
            <VAvatar name={child.name} size={68} ring />
            <div className="flex-1">
              <h2>{child.name}</h2>
              <div style={{ fontSize: 12, color: "var(--ink-2)" }}>Class {child.class.replace(/(\d+)/, "$1 - ")} • {school.name}</div>
              <div className="mt-2"><VBadge tone={status.tone}><span className="inline-flex items-center gap-1">{status.icon} {status.label}</span></VBadge></div>
            </div>
          </div>
        </div>
        <div className="px-5 py-4 flex items-center gap-4" style={{ borderTop: "1px solid rgba(38,35,77,0.06)" }}>
          <div className="flex-1">
            <Label>Attendance · last 30 days</Label>
            <div className="flex items-baseline gap-1.5 mt-1">
              <span className="font-mono" style={{ fontSize: 24, fontWeight: 700, color: "var(--navy)" }}>92%</span>
              <span style={{ fontSize: 11, color: "#155e3a", fontWeight: 600 }}>+4 vs class avg</span>
            </div>
          </div>
          <VSparkline values={[78, 82, 80, 85, 88, 90, 87, 92, 94, 92]} width={120} height={44} />
        </div>
      </VCard>

      <VCard>
        <Label>Today's schedule</Label>
        <div className="mt-3 space-y-2.5">
          {timetableToday.slice(0, 5).map((p, i) => (
            <div key={p.period} className="flex items-center gap-3 py-1">
              <div className="w-8 text-center font-mono" style={{ fontSize: 11, color: "var(--text-light-3)" }}>{p.period}</div>
              <div className="flex-1">
                <div style={{ fontSize: 13, fontWeight: 600 }}>{p.subject}</div>
                <div style={{ fontSize: 11, color: "var(--text-light-2)" }}>{p.teacher}</div>
              </div>
              <div className="font-mono" style={{ fontSize: 11, color: "var(--text-light-2)" }}>{p.time.split(" – ")[0]}</div>
              {i === 2 && <VBadge tone="arctic">Now</VBadge>}
            </div>
          ))}
        </div>
      </VCard>

      <VCard>
        <Label>What was covered today</Label>
        <div className="mt-2">
          <div style={{ fontWeight: 600 }}>Science — Ch 6 Periodic Table</div>
          <p style={{ fontSize: 13, color: "var(--text-light-2)", marginTop: 4 }}>
            Trends across periods & groups. Mendeleev vs. modern arrangement. Homework: Ex 6.2 Q 1–6.
          </p>
          <div style={{ fontSize: 11, color: "var(--text-light-3)", marginTop: 8 }}>By Dr. Ramesh Sharma • 2h ago</div>
        </div>
      </VCard>

      <VCard padded>
        <div className="flex items-start gap-3">
          <div className="w-10 h-10 rounded-[10px] flex items-center justify-center" style={{ background: "rgba(255,212,163,0.45)" }}><CalIcon size={18} color="#7a3f00" /></div>
          <div className="flex-1">
            <Label>Remind {child.name.split(" ")[0]} about this</Label>
            <div style={{ fontWeight: 600, marginTop: 2 }}>Mathematics Unit Test — Day after tomorrow</div>
            <div style={{ fontSize: 12, color: "var(--text-light-2)" }}>Trigonometric Identities • Ch 8</div>
          </div>
        </div>
      </VCard>

      <VCard>
        <div className="flex items-center justify-between">
          <div>
            <Label>Fees</Label>
            <div className="mt-1 font-mono" style={{ fontSize: 20, color: "#7a3f00" }}>₹ 12,500 due</div>
            <div style={{ fontSize: 11, color: "var(--text-light-2)" }}>by 11 Jun 2026</div>
          </div>
          <VButton tone="peach" stateful successLabel="Soon">Pay now</VButton>
        </div>
      </VCard>

      <VCard>
        <Label>This month's attendance</Label>
        <div className="mt-2 flex items-end justify-between">
          <div className="font-mono" style={{ fontSize: 38, fontWeight: 500, lineHeight: 1 }}>91%</div>
          <div style={{ fontSize: 12, color: "var(--text-light-2)" }}>21 of 23 days</div>
        </div>
        <div className="mt-3 flex h-2 rounded-full overflow-hidden">
          <div style={{ flex: 21, background: "var(--arctic)" }} />
          <div style={{ flex: 2, background: "var(--danger)" }} />
        </div>
      </VCard>
    </div>
  );
}

// ─── Academics ───────────────────────────────────────────────────────────────
function Academics() {
  const [tab, setTab] = useState("Overview");
  return (
    <div className="px-5 pt-5 pb-6">
      <h1 className="mb-3">Academics</h1>
      <VTopTabs tabs={["Overview", "Attendance", "Marks", "Syllabus", "Report"]} value={tab} onChange={setTab} />
      <div className="mt-4 space-y-3">
        {tab === "Overview" && <>
          {["Mathematics", "Science", "English", "Hindi", "Social Studies", "Computer"].map((s, i) => (
            <VCard key={s} padded>
              <div className="flex items-center justify-between">
                <div style={{ fontWeight: 600 }}>{s}</div>
                <div className="flex items-center gap-2">
                  <span className="font-mono">{[74, 88, 81, 76, 79, 85][i]}%</span>
                  <VBadge tone={[1, 2, 4, 5].includes(i) ? "success" : "warning"}>{[1, 2, 4, 5].includes(i) ? "▲" : "▼"}</VBadge>
                </div>
              </div>
              <VProgressBar value={[74, 88, 81, 76, 79, 85][i]} />
            </VCard>
          ))}
          <VCard>
            <Label>Class standing</Label>
            <div className="mt-2 font-mono" style={{ fontSize: 22 }}>Rank 6 of 32</div>
            <div style={{ fontSize: 12, color: "var(--text-light-2)" }}>Last assessment cycle</div>
          </VCard>
        </>}

        {tab === "Attendance" && <>
          <div className="flex items-center justify-between">
            <button className="px-3 py-1.5 rounded-full" style={{ background: "var(--cloud)", fontSize: 13 }}>‹ May</button>
            <span style={{ fontWeight: 700 }}>June 2026</span>
            <button className="px-3 py-1.5 rounded-full" style={{ background: "var(--cloud)", fontSize: 13 }}>Jul ›</button>
          </div>
          <AttendanceHeat />
          <VCard>
            <Label>3 consecutive absences alert</Label>
            <p style={{ fontSize: 13, color: "var(--text-light-2)", marginTop: 4 }}>
              We've noticed Riya was absent on 5, 6 and 12 Jun. Is everything okay?
            </p>
            <div className="mt-3 flex gap-2">
              <VButton variant="secondary" size="sm">Mark as medical leave</VButton>
              <VButton size="sm" tone="teal" stateful successLabel="Sent">Talk to school</VButton>
            </div>
          </VCard>
        </>}

        {tab === "Marks" && <>
          <div className="flex gap-2 overflow-x-auto no-scrollbar">
            {["Mathematics", "Science", "English", "Hindi", "Social Studies"].map((s, i) => (
              <span key={s} className="px-3 py-1.5 rounded-full whitespace-nowrap" style={{ background: i === 0 ? "var(--arctic)" : "var(--cloud)", fontSize: 12, fontWeight: 600 }}>{s}</span>
            ))}
          </div>
          <VCard>
            <Label>Performance trend</Label>
            <div className="mt-3" style={{ height: 180 }}>
              <ResponsiveContainer>
                <LineChart data={subjectTrend} margin={{ top: 5, right: 10, bottom: 0, left: -25 }}>
                  <XAxis dataKey="x" tick={{ fontSize: 11 }} stroke="rgba(8,8,8,0.3)" />
                  <YAxis tick={{ fontSize: 11 }} stroke="rgba(8,8,8,0.3)" />
                  <Tooltip />
                  <Line type="monotone" dataKey="you" stroke="#A0C4F7" strokeWidth={3} dot={{ r: 4, fill: "#A0C4F7" }} />
                  <Line type="monotone" dataKey="avg" stroke="rgba(8,8,8,0.25)" strokeWidth={2} strokeDasharray="4 4" dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>
            <div className="flex gap-4" style={{ fontSize: 11 }}>
              <span className="inline-flex items-center gap-1.5"><span style={{ width: 10, height: 3, background: "#A0C4F7", borderRadius: 2 }} /> Riya</span>
              <span className="inline-flex items-center gap-1.5"><span style={{ width: 10, height: 3, background: "rgba(8,8,8,0.25)", borderRadius: 2 }} /> Class avg</span>
            </div>
          </VCard>
          {marksHistory.filter((m) => m.subject === "Mathematics").map((m, i) => (
            <VCard key={i} padded>
              <div className="flex items-center justify-between">
                <div>
                  <div style={{ fontWeight: 600 }}>{m.name}</div>
                  <div style={{ fontSize: 11, color: "var(--text-light-2)" }}>{m.date} • {m.subject}</div>
                </div>
                <div className="text-right">
                  <div className="font-mono" style={{ fontSize: 17 }}>{m.marks} / {m.total}</div>
                  <div style={{ fontSize: 11, color: "var(--text-light-3)" }}>Class avg {m.avg}</div>
                </div>
              </div>
            </VCard>
          ))}
        </>}

        {tab === "Syllabus" && <>
          {[
            ["Ch 6 — Periodic Table", "Trends across periods. Modern arrangement.", "5 Jun"],
            ["Ch 5 — Carbon Compounds", "Functional groups. Soaps & detergents.", "29 May"],
            ["Ch 4 — Metals & Non-metals", "Reactivity series. Extraction.", "20 May"],
          ].map(([t, b, d]) => (
            <VCard key={t}>
              <div style={{ fontWeight: 600 }}>{t}</div>
              <div style={{ fontSize: 13, color: "var(--text-light-2)", marginTop: 2 }}>{b}</div>
              <div style={{ fontSize: 11, color: "var(--text-light-3)", marginTop: 6 }}>Covered {d}</div>
            </VCard>
          ))}
          <VCard>
            <Label>Coming up next</Label>
            <div style={{ fontWeight: 600, marginTop: 4 }}>Ch 7 — Life Processes</div>
          </VCard>
        </>}

        {tab === "Report" && <VComingSoon title="AI Report Card" description="At the end of each term, VidyaSetu will generate a personalised academic summary for Riya — narrative strengths, focus areas and study tips." preview={<AIReportCardPreview />} />}
      </div>
    </div>
  );
}

// ─── Fees ────────────────────────────────────────────────────────────────────
function Fees() {
  const total = feeBreakdown.reduce((a, b) => a + b.amount, 0);
  const paid = feeHistory.reduce((a, b) => a + b.amount, 0);
  const palette = ["var(--teal-deep)", "var(--navy)", "#e08a3c", "#a87cf0", "#c14a44"];
  const donutData = feeBreakdown.map((f, i) => ({ label: f.head, value: f.amount, color: palette[i % palette.length] }));
  return (
    <div className="px-5 pt-5 pb-6 space-y-3">
      <h1 className="mb-1">Fees</h1>

      {/* Hero: balance + donut split */}
      <VCard padded={false} className="overflow-hidden">
        <div className="relative px-5 pt-5 pb-4" style={{ background: "linear-gradient(135deg, var(--navy), #3b3870)" }}>
          <div className="absolute -right-10 -top-10 w-40 h-40 rounded-full opacity-20" style={{ background: "var(--teal)" }} />
          <Label><span style={{ color: "rgba(255,255,255,0.7)" }}>Balance due</span></Label>
          <div className="font-mono mt-1" style={{ fontSize: 36, fontWeight: 600, color: "#ffffff", letterSpacing: "-0.01em" }}>₹ {total.toLocaleString()}</div>
          <div className="flex items-center gap-2 mt-1" style={{ fontSize: 12, color: "rgba(255,255,255,0.78)" }}>
            <span className="inline-block rounded-full" style={{ width: 6, height: 6, background: "#FFD4A3" }} />
            Due 11 Jun 2026 · 6 days remaining
          </div>
          <button className="mt-4 inline-flex items-center gap-2 px-4 py-2.5 rounded-full" style={{ background: "var(--teal)", color: "#ffffff", fontSize: 13, fontWeight: 700, boxShadow: "0 6px 16px -4px rgba(60,185,169,0.5)" }}>
            Pay now <span style={{ fontSize: 10, opacity: 0.8, marginLeft: 2 }}>· Coming Soon</span>
          </button>
        </div>
        <div className="px-5 py-5 flex items-center gap-5">
          <VDonut size={132} thickness={14} data={donutData}
            center={<>
              <span style={{ fontSize: 10, color: "var(--ink-3)", fontWeight: 700, letterSpacing: "0.06em" }}>TOTAL</span>
              <span className="font-mono" style={{ fontSize: 18, fontWeight: 700, color: "var(--navy)" }}>₹{(total / 1000).toFixed(0)}k</span>
              <span style={{ fontSize: 10, color: "var(--ink-3)" }}>{Math.round((paid / (paid + total)) * 100)}% paid YTD</span>
            </>}
          />
          <div className="flex-1 space-y-2">
            {donutData.map((d) => (
              <div key={d.label} className="flex items-center justify-between">
                <VLegendDot color={d.color} label={d.label} />
                <span className="font-mono" style={{ fontSize: 11, color: "var(--ink)", fontWeight: 600 }}>₹{(d.value / 1000).toFixed(1)}k</span>
              </div>
            ))}
          </div>
        </div>
      </VCard>

      <Label>Fee breakdown</Label>
      {feeBreakdown.map((f) => (
        <VCard key={f.head} padded>
          <div className="flex items-center justify-between">
            <div>
              <div style={{ fontWeight: 600 }}>{f.head}</div>
              <div style={{ fontSize: 11, color: "var(--text-light-2)" }}>{f.status} • Due {f.dueDate}</div>
            </div>
            <div className="font-mono" style={{ fontSize: 15 }}>₹ {f.amount.toLocaleString()}</div>
          </div>
        </VCard>
      ))}

      <Label>Payment history</Label>
      {feeHistory.map((f) => (
        <VCard key={f.receipt} padded>
          <div className="flex items-center justify-between">
            <div>
              <div style={{ fontSize: 13, fontWeight: 600 }}>{f.head}</div>
              <div className="font-mono" style={{ fontSize: 11, color: "var(--text-light-2)" }}>{f.date} • {f.receipt}</div>
            </div>
            <div className="text-right">
              <div className="font-mono" style={{ fontSize: 15 }}>₹ {f.amount.toLocaleString()}</div>
              <button style={{ fontSize: 11, color: "#0a3a76", textDecoration: "underline" }}>Receipt</button>
            </div>
          </div>
        </VCard>
      ))}

      <button className="text-center w-full pt-3" style={{ fontSize: 12, color: "#0a3a76", fontWeight: 600 }}>
        Have a question about your fees? Message the school →
      </button>
    </div>
  );
}

// ─── Activity ────────────────────────────────────────────────────────────────
function Activity() {
  const [filter, setFilter] = useState("All");
  const [replyOpen, setReplyOpen] = useState(false);
  const tones: Record<string, string> = { attendance: "rgba(200,222,255,0.30)", academic: "rgba(255,212,163,0.45)", fees: "rgba(255,173,168,0.45)", announcement: "rgba(8,8,8,0.06)" };
  const filtered = filter === "All" ? notifications : notifications.filter((n) => n.category === filter.toLowerCase());
  return (
    <div className="px-5 pt-5 pb-6">
      <h1 className="mb-3">Activity</h1>
      <div className="flex gap-2 overflow-x-auto no-scrollbar mb-3">
        {["All", "Attendance", "Academic", "Fees", "Announcement", "Messages"].map((f) => (
          <button key={f} onClick={() => setFilter(f)} className="px-3 py-1.5 rounded-full whitespace-nowrap" style={{ background: filter === f ? "var(--arctic)" : "var(--cloud)", fontSize: 12, fontWeight: 600 }}>{f}</button>
        ))}
      </div>
      <div className="space-y-2">
        {filtered.map((n) => (
          <VCard key={n.id} padded style={{ borderLeft: n.unread ? "3px solid var(--arctic-deep)" : undefined }}>
            <div className="flex items-start gap-3">
              <div className="w-10 h-10 rounded-full flex items-center justify-center" style={{ background: tones[n.category] }}>
                {n.category === "attendance" ? <Check size={16} /> : n.category === "academic" ? <BookOpen size={16} /> : n.category === "fees" ? <Wallet size={16} /> : <Bell size={16} />}
              </div>
              <div className="flex-1">
                <div style={{ fontSize: 14, fontWeight: 600 }}>{n.title}</div>
                <div style={{ fontSize: 12, color: "var(--text-light-2)" }}>{n.body}</div>
                <div style={{ fontSize: 11, color: "var(--text-light-3)", marginTop: 4 }}>{n.time}</div>
              </div>
            </div>
          </VCard>
        ))}

        <VCard padded>
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-full flex items-center justify-center" style={{ background: "var(--cloud)" }}><MessageSquare size={16} /></div>
            <div className="flex-1">
              <div className="flex items-center justify-between">
                <div style={{ fontSize: 14, fontWeight: 600 }}>From School</div>
                <VBadge tone="neutral">Conversation</VBadge>
              </div>
              <div style={{ fontSize: 13, color: "var(--text-light-2)" }}>Please carry the report stub to tomorrow's PTM.</div>
              <button onClick={() => setReplyOpen(!replyOpen)} className="mt-2" style={{ fontSize: 12, color: "#0a3a76", fontWeight: 600 }}>
                Reply to school
              </button>
              {replyOpen && (
                <div className="mt-3 flex gap-2">
                  <input className="flex-1 px-3 py-2 rounded-full" style={{ background: "var(--cloud)", fontSize: 13 }} placeholder="Write a reply…" />
                  <button className="w-9 h-9 rounded-full flex items-center justify-center" style={{ background: "var(--arctic)" }}><Send size={16} /></button>
                </div>
              )}
            </div>
          </div>
        </VCard>
      </div>
    </div>
  );
}
