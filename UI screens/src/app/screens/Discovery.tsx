import { useState } from "react";
import { Search, Filter, MapPin, Star, Heart, Share2, Plus, ChevronRight, GraduationCap, Phone, ArrowRight, Check } from "lucide-react";
import { PhoneFrame, VCard, VBadge, VTag, VButton, VInput, VBackHeader, VTopTabs, Label, VComingSoon } from "../components/v/primitives";
import { SRIPreview } from "../components/v/mockups";
import { discoverySchools, calendarEvents } from "../lib/mock";

type View = "list" | "profile" | "compare";

export function DiscoveryApp({ onExit }: { onExit: () => void }) {
  const [view, setView] = useState<View>("list");
  const [active, setActive] = useState(discoverySchools[0]);
  const [compare, setCompare] = useState<string[]>([]);

  return (
    <PhoneFrame>
      {view === "list" && (
        <DiscoveryList
          compare={compare}
          onCompareToggle={(id) => setCompare(compare.includes(id) ? compare.filter((c) => c !== id) : compare.length < 3 ? [...compare, id] : compare)}
          onOpen={(s) => { setActive(s); setView("profile"); }}
          onCompare={() => setView("compare")}
          onExit={onExit}
        />
      )}
      {view === "profile" && <SchoolProfile school={active} onBack={() => setView("list")} />}
      {view === "compare" && <SchoolCompare ids={compare} onBack={() => setView("list")} />}
    </PhoneFrame>
  );
}

function DiscoveryList({ compare, onCompareToggle, onOpen, onCompare, onExit }: { compare: string[]; onCompareToggle: (id: string) => void; onOpen: (s: typeof discoverySchools[number]) => void; onCompare: () => void; onExit: () => void }) {
  return (
    <>
      <div className="px-5 pt-5 pb-3" style={{ background: "var(--cloud-pure)", borderBottom: "1px solid var(--border-light-1)" }}>
        <div className="flex items-center justify-between mb-3">
          <div>
            <Label>Discover</Label>
            <h2 className="mt-1">Find your child's school</h2>
          </div>
          <button onClick={onExit} className="px-3 py-1.5 rounded-full" style={{ background: "var(--cloud)", fontSize: 12, fontWeight: 600 }}>Exit</button>
        </div>
        <VInput placeholder="Find schools near you or by name" icon={<Search size={16} />} />
        <div className="mt-3 flex gap-2 overflow-x-auto no-scrollbar">
          {["📍 Within 3 km", "🎓 CBSE", "💰 Fee range", "🏫 Type", "⭐ SRI"].map((f, i) => (
            <VTag key={f} active={i === 0}>{f}</VTag>
          ))}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-3" style={{ paddingBottom: compare.length ? 96 : 24 }}>
        {discoverySchools.map((s) => (
          <VCard key={s.id} padded>
            <div className="rounded-[12px] h-36 mb-3 overflow-hidden relative">
              <img src={s.photo} alt={s.name} className="w-full h-full object-cover" />
              <div className="absolute inset-0" style={{ background: "linear-gradient(180deg, rgba(0,0,0,0) 50%, rgba(0,0,0,0.35) 100%)" }} />
              <div className="absolute top-3 left-3 inline-flex items-center gap-1 px-2 py-1 rounded-full backdrop-blur-md" style={{ background: "rgba(255,255,255,0.85)" }}>
                <MapPin size={11} style={{ color: "var(--teal-deep)" }} /> <span style={{ fontSize: 11, fontWeight: 600, color: "var(--ink)" }}>{s.distance}</span>
              </div>
            </div>
            <div className="flex items-start justify-between gap-2">
              <div className="flex-1">
                <h3>{s.name}</h3>
                <div className="mt-1 flex flex-wrap gap-1.5">
                  <VBadge tone="arctic">{s.board}</VBadge>
                  <VBadge tone="neutral">{s.type}</VBadge>
                </div>
              </div>
              <div className="text-right">
                <div className="inline-flex items-center gap-1 px-2 py-1 rounded-full font-mono" style={{ background: "rgba(200,222,255,0.30)", fontSize: 12, color: "#0a3a76" }}>
                  <Star size={12} fill="currentColor" /> {s.sri}
                </div>
                <div className="font-mono mt-1" style={{ fontSize: 11, color: "var(--text-light-2)" }}>SRI score</div>
              </div>
            </div>
            <div className="mt-3 flex items-center gap-3 flex-wrap" style={{ fontSize: 12, color: "var(--text-light-2)" }}>
              <span className="inline-flex items-center gap-1"><MapPin size={12} /> {s.distance}</span>
              <span>{s.medium}</span>
              <span>Board result {s.result}</span>
            </div>
            <div className="font-mono mt-2" style={{ fontSize: 13 }}>{s.feeRange} / year</div>
            <div className="mt-3 flex gap-2">
              <VButton variant="secondary" size="sm" full onClick={() => onCompareToggle(s.id)}>
                {compare.includes(s.id) ? <><Check size={14} /> In compare</> : "Compare"}
              </VButton>
              <VButton size="sm" full tone="sky" onClick={() => onOpen(s)}>Enquire</VButton>
            </div>
          </VCard>
        ))}
      </div>

      {compare.length > 0 && (
        <div className="sticky bottom-0 left-0 right-0 px-5 py-3 flex items-center gap-3" style={{ background: "var(--void)", color: "var(--cloud)" }}>
          <div className="flex -space-x-2">
            {compare.map((id) => {
              const s = discoverySchools.find((d) => d.id === id)!;
              return <div key={id} className="w-8 h-8 rounded-full inline-flex items-center justify-center" style={{ background: "var(--arctic)", color: "var(--void)", border: "2px solid var(--void)", fontSize: 11, fontWeight: 700 }}>{s.name.split(" ").map((w) => w[0]).join("").slice(0, 2)}</div>;
            })}
          </div>
          <div className="flex-1" style={{ fontSize: 13 }}>{compare.length} school{compare.length > 1 ? "s" : ""} selected</div>
          <VButton size="sm" tone="sky" onClick={onCompare}>Compare now <ArrowRight size={14} /></VButton>
        </div>
      )}
    </>
  );
}

