import { useState } from "react";
import { Splash, Login, TeacherFirstLogin, ParentLinkChild, SchoolOnboarding } from "./screens/Auth";
import { AdminApp, AnnouncementDetail } from "./screens/Admin";
import { TeacherApp } from "./screens/Teacher";
import { ParentApp } from "./screens/Parent";
import { DiscoveryApp, AcademicCalendar } from "./screens/Discovery";
import { NotificationsScreen } from "./screens/Notifications";

type Stop =
  | "splash" | "login" | "teacher-first" | "parent-link" | "onboarding"
  | "admin" | "teacher" | "parent" | "discovery" | "calendar-dark" | "calendar-light" | "notifications";

const stops: { id: Stop; label: string }[] = [
  { id: "splash", label: "Splash" },
  { id: "login", label: "Login" },
  { id: "onboarding", label: "School Onboarding" },
  { id: "teacher-first", label: "Teacher First Login" },
  { id: "parent-link", label: "Parent — Link Child" },
  { id: "admin", label: "Admin Portal" },
  { id: "teacher", label: "Teacher Portal" },
  { id: "parent", label: "Parent Portal" },
  { id: "discovery", label: "Discovery" },
  { id: "calendar-dark", label: "Calendar (dark)" },
  { id: "calendar-light", label: "Calendar (light)" },
  { id: "notifications", label: "Notifications" },
];

export default function App() {
  const [stop, setStop] = useState<Stop>("splash");
  const [openSwitcher, setOpenSwitcher] = useState(false);
  const [night, setNight] = useState(false);

  let screen: React.ReactNode = null;
  switch (stop) {
    case "splash": screen = <Splash onDone={() => setStop("login")} />; break;
    case "login": screen = <Login onLogin={(p) => setStop(p === "admin" ? "admin" : p === "teacher" ? "teacher" : "parent")} />; break;
    case "teacher-first": screen = <TeacherFirstLogin onDone={() => setStop("teacher")} />; break;
    case "parent-link": screen = <ParentLinkChild onDone={() => setStop("parent")} />; break;
    case "onboarding": screen = <SchoolOnboarding onDone={() => setStop("admin")} />; break;
    case "admin": screen = <AdminApp onExit={() => setStop("login")} />; break;
    case "teacher": screen = <TeacherApp onExit={() => setStop("login")} />; break;
    case "parent": screen = <ParentApp onExit={() => setStop("login")} />; break;
    case "discovery": screen = <DiscoveryApp onExit={() => setStop("login")} />; break;
    case "calendar-dark": screen = <AcademicCalendar dark onBack={() => setStop("admin")} />; break;
    case "calendar-light": screen = <AcademicCalendar onBack={() => setStop("parent")} />; break;
    case "notifications": screen = <NotificationsScreen onBack={() => setStop("parent")} />; break;
  }

  return (
    <div className={"relative size-full " + (night ? "theme-night" : "")} style={{ background: "var(--lavender)", minHeight: "100vh" }}>
      <div className="mx-auto" style={{ maxWidth: 440 }}>{screen}</div>

      {/* Floating dev portal switcher + night toggle */}
      <div className="fixed top-4 right-4 z-[9999] flex items-center gap-2" style={{ fontFamily: "Plus Jakarta Sans, system-ui" }}>
        <button onClick={() => setNight(!night)}
          aria-label="Toggle night mode"
          className="w-9 h-9 rounded-full inline-flex items-center justify-center transition-all"
          style={{
            background: night ? "#1a1a1d" : "#ffffff",
            color: night ? "#f4f4f6" : "#26234d",
            border: night ? "1px solid rgba(255,255,255,0.10)" : "1px solid rgba(38,35,77,0.10)",
            boxShadow: night ? "0 6px 14px rgba(0,0,0,0.5)" : "0 6px 14px rgba(38,35,77,0.18)",
          }}>
          {night ? (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
            </svg>
          ) : (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
            </svg>
          )}
        </button>
        <button onClick={() => setOpenSwitcher(!openSwitcher)}
          className="px-3 py-2 rounded-full inline-flex items-center gap-2"
          style={{ background: night ? "#f3f0ff" : "var(--navy)", color: night ? "#0a0a0a" : "#ffffff", fontSize: 12, fontWeight: 700, boxShadow: night ? "0 8px 24px rgba(0,0,0,0.5)" : "0 8px 24px -6px rgba(38,35,77,0.4)" }}>
          <span style={{ width: 6, height: 6, borderRadius: "50%", background: "var(--teal)" }} />
          {stops.find((s) => s.id === stop)?.label}
        </button>
        {openSwitcher && (
          <div className="mt-2 w-64 rounded-[14px] overflow-hidden"
            style={{ background: "#ffffff", border: "1px solid rgba(38,35,77,0.08)", boxShadow: "0 16px 40px rgba(38,35,77,0.18)" }}>
            <div className="px-3 py-2" style={{ fontSize: 10, fontWeight: 700, letterSpacing: "0.08em", color: "var(--ink-3)", textTransform: "uppercase" }}>Jump to screen</div>
            {stops.map((s) => (
              <button key={s.id} onClick={() => { setStop(s.id); setOpenSwitcher(false); }}
                className="w-full text-left px-3 py-2 transition-colors"
                style={{ fontSize: 13, color: stop === s.id ? "var(--teal-deep)" : "var(--ink)", background: stop === s.id ? "rgba(60,185,169,0.10)" : "transparent", fontWeight: stop === s.id ? 700 : 500 }}>
                {s.label}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
