/**
 * Typed fetch client over the existing Ktor backend.
 *
 * Wraps the server's uniform envelope (core/ApiResponse.kt):
 *   success:  { "success": true,  "message": "…", "data": {…} }
 *   failure:  { "success": false, "message": "…", "error_code": "…" }
 *
 * No axios, the platform `fetch` covers everything we need (see ARCHITECTURE.md §5).
 */

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") ?? "http://localhost:8080";

export interface ApiEnvelope<T> {
  success: boolean;
  message?: string;
  data?: T;
  error_code?: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;
  constructor(message: string, status: number, code?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
  token?: string | null;
  /** Extra headers (e.g. Accept-Language). */
  headers?: Record<string, string>;
}

/**
 * Core request. Unwraps the envelope and throws a typed {@link ApiError}
 * carrying the server `error_code` so callers can map it to inline messages.
 */
export async function apiRequest<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, token, headers = {} } = opts;

  const finalHeaders: Record<string, string> = {
    Accept: "application/json",
    Platform: "web",
    ...headers,
  };
  if (body !== undefined) finalHeaders["Content-Type"] = "application/json";
  if (token) finalHeaders["Authorization"] = `Bearer ${token}`;

  let res: Response;
  try {
    res = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers: finalHeaders,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      cache: "no-store",
    });
  } catch {
    throw new ApiError(
      "Cannot reach the server. Check your connection and try again.",
      0,
      "NETWORK_ERROR"
    );
  }

  let envelope: ApiEnvelope<T> | null = null;
  const text = await res.text();
  if (text) {
    try {
      envelope = JSON.parse(text) as ApiEnvelope<T>;
    } catch {
      envelope = null;
    }
  }

  if (!res.ok || (envelope && envelope.success === false)) {
    const message =
      envelope?.message ?? `Request failed (${res.status}). Please try again.`;
    throw new ApiError(message, res.status, envelope?.error_code);
  }

  return (envelope?.data ?? (envelope as unknown)) as T;
}

// ─── Typed endpoints the website uses ─────────────────────────────────────────

export interface AuthTokenResponse {
  token: string;
  refresh_token: string;
  user_id: string;
  name: string;
  role: string;
  profile_completed: boolean;
  must_change_password?: boolean;
}

export interface SchoolRegisterPayload {
  name: string;
  identifier: string; // email
  password: string;
  school_name: string;
  board?: string;
  school_type?: string;
  city?: string;
  state?: string;
  contact_phone?: string;
  device_info?: { device_id?: string; platform?: string };
}

/** POST /api/v1/auth/register-school, atomically mints school_admin + school. */
export function registerSchool(payload: SchoolRegisterPayload) {
  return apiRequest<AuthTokenResponse>("/api/v1/auth/register-school", {
    method: "POST",
    body: payload,
  });
}

/** POST /api/v1/auth/login, email+password for school admins. */
export function loginAdmin(identifier: string, password: string) {
  return apiRequest<AuthTokenResponse>("/api/v1/auth/login", {
    method: "POST",
    body: { identifier, password, role: "school_admin", device_info: { platform: "web" } },
  });
}

export interface SubmitStepResponse {
  next_step: string | null;
  is_onboarding_complete: boolean;
  redirect_to_home: boolean;
}

/** POST /api/v1/onboarding/submit, upserts drafts + persists real rows. */
export function submitOnboardingStep(
  token: string,
  obStepType: string,
  dataPayload: Record<string, unknown>,
  isFinal = false
) {
  return apiRequest<SubmitStepResponse>("/api/v1/onboarding/submit", {
    method: "POST",
    token,
    body: {
      ob_step_type: obStepType,
      is_final_submission: isFinal,
      data_payload: dataPayload,
    },
  });
}

export interface OnboardingStatusResponse {
  school_id: string | null;
  is_complete: boolean;
  completion_percent: number;
  resume_step: string;
  total_step_count: number;
  steps: { step: string; current_step_count: number; is_done: boolean }[];
}

/** GET /api/v1/onboarding/status, server-truth completion state. */
export function getOnboardingStatus(token: string) {
  return apiRequest<OnboardingStatusResponse>("/api/v1/onboarding/status", { token });
}

// ─── Student bulk import (onboarding CSV upload) ──────────────────────────────
// Mirrors server BulkImportStudentsResponse from SchoolStudentsRouting.kt.
// The /import endpoint accepts EITHER a parsed `students` array OR raw `csv`
// text; we send the raw CSV so the server's battle-tested parseStudentCsv (which
// already handles header aliases, quoted fields and per-row validation) is the
// single source of truth, no duplicate parsing contract to drift.
export interface BulkImportRowResult {
  row: number;
  success: boolean;
  student_code?: string | null;
  error?: string | null;
}
export interface BulkImportStudentsResponse {
  total: number;
  inserted: number;
  failed: number;
  results: BulkImportRowResult[];
}

/** POST /api/v1/school/students/import with raw CSV text (school-admin token). */
export function importStudentsCsv(token: string, csv: string) {
  return apiRequest<BulkImportStudentsResponse>("/api/v1/school/students/import", {
    method: "POST",
    token,
    body: { csv },
  });
}
