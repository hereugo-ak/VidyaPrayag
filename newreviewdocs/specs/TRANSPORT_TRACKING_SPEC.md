# Transport Tracking — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

GPS bus tracking and transport management: route creation, vehicle/driver management, real-time location tracking, student pickup/drop status, and transport fee integration.

### Goals

- Admin manages routes, vehicles, drivers, and student assignments
- Parent sees real-time bus location on map
- Parent receives notification when bus approaches pickup/drop
- Driver app (or teacher proxy) marks pickup/drop per stop
- Transport fee linked to `FeeRecordsTable`
- Route optimization suggestions

---

## 2. Current System Assessment

- `feature_audit.csv` L117-118: Transport tracking missing (0%)
- `DIFFERENTIATING_FEATURES.md` §6.2: Transport Tracking, effort L
- No transport tables in `Tables.kt`
- Google Maps integration not present (but Ktor Client can call Maps API)

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Admin creates routes with stops (name, lat/lng, sequence, estimated time) |
| FR-2 | Admin assigns vehicles (bus number, capacity, driver) to routes |
| FR-3 | Admin assigns students to routes/stops |
| FR-4 | Real-time GPS tracking: vehicle location updated every 30 seconds |
| FR-5 | Parent views live bus location on map |
| FR-6 | Notification when bus is 5 minutes from pickup/drop |
| FR-7 | Driver marks pickup/drop per student per stop |
| FR-8 | Transport fee auto-created in `FeeRecordsTable` |
| FR-9 | Route history and attendance (who rode which day) |

---

## 4. Database Design

### 4.1 New Tables

```sql
CREATE TABLE transport_routes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,                 -- "Route A - North Zone"
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE transport_stops (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id        UUID NOT NULL REFERENCES transport_routes(id) ON DELETE CASCADE,
    name            TEXT NOT NULL,                 -- "Sector 12 Bus Stop"
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    sequence        INTEGER NOT NULL,              -- order in route
    estimated_time  VARCHAR(8),                    -- "07:15"
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE transport_vehicles (
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

CREATE TABLE transport_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    route_id        UUID NOT NULL REFERENCES transport_routes(id),
    stop_id         UUID NOT NULL REFERENCES transport_stops(id),
    vehicle_id      UUID NOT NULL REFERENCES transport_vehicles(id),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE transport_tracking (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id      UUID NOT NULL REFERENCES transport_vehicles(id),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    speed           REAL,                          -- km/h
    heading         REAL,                          -- degrees
    recorded_at     TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_transport_tracking_vehicle ON transport_tracking(vehicle_id, recorded_at DESC);

CREATE TABLE transport_attendance (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    route_id        UUID NOT NULL,
    date            DATE NOT NULL,
    pickup_status   VARCHAR(16),                   -- picked | missed | absent
    drop_status     VARCHAR(16),                   -- dropped | missed
    pickup_time     TIMESTAMP,
    drop_time       TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, student_id, date)
);
```

---

## 5. Backend Architecture

### 5.1 Services

```kotlin
class TransportRouteService { /* CRUD routes, stops */ }
class TransportVehicleService { /* CRUD vehicles, assign to routes */ }
class TransportAssignmentService { /* assign students to routes */ }
class TransportTrackingService {
    suspend fun updateLocation(vehicleId: UUID, lat: Double, lng: Double, speed: Float, heading: Float)
    suspend fun getLiveLocation(vehicleId: UUID): TransportTrackingDto
    suspend fun getRouteProgress(routeId: UUID): RouteProgressDto  // current stop, next stop, ETA
}
class TransportAttendanceService {
    suspend fun markPickup(studentId: UUID, routeId: UUID, date: LocalDate)
    suspend fun markDrop(studentId: UUID, routeId: UUID, date: LocalDate)
    suspend fun getDailyAttendance(routeId: UUID, date: LocalDate): List<TransportAttendanceDto>
}
```

### 5.2 Geofence Notification

When vehicle GPS is within 500m of a stop, trigger notification to parents assigned to that stop:
- "Bus arriving at {stop_name} in ~5 minutes"

---

## 6. API Contracts

```
# Admin
GET/POST /api/v1/school/transport/routes
GET/POST /api/v1/school/transport/vehicles
POST /api/v1/school/transport/assignments
GET /api/v1/school/transport/attendance?route_id={uuid}&date={YYYY-MM-DD}

# Driver (or teacher proxy)
POST /api/v1/transport/location  { vehicle_id, lat, lng, speed, heading }
POST /api/v1/transport/pickup    { student_id, route_id }
POST /api/v1/transport/drop      { student_id, route_id }

# Parent
GET /api/v1/parent/transport/live-location/{childId}
GET /api/v1/parent/transport/route/{childId}
```

---

## 7. Frontend Architecture

- Parent: Map view (Google Maps SDK or OpenStreetMap) with bus marker + route polyline + stop markers
- Admin: Route management, vehicle assignment, student assignment, attendance dashboard
- Driver: Simple list of stops with pickup/drop buttons per student

### Map Integration

- **Android:** Google Maps Compose (`com.google.maps.android:maps-compose`)
- **iOS:** Google Maps SDK for iOS
- **Web:** Google Maps JS API or Leaflet (OpenStreetMap, no API key needed)

---

## 8. Acceptance Criteria

- [ ] Admin can create routes with stops and assign vehicles
- [ ] Students can be assigned to routes/stops
- [ ] Vehicle location updates in real-time (≤30s delay)
- [ ] Parent sees live bus location on map
- [ ] Notification sent when bus approaches stop
- [ ] Driver can mark pickup/drop per student
- [ ] Transport fee auto-created in fee_records
- [ ] Daily transport attendance report available

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables |
| 2 | 3 days | Route/Vehicle/Assignment services + API |
| 3 | 2 days | Tracking service + geofence notifications |
| 4 | 2 days | Transport attendance service |
| 5 | 3 days | Client UI (admin route management, parent map view, driver attendance) |
| 6 | 2 days | Map SDK integration (Android + iOS) |
| 7 | 2 days | Tests |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 6 new transport tables |
| `server/.../feature/transport/*.kt` | New | All transport services + routing |
| `docs/db/migration_045_transport.sql` | New | DDL |
| `shared/.../feature/transport/TransportApi.kt` | New | Client API |
| `composeApp/.../ui/v2/screens/parent/BusTrackingScreen.kt` | New | Parent map view |
| `composeApp/.../ui/v2/screens/admin/TransportManagementScreen.kt` | New | Admin management |
| `composeApp/.../ui/v2/screens/teacher/TransportAttendanceScreen.kt` | New | Driver/teacher attendance |
