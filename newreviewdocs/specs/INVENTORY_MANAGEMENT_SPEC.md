# Inventory Management — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

School inventory and asset tracking: equipment, furniture, consumables, lab supplies, sports equipment with stock levels, assignments, and depreciation tracking.

### Goals

- Track assets (furniture, computers, lab equipment, sports gear) with assignment to rooms/persons
- Track consumables (stationery, lab supplies) with stock levels and reorder alerts
- Asset depreciation tracking
- Stock-in/stock-out log
- Low stock alerts

---

## 2. Current System Assessment

- `feature_audit.csv` L139: Inventory missing (0%)
- No inventory tables in `Tables.kt`

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Asset tracking: item name, category, quantity, condition, assigned to (room/person), purchase date, cost |
| FR-2 | Consumable tracking: item name, category, current stock, min stock, unit, reorder level |
| FR-3 | Stock movements: stock-in (purchase), stock-out (issue), with date and person |
| FR-4 | Low stock alert when stock < reorder level |
| FR-5 | Asset depreciation: straight-line method, annual depreciation report |
| FR-6 | Search and filter by category, condition, location |

---

## 4. Database Design

```sql
CREATE TABLE inventory_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,
    category        VARCHAR(32) NOT NULL,          -- furniture | electronics | lab | sports | stationery | consumable
    item_type       VARCHAR(16) NOT NULL,          -- asset | consumable
    unit            VARCHAR(16) DEFAULT 'each',    -- each | kg | box | ream
    current_stock   INTEGER NOT NULL DEFAULT 0,
    min_stock       INTEGER NOT NULL DEFAULT 0,
    reorder_level   INTEGER NOT NULL DEFAULT 0,
    condition       VARCHAR(16),                   -- new | good | fair | damaged
    location        TEXT,                          -- room/department
    assigned_to     TEXT,                          -- person name
    purchase_date   DATE,
    purchase_cost   DOUBLE PRECISION,
    useful_life_years INTEGER,                     -- for depreciation
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_inventory_school_category ON inventory_items(school_id, category);

CREATE TABLE inventory_movements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    item_id         UUID NOT NULL REFERENCES inventory_items(id),
    movement_type   VARCHAR(8) NOT NULL,           -- IN | OUT
    quantity        INTEGER NOT NULL,
    reason          TEXT,                          -- "Purchase", "Issued to Lab 3", "Damaged"
    person_name     TEXT,
    date            DATE NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_inventory_movements_item ON inventory_movements(item_id, date DESC);
```

---

## 5. API Contracts

```
GET/POST /api/v1/school/inventory/items
PATCH /api/v1/school/inventory/items/{id}
POST /api/v1/school/inventory/movements  { item_id, movement_type, quantity, reason }
GET /api/v1/school/inventory/low-stock
GET /api/v1/school/inventory/depreciation-report?academic_year_id={uuid}
```

---

## 6. Acceptance Criteria

- [ ] Assets and consumables tracked
- [ ] Stock-in/stock-out logged
- [ ] Low stock alerts triggered
- [ ] Asset depreciation calculated
- [ ] Search and filter works
- [ ] Assignment to rooms/persons tracked

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables, services |
| 2 | 2 days | API endpoints + low stock alerts |
| 3 | 3 days | Client UI (item list, stock movement, low stock dashboard) |
| 4 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 2 inventory tables |
| `server/.../feature/inventory/*.kt` | New | Services + routing |
| `docs/db/migration_053_inventory.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/InventoryScreen.kt` | New | Inventory management |
