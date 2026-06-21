/** Token + onboarding-draft persistence helpers (localStorage). See ARCHITECTURE.md §6. */

import type { AuthTokenResponse } from "./api";

const AUTH_KEY = "enrollplus.auth.v1";

export interface StoredAuth {
  token: string;
  refreshToken: string;
  userId: string;
  name: string;
  role: string;
}

export function saveAuth(res: AuthTokenResponse): StoredAuth {
  const stored: StoredAuth = {
    token: res.token,
    refreshToken: res.refresh_token,
    userId: res.user_id,
    name: res.name,
    role: res.role,
  };
  if (typeof window !== "undefined") {
    window.localStorage.setItem(AUTH_KEY, JSON.stringify(stored));
  }
  return stored;
}

export function loadAuth(): StoredAuth | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(AUTH_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as StoredAuth;
  } catch {
    return null;
  }
}

export function clearAuth() {
  if (typeof window !== "undefined") window.localStorage.removeItem(AUTH_KEY);
}
