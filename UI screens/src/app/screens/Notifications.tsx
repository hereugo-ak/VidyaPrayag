import { useState } from "react";
import { motion } from "motion/react";
import { Bell, BookOpen, Calendar, Wallet, Megaphone, X, Check, ChevronRight } from "lucide-react";
import { PhoneFrame, VBackHeader, VBadge } from "../components/v/primitives";
import { notifications } from "../lib/mock";

const iconFor = (cat: string) => {
  if (cat === "attendance") return <Calendar size={16} />;
  if (cat === "academic") return <BookOpen size={16} />;
  if (cat === "fees") return <Wallet size={16} />;
  return <Megaphone size={16} />;
};

const toneFor = (cat: string): { bg: string; fg: string } => {
  if (cat === "attendance") return { bg: "rgba(255,212,163,0.55)", fg: "#7a3f00" };
  if (cat === "fees") return { bg: "rgba(255,173,168,0.55)", fg: "#7a1c18" };
  if (cat === "academic") return { bg: "rgba(60,185,169,0.18)", fg: "var(--teal-deep)" };
  return { bg: "rgba(168,230,207,0.42)", fg: "#155e3a" };
};

export function NotificationsScreen({ onBack, dark }: { onBack: () => void; dark?: boolean }) {
  const [filter, setFilter] = useState<"all" | "unread">("all");
  const [list, setList] = useState(notifications);
  const items = filter === "unread" ? list.filter((n) => n.unread) : list;
  const unread = list.filter((n) => n.unread).length;
  const markAll = () => setList(list.map((n) => ({ ...n, unread: false })));

  return (
    <PhoneFrame dark={dark} className={dark ? "dark" : ""}>
      <VBackHeader dark={dark} title="Notifications" onBack={onBack} action={
        <button onClick={markAll} className="inline-flex items-center gap-1" style={{ fontSize: 12, color: "var(--teal-deep)", fontWeight: 700 }}>
          <Check size={14} /> Mark all
        </button>
      } />
      <div className="flex-1 overflow-y-auto">
        {/* Hero */}
        <div className="px-5 pt-2 pb-5">
          <div className="rounded-[18px] p-5 relative overflow-hidden"
            style={{ background: "linear-gradient(135deg, var(--navy) 0%, #3b3870 100%)", color: "#ffffff" }}>
            <div aria-hidden className="absolute -right-10 -top-10 w-44 h-44 rounded-full"
              style={{ background: "radial-gradient(circle, rgba(60,185,169,0.45), transparent 70%)" }} />
            <div className="relative flex items-center gap-4">
              <div className="w-12 h-12 rounded-full inline-flex items-center justify-center"
                style={{ background: "rgba(255,255,255,0.14)", backdropFilter: "blur(8px)" }}>
                <Bell size={20} />
              </div>
              <div className="flex-1">
                <div style={{ fontSize: 12, opacity: 0.7, letterSpacing: "0.05em", textTransform: "uppercase" }}>Inbox</div>
                <div className="font-mono" style={{ fontSize: 28, fontWeight: 600, lineHeight: 1.1 }}>
                  {unread} <span style={{ fontSize: 14, opacity: 0.7 }}>unread</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Filter pills */}
        <div className="px-5 mb-3 flex gap-2">
          {(["all", "unread"] as const).map((f) => (
            <button key={f} onClick={() => setFilter(f)}
              className="px-3.5 py-1.5 rounded-full capitalize"
              style={{
                fontSize: 12, fontWeight: 700,
                background: filter === f ? "var(--navy)" : "var(--cream)",
                color: filter === f ? "#ffffff" : "var(--ink-2)",
              }}>
              {f} {f === "unread" && unread > 0 && `· ${unread}`}
            </button>
          ))}
        </div>

        {/* List */}
        <div className="px-5 pb-8 space-y-2">
          {items.length === 0 && (
            <div className="text-center py-12" style={{ color: "var(--ink-3)" }}>
              <div className="inline-flex w-14 h-14 rounded-full items-center justify-center mb-3" style={{ background: "var(--cream)" }}>
                <Check size={22} style={{ color: "var(--teal-deep)" }} />
              </div>
              <div style={{ fontWeight: 700, color: "var(--ink)" }}>You're all caught up</div>
              <div style={{ fontSize: 12 }}>No unread notifications.</div>
            </div>
          )}
          {items.map((n, i) => {
            const t = toneFor(n.category);
            return (
              <motion.button
                key={n.id}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.04 }}
                onClick={() => setList(list.map((x) => x.id === n.id ? { ...x, unread: false } : x))}
                className="w-full text-left rounded-[14px] p-3.5 relative"
                style={{
                  background: "#ffffff",
                  border: "1px solid rgba(38,35,77,0.06)",
                  boxShadow: n.unread ? "0 6px 14px -6px rgba(38,35,77,0.12)" : "0 2px 6px rgba(38,35,77,0.04)",
                }}>
                {n.unread && <span className="absolute top-3 right-3 rounded-full" style={{ width: 8, height: 8, background: "var(--teal-deep)" }} />}
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded-[10px] inline-flex items-center justify-center shrink-0"
                    style={{ background: t.bg, color: t.fg }}>
                    {iconFor(n.category)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <VBadge tone={n.category === "fees" ? "danger" : n.category === "attendance" ? "warning" : n.category === "academic" ? "arctic" : "success"}>
                        {n.category}
                      </VBadge>
                      <span style={{ fontSize: 11, color: "var(--ink-3)" }}>{n.time}</span>
                    </div>
                    <div className="mt-1.5" style={{ fontSize: 14, fontWeight: 600, color: "var(--ink)" }}>{n.title}</div>
                    <div className="mt-0.5" style={{ fontSize: 12, color: "var(--ink-2)" }}>{n.body}</div>
                  </div>
                  <ChevronRight size={16} style={{ color: "var(--ink-3)" }} className="mt-1 shrink-0" />
                </div>
              </motion.button>
            );
          })}
        </div>

        {/* Settings link */}
        <div className="px-5 pb-10">
          <button className="w-full rounded-[12px] py-3 inline-flex items-center justify-center gap-2"
            style={{ background: "var(--cream)", fontSize: 13, fontWeight: 600, color: "var(--ink-2)" }}>
            <X size={14} /> Notification preferences
          </button>
        </div>
      </div>
    </PhoneFrame>
  );
}
