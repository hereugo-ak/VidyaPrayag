# Spec Migration Tracker — Standard Format Alignment

> **Created:** 2026-06-28
> **Template:** `_SPEC_TEMPLATE.md` (25 mandatory sections + 6 optional)
> **Total specs to migrate:** 69
>
> **⚠ Migration Principle:**
> - Do NOT reduce content from the original spec. All existing data must be preserved.
> - If the original spec has more sections than the template, keep the extra sections — do not remove them.
> - Only ADD what is missing from the standard template. Never delete existing content.
> - Reorganize existing content into the standard section structure, but keep every detail.
> **Migrated:** 69
> **In Progress:** 0
> **Remaining:** 0

---

## Migration Status Legend

| Symbol | Meaning |
|---|---|
| 🔴 | Not started |
| 🟡 | In progress |
| 🟢 | Migrated to standard format |

---

## Phase 0 — Survival / Platform Infrastructure (Priority: Critical)

| # | Spec File | Status | Migrated Date | Notes |
|---|---|---|---|---|
| P0-01 | `FEE_PAYMENT_SPEC.md` | 🟢 | 2026-06-28 | Critical, effort L — migrated |
| P0-02 | `WHATSAPP_INTEGRATION_SPEC.md` | 🟢 | 2026-06-28 | Critical, effort M — migrated |
| P0-03 | `NEP_COMPLIANCE_SPEC.md` | 🟢 | 2026-06-28 | Critical, effort L — migrated |
| P0-04 | `DPDP_COMPLIANCE_SPEC.md` | 🟢 | 2026-06-28 | Critical, effort L, depends on AUDIT_LOG — migrated |
| P0-05 | `BULK_IMPORT_EXPORT_SPEC.md` | 🟢 | 2026-06-28 | High, effort M — migrated |
| P0-06 | `AI_INFRASTRUCTURE_SPEC.md` | 🟢 | 2026-06-28 | Critical, effort M — migrated |
| P0-07 | `OFFLINE_MODE_SPEC.md` | 🟢 | 2026-06-28 | High, effort L — migrated |
| P0-08 | `DARK_MODE_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P0-09 | `TABLET_LAYOUT_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P0-10 | `WEB_APP_SPEC.md` | 🟢 | 2026-06-28 | High, effort L — migrated |
| P0-11 | `IOS_PUSH_SPEC.md` | 🟢 | 2026-06-28 | High, effort M — migrated |
| P0-12 | `TWO_FACTOR_AUTH_SPEC.md` | 🟢 | 2026-06-28 | High, effort M — migrated |
| P0-13 | `AUDIT_LOG_SPEC.md` | 🟢 | 2026-06-28 | High, effort L — migrated |

## Phase 1 — Core: Operational Features (Priority: High)

