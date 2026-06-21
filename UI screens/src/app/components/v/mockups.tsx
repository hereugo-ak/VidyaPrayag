import { TrendingUp, AlertTriangle, Sparkles, BookOpen, Heart, Target } from "lucide-react";

// SRI breakdown preview — 11 weighted signals shown as a tidy bar grid.
export function SRIPreview({ score = 7.9 }: { score?: number }) {
  const signals = [
    { label: "Academic outcomes", w: 92 },
    { label: "Teacher retention", w: 84 },
    { label: "Parent sentiment", w: 78 },
    { label: "Safety & infra", w: 88 },
    { label: "Co-curricular", w: 71 },
    { label: "Attendance norms", w: 86 },
  ];
  return (
    <div className="text-left">
      <div className="flex items-end justify-between mb-3">
        <div>
          <div className="font-mono" style={{ fontSize: 32, fontWeight: 600, color: "var(--navy)", lineHeight: 1 }}>{score.toFixed(1)}<span style={{ fontSize: 16, color: "var(--ink-3)" }}>/10</span></div>
          <div style={{ fontSize: 11, color: "var(--ink-3)", marginTop: 2 }}>Above Lucknow median (7.4)</div>
        </div>
        <div className="inline-flex items-center gap-1 px-2 py-1 rounded-full" style={{ background: "rgba(168,230,207,0.4)", color: "#155e3a", fontSize: 11, fontWeight: 600 }}>
          <TrendingUp size={11} /> +0.3 YoY
        </div>
      </div>
      <div className="space-y-2">
        {signals.map((s) => (
          <div key={s.label} className="flex items-center gap-2">
            <span style={{ fontSize: 11, color: "var(--ink-2)", width: 116, textAlign: "left" }}>{s.label}</span>
            <div className="flex-1 h-1.5 rounded-full overflow-hidden" style={{ background: "var(--cream)" }}>
              <div className="h-full rounded-full" style={{ width: `${s.w}%`, background: "var(--teal-deep)" }} />
            </div>
            <span className="font-mono" style={{ fontSize: 11, color: "var(--ink-3)", width: 28, textAlign: "right" }}>{s.w}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

// PEWS — Predictive Early-Warning Score. Risk band + 3 at-risk students.
export function PEWSPreview() {
  const atRisk = [
    { name: "Aman Verma", cls: "9-B", score: 78, drivers: "Attendance ↓ 22%, Maths −18 pts" },
    { name: "Ishita Roy", cls: "10-A", score: 64, drivers: "Late submissions, mood dip flagged" },
    { name: "Karan Singh", cls: "9-A", score: 52, drivers: "Fee overdue, syllabus gap" },
  ];
  return (
    <div className="text-left space-y-3">
      <div className="rounded-[12px] p-3" style={{ background: "var(--cream)" }}>
        <div className="flex items-center justify-between mb-2">
          <span style={{ fontSize: 11, fontWeight: 700, color: "var(--ink-3)", letterSpacing: "0.08em" }}>RISK BAND • TODAY</span>
          <span style={{ fontSize: 10, color: "var(--ink-3)" }}>180 students scored</span>
        </div>
        <div className="grid grid-cols-3 gap-2">
          {[["Low", 142, "#A8E6CF", "#155e3a"], ["Watch", 27, "#FFD4A3", "#7a3f00"], ["High", 11, "#FFADA8", "#7a1c18"]].map(([l, n, bg, fg]) => (
            <div key={l as string} className="rounded-[10px] p-2 text-center" style={{ background: bg as string }}>
              <div className="font-mono" style={{ fontSize: 18, fontWeight: 600, color: fg as string }}>{n as number}</div>
              <div style={{ fontSize: 10, fontWeight: 600, color: fg as string }}>{l as string}</div>
            </div>
          ))}
        </div>
      </div>
      <div>
        <div className="flex items-center gap-2 mb-2">
          <AlertTriangle size={13} style={{ color: "#7a1c18" }} />
          <span style={{ fontSize: 11, fontWeight: 700, color: "var(--ink-2)", letterSpacing: "0.04em" }}>HIGHEST PRIORITY</span>
        </div>
        <div className="space-y-2">
          {atRisk.map((s) => (
            <div key={s.name} className="flex items-center gap-3 p-2.5 rounded-[10px]" style={{ background: "var(--cream)" }}>
              <div className="font-mono w-10 text-center rounded-md py-1" style={{ background: s.score > 70 ? "#FFADA8" : s.score > 55 ? "#FFD4A3" : "#FFE7B0", color: s.score > 70 ? "#7a1c18" : "#7a3f00", fontSize: 13, fontWeight: 600 }}>{s.score}</div>
              <div className="flex-1 min-w-0">
                <div style={{ fontSize: 12, fontWeight: 700 }}>{s.name} <span style={{ color: "var(--ink-3)", fontWeight: 500 }}>· {s.cls}</span></div>
                <div className="truncate" style={{ fontSize: 11, color: "var(--ink-3)" }}>{s.drivers}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
      <div className="text-left rounded-[10px] p-2.5" style={{ background: "rgba(60,185,169,0.10)", border: "1px dashed rgba(0,106,96,0.3)" }}>
        <div className="flex items-center gap-1.5" style={{ fontSize: 11, fontWeight: 700, color: "var(--teal-deep)" }}><Sparkles size={11} /> SUGGESTED INTERVENTION</div>
        <p className="mt-1" style={{ fontSize: 12, color: "var(--ink-2)", lineHeight: 1.5 }}>Schedule a parent call for the 3 high-risk students this week. Recommend remedial Maths slot Tue/Thu.</p>
      </div>
    </div>
  );
}

// AI Report Card preview — narrative term summary.
export function AIReportCardPreview() {
  const grades = [
    { subj: "Mathematics", grade: "A", score: 88, trend: "↑" },
    { subj: "Science", grade: "A-", score: 84, trend: "↑" },
    { subj: "English", grade: "B+", score: 78, trend: "→" },
    { subj: "Hindi", grade: "A", score: 91, trend: "↑" },
  ];
  return (
    <div className="text-left space-y-3">
      <div className="rounded-[12px] p-3" style={{ background: "var(--navy)", color: "#ffffff" }}>
        <div className="flex items-center gap-1.5 mb-1" style={{ fontSize: 10, fontWeight: 700, letterSpacing: "0.10em", opacity: 0.7 }}>
          <Sparkles size={11} /> AI NARRATIVE · TERM 2
        </div>
        <p style={{ fontSize: 13, lineHeight: 1.55 }}>
          "Riya had a focused term. Her Mathematics is the steadiest it has been all year, and she's now consistently scoring above class average in Hindi. English is holding — short reading sprints will help."
        </p>
      </div>
      <div className="grid grid-cols-2 gap-2">
        {grades.map((g) => (
          <div key={g.subj} className="rounded-[10px] p-2.5" style={{ background: "var(--cream)" }}>
            <div className="flex items-center justify-between">
              <span style={{ fontSize: 11, fontWeight: 600, color: "var(--ink-2)" }}>{g.subj}</span>
              <span style={{ fontSize: 11, color: g.trend === "↑" ? "#155e3a" : "var(--ink-3)" }}>{g.trend}</span>
            </div>
            <div className="flex items-baseline gap-1.5 mt-0.5">
              <span style={{ fontSize: 20, fontWeight: 800, color: "var(--navy)" }}>{g.grade}</span>
              <span className="font-mono" style={{ fontSize: 11, color: "var(--ink-3)" }}>{g.score}</span>
            </div>
          </div>
        ))}
      </div>
      <div className="space-y-2">
        {[
          { icon: <Heart size={12} />, label: "STRENGTHS", body: "Number sense · Independent reading · Lab discipline", tone: "rgba(168,230,207,0.4)" },
          { icon: <Target size={12} />, label: "FOCUS AREAS", body: "English long-form writing · Punctuality in PE", tone: "rgba(255,212,163,0.5)" },
          { icon: <BookOpen size={12} />, label: "STUDY TIPS", body: "15-min daily journaling · NCERT exemplar Q14–18 weekly", tone: "rgba(60,185,169,0.16)" },
        ].map((b) => (
          <div key={b.label} className="rounded-[10px] p-2.5" style={{ background: b.tone }}>
            <div className="flex items-center gap-1.5" style={{ fontSize: 10, fontWeight: 700, color: "var(--ink)", letterSpacing: "0.06em" }}>{b.icon}{b.label}</div>
            <div className="mt-1" style={{ fontSize: 12, color: "var(--ink-2)", lineHeight: 1.5 }}>{b.body}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
