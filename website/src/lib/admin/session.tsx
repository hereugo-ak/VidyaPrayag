"use client";

/**
 * Admin session, JWT access + refresh token storage and a React context that
 * gates every /admin route. Tokens live in localStorage under a dedicated key
 * (the wizard's `enrollplus.auth.v1` is separate and short-lived). On a 401 the
 * authed client transparently rotates the token via /api/v1/auth/refresh
 * (server rotates the refresh token on every use, see AuthRouting.kt §refresh).
 *
 * Logout revokes the refresh session server-side and clears local state.
 */

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { useRouter } from "next/navigation";
import type { AuthTokenResponse } from "./types";

const ADMIN_KEY = "enrollplus.admin.v1";

export interface AdminSession {
  token: string;
  refreshToken: string;
  userId: string;
  name: string;
  role: string;
}

export function readSession(): AdminSession | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(ADMIN_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AdminSession;
  } catch {
    return null;
  }
}

export function writeSession(res: AuthTokenResponse): AdminSession {
  const s: AdminSession = {
    token: res.token,
    refreshToken: res.refresh_token,
    userId: res.user_id,
    name: res.name,
    role: res.role,
  };
  if (typeof window !== "undefined") {
    window.localStorage.setItem(ADMIN_KEY, JSON.stringify(s));
  }
  return s;
}

export function patchTokens(token: string, refreshToken: string) {
  const cur = readSession();
  if (!cur) return;
  const next = { ...cur, token, refreshToken };
  if (typeof window !== "undefined") {
    window.localStorage.setItem(ADMIN_KEY, JSON.stringify(next));
  }
}

export function eraseSession() {
  if (typeof window !== "undefined") window.localStorage.removeItem(ADMIN_KEY);
}

// ── React context ────────────────────────────────────────────────────────────

interface AdminAuthValue {
  session: AdminSession | null;
  ready: boolean;
  signIn: (res: AuthTokenResponse) => void;
  signOut: () => Promise<void>;
}

const AdminAuthContext = createContext<AdminAuthValue | null>(null);

export function AdminAuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [session, setSession] = useState<AdminSession | null>(null);
  const [ready, setReady] = useState(false);
  const signingOut = useRef(false);

  useEffect(() => {
    setSession(readSession());
    setReady(true);
    // Keep tabs in sync (login/logout in another tab).
    const onStorage = (e: StorageEvent) => {
      if (e.key === ADMIN_KEY) setSession(readSession());
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  const signIn = useCallback((res: AuthTokenResponse) => {
    setSession(writeSession(res));
  }, []);

  const signOut = useCallback(async () => {
    if (signingOut.current) return;
    signingOut.current = true;
    const s = readSession();
    // Best-effort server-side revocation; never block the user from leaving.
    if (s) {
      try {
        await fetch(
          `${process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") ?? "http://localhost:8080"}/api/v1/auth/logout`,
          {
            method: "POST",
            headers: {
              Authorization: `Bearer ${s.token}`,
              "Content-Type": "application/json",
              Platform: "web",
            },
            body: JSON.stringify({ refresh_token: s.refreshToken }),
          }
        );
      } catch {
        /* ignore */
      }
    }
    eraseSession();
    setSession(null);
    signingOut.current = false;
    router.replace("/login");
  }, [router]);

  const value = useMemo(
    () => ({ session, ready, signIn, signOut }),
    [session, ready, signIn, signOut]
  );

  return (
    <AdminAuthContext.Provider value={value}>
      {children}
    </AdminAuthContext.Provider>
  );
}

export function useAdminAuth(): AdminAuthValue {
  const ctx = useContext(AdminAuthContext);
  if (!ctx) throw new Error("useAdminAuth must be used inside <AdminAuthProvider>");
  return ctx;
}