function SchoolProfile({ school, onBack }: { school: typeof discoverySchools[number]; onBack: () => void }) {
  const [enquireOpen, setEnquireOpen] = useState(false);
  return (
    <>
      <VBackHeader title="School profile" onBack={onBack} action={<button><Share2 size={18} /></button>} />
      <div className="flex-1 overflow-y-auto">
        <div className="h-56 relative overflow-hidden">
          <img src={school.photo} alt={school.name} className="w-full h-full object-cover" />
          <div className="absolute inset-0" style={{ background: "linear-gradient(180deg, rgba(0,0,0,0) 40%, rgba(0,0,0,0.5) 100%)" }} />
        </div>
        <div className="px-5 py-4 space-y-4">
          <div>
            <h2>{school.name}</h2>
            <div className="mt-1 flex flex-wrap gap-1.5">
              <VBadge tone="arctic">{school.board}</VBadge>
              <VBadge tone="neutral">{school.type}</VBadge>
              <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full" style={{ background: "rgba(200,222,255,0.30)", fontSize: 11, color: "#0a3a76", fontWeight: 600 }}>
                <Star size={11} fill="currentColor" /> SRI {school.sri}
              </span>
            </div>
            <div className="mt-2 flex items-center gap-3" style={{ fontSize: 12, color: "var(--text-light-2)" }}>
              <span className="inline-flex items-center gap-1"><MapPin size={12} /> {school.distance}</span>
              <span>{school.medium}</span>
            </div>
          </div>
          <div className="flex gap-2">
            <VButton variant="secondary" size="sm"><Heart size={14} /></VButton>
            <VButton variant="secondary" size="sm" full>Compare</VButton>
            <VButton size="sm" full tone="sky" onClick={() => setEnquireOpen(true)}><Phone size={14} /> Enquire now</VButton>
          </div>

          <Section title="About">
            <p style={{ fontSize: 13, color: "var(--text-light-2)" }}>Founded 1987 • English-medium co-educational institution focused on holistic CBSE-pattern education with strong emphasis on STEM and the arts.</p>
            <div className="mt-2 flex flex-wrap gap-1.5">
              {["Smart Classrooms", "Library", "Sports Ground", "Computer Lab", "Canteen", "Transport"].map((f) => <VBadge key={f} tone="neutral">{f}</VBadge>)}
            </div>
          </Section>

          <Section title="Academics">
            <Row k="Board" v={school.board} />
            <Row k="Classes offered" v="Nursery – 12" />
            <Row k="Medium" v={school.medium} />
            <Row k="Board result (last yr)" v={school.result} />
            <Row k="Teacher–student ratio" v="Coming Soon" />
          </Section>

          <Section title="Fee structure">
            <Row k="Tuition (annual)" v={school.feeRange} />
            <Row k="Admission" v="₹ 18,000 (one-time)" />
            <Row k="Transport" v="₹ 22,000 / yr" />
          </Section>

          <Section title="SRI breakdown">
            <VComingSoon title="School Reputation Index" description="Our 11-signal score lets you compare schools on academics, safety, facilities and parent sentiment." preview={<SRIPreview score={school.sri} />} />
          </Section>

          <Section title="Parent reviews">
            <VCard padded>
              <div className="flex items-center gap-2">
                <Star size={14} fill="#FFD4A3" stroke="none" /><Star size={14} fill="#FFD4A3" stroke="none" /><Star size={14} fill="#FFD4A3" stroke="none" /><Star size={14} fill="#FFD4A3" stroke="none" />
                <Star size={14} stroke="#FFD4A3" />
                <span className="font-mono" style={{ fontSize: 12 }}>4.2 / 5 — 84 reviews</span>
              </div>
              <p className="mt-3" style={{ fontSize: 13, color: "var(--text-light-2)" }}>
                "Teachers actually respond. Attendance updates land within minutes." — Verified parent
              </p>
              <div className="mt-1"><VBadge tone="success">Verified VidyaSetu parents only</VBadge></div>
            </VCard>
          </Section>

          <Section title="Location">
            <VCard padded>
              <div className="h-32 rounded-[10px] flex items-center justify-center" style={{ background: "var(--cloud)" }}>
                <MapPin size={28} style={{ opacity: 0.4 }} />
              </div>
              <p className="mt-2" style={{ fontSize: 13 }}>12 Civil Lines, Lucknow, UP 226001</p>
            </VCard>
          </Section>

          <Section title="Admission">
            <Row k="Age criteria" v="3+ (Nursery)" />
            <Row k="Process" v="Online form → interaction → confirmation" />
            <Row k="Admissions officer" v="Mrs. R. Singh — +91 522 220 4421" />
          </Section>
        </div>
      </div>

      {enquireOpen && (
        <div className="absolute inset-0 z-50 flex items-end" style={{ background: "rgba(8,8,8,0.45)" }}>
          <div className="w-full px-5 py-6 rounded-t-[20px]" style={{ background: "var(--cloud-pure)" }}>
            <h3>Send enquiry</h3>
            <p style={{ fontSize: 12, color: "var(--text-light-2)" }}>The admissions team will respond within 2 working days.</p>
            <div className="mt-4 space-y-3">
              <VInput label="Your name" defaultValue="Sneha Sharma" />
              <VInput label="Child's name" placeholder="" />
              <div className="grid grid-cols-2 gap-2">
                <VInput label="Current class" placeholder="—" />
                <VInput label="Apply for class" placeholder="—" />
              </div>
              <VInput label="Message (optional)" placeholder="Any specific question?" />
            </div>
            <div className="mt-5 flex gap-2">
              <VButton variant="ghost" full onClick={() => setEnquireOpen(false)}>Cancel</VButton>
              <VButton full tone="sky" stateful successLabel="Sent" onClick={() => setEnquireOpen(false)}>Submit enquiry</VButton>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <Label>{title}</Label>
      <div className="mt-2 space-y-2">{children}</div>
    </div>
  );
}

function Row({ k, v }: { k: string; v: string }) {
  return (
    <div className="flex items-center justify-between py-1.5" style={{ borderBottom: "1px solid var(--border-light-1)" }}>
      <span style={{ fontSize: 13, color: "var(--text-light-2)" }}>{k}</span>
      <span style={{ fontSize: 13, fontWeight: 600 }}>{v}</span>
    </div>
  );
}

function SchoolCompare({ ids, onBack }: { ids: string[]; onBack: () => void }) {
  const items = ids.map((id) => discoverySchools.find((s) => s.id === id)!).filter(Boolean);
  const rows: { label: string; pick: (s: typeof discoverySchools[number]) => string }[] = [
    { label: "Board", pick: (s) => s.board },
    { label: "Type", pick: (s) => s.type },
    { label: "Medium", pick: (s) => s.medium },
    { label: "Fee range", pick: (s) => s.feeRange },
    { label: "SRI score", pick: (s) => `${s.sri}` },
    { label: "Distance", pick: (s) => s.distance },
    { label: "Board result", pick: (s) => s.result },
    { label: "Co-ed", pick: (s) => (s.coed ? "Yes" : "Girls only") },
  ];
  return (
    <>
      <VBackHeader title="Compare schools" onBack={onBack} />
      <div className="flex-1 overflow-auto px-3 py-4">
        <div className="flex gap-2 px-2 mb-3">
          <div className="w-24 shrink-0" />
          {items.map((s) => (
            <div key={s.id} className="flex-1 text-center rounded-[10px] p-2" style={{ background: "var(--cloud-pure)", border: "1px solid var(--border-light-1)" }}>
              <div className="w-10 h-10 mx-auto rounded-full inline-flex items-center justify-center" style={{ background: "var(--arctic)" }}>
                <GraduationCap size={18} />
              </div>
              <div className="mt-1" style={{ fontSize: 11, fontWeight: 600 }}>{s.name.split(" — ")[0]}</div>
            </div>
          ))}
        </div>
        <VCard>
          {rows.map((r, i) => {
            const vals = items.map(r.pick);
            const best = r.label === "SRI score" ? vals.indexOf(`${Math.max(...items.map((s) => s.sri))}`) : -1;
            return (
              <div key={r.label} className="flex items-center gap-2 py-2.5" style={{ borderTop: i ? "1px solid var(--border-light-1)" : "none" }}>
                <div className="w-24 shrink-0" style={{ fontSize: 11, color: "var(--text-light-2)", textTransform: "uppercase", letterSpacing: "0.06em" }}>{r.label}</div>
                {vals.map((v, ix) => (
                  <div key={ix} className="flex-1 text-center px-1 py-1.5 rounded-md" style={{ background: best === ix ? "rgba(200,222,255,0.30)" : "transparent", fontSize: 12, fontWeight: 600 }}>{v}</div>
                ))}
              </div>
            );
          })}
        </VCard>
        <VButton className="mt-4" full size="lg" tone="sky" stateful successLabel="Enquiries sent">Enquire to all selected</VButton>
      </div>
    </>
  );
}

// ─── Academic Calendar (cross-portal) ────────────────────────────────────────
export function AcademicCalendar({ onBack, dark }: { onBack: () => void; dark?: boolean }) {
  return (
    <PhoneFrame dark={dark} className={dark ? "dark" : ""}>
      <VBackHeader dark={dark} title="Academic calendar" onBack={onBack} />
      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-4">
        <div className="flex items-center justify-between">
          <button className="px-3 py-1.5 rounded-full" style={{ background: dark ? "rgba(245,245,243,0.06)" : "var(--cloud)", fontSize: 13 }}>‹ May</button>
          <span style={{ fontWeight: 700 }}>June 2026</span>
          <button className="px-3 py-1.5 rounded-full" style={{ background: dark ? "rgba(245,245,243,0.06)" : "var(--cloud)", fontSize: 13 }}>Jul ›</button>
        </div>
        <VCard dark={dark}>
          <div className="grid grid-cols-7 gap-1.5">
            {["S", "M", "T", "W", "T", "F", "S"].map((d, i) => <div key={i} className="text-center" style={{ fontSize: 10, opacity: 0.6 }}>{d}</div>)}
            {Array.from({ length: 30 }, (_, i) => {
              const day = i + 1;
              const ev = calendarEvents.find((e) => e.day === day);
              const tone = ev?.type === "academic" ? "var(--arctic)" : ev?.type === "holiday" ? "var(--warning)" : ev?.type === "deadline" ? "var(--danger)" : ev ? "var(--cloud)" : "transparent";
              return (
                <div key={day} className="aspect-square rounded-full flex flex-col items-center justify-center" style={{ background: ev ? tone : "transparent", color: ev ? "var(--void)" : dark ? "var(--cloud)" : "var(--void)", fontSize: 11 }}>
                  {day}
                </div>
              );
            })}
          </div>
        </VCard>
        <Label dark={dark}>Upcoming events</Label>
        {calendarEvents.map((e) => (
          <VCard dark={dark} key={e.day}>
            <div className="flex items-center gap-3">
              <div className="w-12 text-center">
                <div className="font-mono" style={{ fontSize: 20, fontWeight: 600 }}>{e.day}</div>
                <div style={{ fontSize: 10, opacity: 0.6 }}>JUN</div>
              </div>
              <div className="flex-1">
                <div style={{ fontWeight: 600 }}>{e.label}</div>
                <div style={{ fontSize: 11, opacity: 0.6, textTransform: "capitalize" }}>{e.type}</div>
              </div>
              <ChevronRight size={16} className="opacity-50" />
            </div>
          </VCard>
        ))}
      </div>
    </PhoneFrame>
  );
}
