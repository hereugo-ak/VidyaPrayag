-- =============================================================================
-- Migration 053 — Transport Tracking
--                (GPS bus tracking, route/vehicle/driver management,
--                 student pickup/drop, transport fee integration)
--
-- WHY THIS EXISTS
--   TRANSPORT_TRACKING_SPEC.md — K-12 transport management: admin creates
--   routes with stops, assigns vehicles/drivers, assigns students to
--   routes/stops, real-time GPS tracking (30s interval), parent live map
--   view, geofence notifications when bus approaches a stop, driver marks
--   pickup/drop per student, transport fee auto-created in fee_records,
--   daily transport attendance report.
--
--   This creates 6 tables:
--     transport_routes      — route definitions (name, description, active)
--     transport_stops       — ordered stops per route (lat/lng, sequence, ETA)
--     transport_vehicles    — buses assigned to routes (capacity, driver info)
--     transport_assignments — student-to-route/stop/vehicle mapping
--     transport_tracking    — real-time GPS pings (lat/lng/speed/heading)
--     transport_attendance  — daily pickup/drop status per student per route
--
--   The Exposed mappings (TransportRoutesTable, TransportStopsTable, etc. in
--   server/.../db/Tables.kt) reference these tables. AUTO_CREATE_TABLES is
--   OFF in production and validateSchema() gates boot on table presence, so
--   this file MUST be applied in Supabase BEFORE the matching backend deploy.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every CREATE TABLE uses IF NOT EXISTS.
--
-- RUN ORDER (appended to the existing chain):
--   ...
--   52. docs/db/migration_052_alumni_management.sql
--   53. docs/db/migration_053_transport_tracking.sql       <-- this file
--
-- Column names/types are kept in lock-step with
-- server/.../db/Tables.kt (the Exposed mappings land in the same commit).
-- =============================================================================

BEGIN;

-- ── transport_routes ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transport_routes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_transport_routes_school
    ON transport_routes (school_id, is_active);

-- ── transport_stops ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transport_stops (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id        UUID NOT NULL REFERENCES transport_routes(id) ON DELETE CASCADE,
    name            TEXT NOT NULL,
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    sequence        INTEGER NOT NULL,
    estimated_time  VARCHAR(8),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_transport_stops_route
    ON transport_stops (route_id, sequence);

-- ── transport_vehicles ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transport_vehicles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    route_id        UUID REFERENCES transport_routes(id),
    bus_number      TEXT NOT NULL,
    capacity        INTEGER NOT NULL DEFAULT 40,
    driver_name     TEXT,
    driver_phone    VARCHAR(32),
    driver_license  TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_transport_vehicles_school
    ON transport_vehicles (school_id, is_active);

-- ── transport_assignments ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transport_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    route_id        UUID NOT NULL REFERENCES transport_routes(id),
    stop_id         UUID NOT NULL REFERENCES transport_stops(id),
    vehicle_id      UUID NOT NULL REFERENCES transport_vehicles(id),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_transport_assignments_school
    ON transport_assignments (school_id, is_active);

CREATE INDEX IF NOT EXISTS idx_transport_assignments_student
    ON transport_assignments (student_id, is_active);

CREATE INDEX IF NOT EXISTS idx_transport_assignments_route
    ON transport_assignments (route_id, is_active);

-- ── transport_tracking ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transport_tracking (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id      UUID NOT NULL REFERENCES transport_vehicles(id) ON DELETE CASCADE,
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    speed           REAL,
    heading         REAL,
    recorded_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_transport_tracking_vehicle
    ON transport_tracking (vehicle_id, recorded_at DESC);

-- ── transport_attendance ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transport_attendance (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    route_id        UUID NOT NULL,
    date            DATE NOT NULL,
    pickup_status   VARCHAR(16),
    drop_status     VARCHAR(16),
    pickup_time     TIMESTAMP,
    drop_time       TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, student_id, date)
);

CREATE INDEX IF NOT EXISTS idx_transport_attendance_route_date
    ON transport_attendance (route_id, date);

CREATE INDEX IF NOT EXISTS idx_transport_attendance_school_date
    ON transport_attendance (school_id, date);

COMMIT;