| # | Spec File | Status | Migrated Date | Notes |
|---|---|---|---|---|
| P1-01 | `AI_EXAM_ANALYSIS_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P1-02 | `AI_FEE_REMINDER_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P1-03 | `AI_TIMETABLE_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort L — migrated |
| P1-04 | `AI_NL_QUERY_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort L — migrated |
| P1-05 | `AI_REPORT_CARD_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort L — migrated |
| P1-06 | `OCR_ADMISSION_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P1-07 | `TRANSPORT_TRACKING_SPEC.md` | 🟢 | 2026-06-28 | Large, effort L — migrated |
| P1-08 | `LIBRARY_MANAGEMENT_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P1-09 | `HEALTH_RECORDS_SPEC.md` | 🟢 | 2026-06-28 | High, effort L — migrated |
| P1-09b | `HOSTEL_MANAGEMENT_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort L — migrated |
| P1-10 | `PAYROLL_MANAGEMENT_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort L — migrated |
| P1-11 | `ID_CARD_GENERATION_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort S — migrated |
| P1-12 | `HEALTH_RECORDS_SPEC.md` | 🟢 | 2026-06-28 | High, effort L — migrated (same as P1-09) |
| P1-13 | `MULTI_BRANCH_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort L — migrated |
| P1-14 | `EXPENSE_MANAGEMENT_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort L — migrated |
| P1-15 | `INVENTORY_MANAGEMENT_SPEC.md` | 🟢 | 2026-06-28 | Low, effort L — migrated |
| P1-16 | `TRANSPORT_TRACKING_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort L — already migrated |
| P1-17 | `SCHEDULED_ANNOUNCEMENTS_SPEC.md` | 🟢 | 2026-06-28 | Low, effort S — migrated |
| P1-18 | `BROADCAST_GROUPS_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P1-19 | `EXAM_TIMETABLE_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P1-20 | `LESSON_PLANNING_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P1-21 | `ONLINE_ASSIGNMENTS_SPEC.md` | 🟢 | 2026-06-28 | High, effort L, depends on STUDENT_APP — migrated |
| P1-22 | `SCHOLARSHIP_WORKFLOW_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P1-23 | `DOCUMENTS_MANAGEMENT_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P1-24 | `CALENDAR_SYNC_SPEC.md` | 🟢 | 2026-06-28 | Low, effort S — migrated |
| P1-25 | `CURRICULUM_TEMPLATES_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort L, depends on NEP — migrated |
| P1-26 | `EVENT_REGISTRATION_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P1-27 | `VISITOR_MANAGEMENT_SPEC.md` | 🟢 | 2026-06-28 | Low, effort M — migrated |
| P1-28 | `ALUMNI_MANAGEMENT_SPEC.md` | 🟢 | 2026-06-28 | Low, effort L — migrated |
| P1-29 | `FORMS_SURVEYS_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P1-30 | `NEWSLETTER_BUILDER_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P1-31 | `FESTIVAL_CALENDAR_SPEC.md` | 🟢 | 2026-06-28 | Low, effort S — migrated |

## Phase 2 — Differentiate: Unique Features (Priority: High)

| # | Spec File | Status | Migrated Date | Notes |
|---|---|---|---|---|
| P2-01 | `PARENT_PULSE_SPEC.md` | 🟢 | 2026-06-28 | High, effort M — migrated |
| P2-02 | `FAMILY_CIRCLE_SPEC.md` | 🟢 | 2026-06-28 | High, effort M — migrated |
| P2-03 | `CLASS_COMMUNITY_FEED_SPEC.md` | 🟢 | 2026-06-28 | High, effort M — migrated |
| P2-04 | `SMART_NOTIFICATIONS_SPEC.md` | 🟢 | 2026-06-28 | High, effort M — migrated |
| P2-05 | `MULTI_LANGUAGE_SPEC.md` | 🟢 | 2026-06-28 | High, effort M — migrated |
| P2-06 | `MARKETPLACE_SPEC.md` | 🟢 | 2026-06-28 | High, effort L — migrated |
| P2-07 | `PARENT_ANALYTICS_SPEC.md` | 🟢 | 2026-06-28 | High, effort M — migrated |
| P2-08 | `SCHOOL_BRANDING_KIT_SPEC.md` | 🟢 | 2026-06-28 | Low, effort M — migrated |

## Phase 3 — Leapfrog: Blue Ocean Features (Priority: Medium)

| # | Spec File | Status | Migrated Date | Notes |
|---|---|---|---|---|
| P3-01 | `VIDYASETU_AI_TUTOR_SPEC.md` | 🟢 | 2026-06-28 | High, effort L, depends on AI_INFRA + STUDENT_APP — migrated |
| P3-02 | `TEACHER_COPILOT_SPEC.md` | 🟢 | 2026-06-28 | High, effort L, depends on AI_INFRA — migrated |
| P3-03 | `VIDYA_PASSPORT_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort L, depends on STUDENT_APP — migrated |
| P3-04 | `SCHOOL_BENCHMARKING_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M, depends on MULTI_BRANCH — migrated |
| P3-05 | `STUDENT_WELLNESS_SPEC.md` | 🟢 | 2026-06-28 | High, effort L, depends on AI_INFRA + STUDENT_APP — migrated |
| P3-06 | `TRANSPARENCY_DASHBOARD_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P3-07 | `TEACHER_WELLNESS_SPEC.md` | 🟢 | 2026-06-28 | Low, effort M — migrated |
| P3-08 | `FEE_EXPENSE_TRANSPARENCY_SPEC.md` | 🟢 | 2026-06-28 | Low, effort S, depends on EXPENSE_MGMT — migrated |
| P3-09 | `STUDENT_GOALS_SPEC.md` | 🟢 | 2026-06-28 | Low, effort M, depends on STUDENT_APP — migrated |
| P3-10 | `VOICE_PHOTO_CAPTURE_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P3-11 | `SOS_SAFETY_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |

## Phase 4 — Future: Emerging Tech (Priority: Low)

| # | Spec File | Status | Migrated Date | Notes |
|---|---|---|---|---|
| P4-01 | `FACIAL_RECOGNITION_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort L — migrated |
| P4-02 | `LIVE_CLASSES_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort L, depends on STUDENT_APP — migrated |
| P4-03 | `BEHAVIOR_TRACKING_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort M — migrated |
| P4-04 | `STUDENT_PORTFOLIO_SPEC.md` | 🟢 | 2026-06-28 | Medium, effort L, depends on STUDENT_APP — migrated |
| P4-05 | `STUDENT_APP_SPEC.md` | 🔴 | — | High, effort L |
| P4-06 | `VOICE_VIDEO_CALLS_SPEC.md` | 🔴 | — | Medium, effort L |
| P4-07 | `OPEN_API_SPEC.md` | 🔴 | — | Low, effort M |

---

## Migration Checklist (Per Spec)

When migrating a spec, verify all 25 sections are present:

- [ ] 1. Feature Overview (purpose, business value, goals, non-goals, dependencies, related modules)
- [ ] 2. Current System Assessment (code, DB, APIs, UI, services, docs, tech debt, gaps)
- [ ] 3. Functional Requirements (FR-NNN with priority, roles, acceptance notes)
- [ ] 4. User Stories (grouped by role)
- [ ] 5. Business Rules (BR-NNN with enforcement)
- [ ] 6. Database Design (ER, tables, indexes, constraints, FKs, soft delete, audit, migration)
- [ ] 7. State Machines (state diagrams + transition tables)
- [ ] 8. Backend Architecture (repos, services, validators, mappers, permissions, jobs, events, cache, transactions)
- [ ] 9. API Contracts (every endpoint: request, response, errors, auth, rate limit)
- [ ] 10. Frontend Architecture (screens, nav, state, offline, loading, errors, search, pagination)
- [ ] 11. Shared Module / KMP (DTOs, models, repos, usecases, validation, serialization, network APIs, DB models)
- [ ] 12. Permissions Matrix (role × action table)
- [ ] 13. Notifications (N-NNN with trigger, recipient, template, channel, retry, dedup)
- [ ] 14. Background Jobs (schedule, description, error handling)
- [ ] 15. Integrations (external systems, auth, fallback)
- [ ] 16. Security (auth, authz, encryption, audit, PII, DPDP, rate limiting, input validation)
- [ ] 17. Performance & Scalability (scale tables, latency, caching, indexes, batching, pagination)
- [ ] 18. Edge Cases (EC-NNN table)
- [ ] 19. Error Handling (HTTP codes, error format, recovery strategy)
- [ ] 20. Analytics & Reporting (reports, KPIs, dashboards, exports)
- [ ] 21. Testing Strategy (unit, integration, UI, performance, security, offline, migration, regression)
- [ ] 22. Acceptance Criteria (checkbox per FR)
- [ ] 23. Implementation Roadmap (phase table with duration, tasks, deliverables)
- [ ] 24. File-Level Impact Analysis (server, shared, Android, iOS, web, docs, migration, tests)
- [ ] 25. Future Enhancements (out-of-scope items)

Optional sections (recommended for long-lived features):

- [ ] A. Sequence Diagrams
- [ ] B. Domain Model / ER Diagram
- [ ] C. Event Flow
- [ ] D. Configuration
- [ ] E. Migration & Rollback
- [ ] F. Observability

---

## Summary

| Phase | Total | Migrated | In Progress | Remaining |
|---|---|---|---|---|
| Phase 0 | 13 | 13 | 0 | 0 |
| Phase 1 | 31 | 31 | 0 | 0 |
| Phase 2 | 8 | 8 | 0 | 0 |
| Phase 3 | 11 | 11 | 0 | 0 |
| Phase 4 | 7 | 7 | 0 | 0 |
| **Total** | **69** | **69** | **0** | **0** |
