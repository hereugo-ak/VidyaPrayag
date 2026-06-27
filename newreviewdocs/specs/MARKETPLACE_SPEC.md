# School Marketplace — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §9.1

---

## 1. Feature Overview

An in-app marketplace where schools can list and sell items (uniforms, books, stationery, merchandise) and parents can browse and purchase online. Includes order management, payment integration (Razorpay), and pickup/delivery options.

### Goals

- School admin lists products (uniforms, books, stationery, event tickets, merchandise)
- Parent browses products by category, adds to cart, checks out
- Payment via Razorpay (reuses `FEE_PAYMENT_SPEC.md` payment infrastructure)
- Order management: admin processes orders (confirmed → packed → ready → delivered)
- Pickup at school or delivery (configurable)
- Order history for parents

---

## 2. Current System Assessment

- `FEE_PAYMENT_SPEC.md` — Razorpay integration for fees (reusable payment infrastructure)
- Supabase Storage — for product images
- No marketplace/e-commerce tables exist
- `DIFFERENTIATING_FEATURES.md` §9.1: Marketplace, effort L

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Admin lists products: name, description, category, price, images, stock, variants (size/color) |
| FR-2 | Parent browses by category, searches, views product details |
| FR-3 | Cart: add/remove items, quantity selection |
| FR-4 | Checkout: Razorpay payment, pickup or delivery option |
| FR-5 | Order status: placed → confirmed → packed → ready_for_pickup/delivered → completed |
| FR-6 | Admin order management dashboard |
| FR-7 | Order history for parents |
| FR-8 | Low stock alerts for admin |
| FR-9 | Optional: digital products (event tickets, e-books) with QR code delivery |

---

## 4. Database Design

```sql
CREATE TABLE marketplace_products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,
    description     TEXT,
    category        VARCHAR(32) NOT NULL,          -- uniform | books | stationery | merchandise | tickets | digital
    price           DOUBLE PRECISION NOT NULL,
    compare_at_price DOUBLE PRECISION,             -- original price (for discounts)
    images          TEXT NOT NULL DEFAULT '[]',    -- JSON array of Supabase URLs
    stock           INTEGER NOT NULL DEFAULT 0,
    is_digital      BOOLEAN NOT NULL DEFAULT false,
    variants        TEXT,                          -- JSON: [{"name": "Size", "options": ["S", "M", "L", "XL"]}]
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_marketplace_products_school ON marketplace_products(school_id, category, is_active);

CREATE TABLE marketplace_orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    order_number    VARCHAR(16) NOT NULL UNIQUE,   -- "ORD-2026-0001"
    parent_id       UUID NOT NULL,
    parent_name     TEXT NOT NULL,
    student_id      UUID,
    student_name    TEXT,
    items           TEXT NOT NULL,                 -- JSON: [{"product_id": "...", "name": "...", "price": 500, "quantity": 2, "variant": "M"}]
    total_amount    DOUBLE PRECISION NOT NULL,
    payment_id      TEXT,                          -- Razorpay payment ID
    payment_status  VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending | paid | failed | refunded
    fulfillment_type VARCHAR(16) NOT NULL DEFAULT 'pickup', -- pickup | delivery
    delivery_address TEXT,                         -- if delivery
    status          VARCHAR(16) NOT NULL DEFAULT 'placed', -- placed | confirmed | packed | ready | delivered | completed | cancelled
    placed_at       TIMESTAMP NOT NULL DEFAULT now(),
    confirmed_at    TIMESTAMP,
    delivered_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_marketplace_orders_school ON marketplace_orders(school_id, status, placed_at DESC);
CREATE INDEX idx_marketplace_orders_parent ON marketplace_orders(parent_id, placed_at DESC);
```

---

## 5. Backend Architecture

### 5.1 Services

```kotlin
class ProductService { /* CRUD products, stock management, low stock alerts */ }
class CartService { /* in-memory or client-side cart */ }
class OrderService {
    suspend fun placeOrder(parentId: UUID, items: List<CartItem>, fulfillment: String): OrderDto
    suspend fun updateOrderStatus(orderId: UUID, status: String)
    suspend fun getOrderHistory(parentId: UUID): List<OrderDto>
    suspend fun getSchoolOrders(schoolId: UUID, status: String?): PaginatedResult<OrderDto>
}
```

### 5.2 Payment Integration

Reuses Razorpay from `FEE_PAYMENT_SPEC.md`:
1. Create Razorpay order for marketplace total
2. Parent pays via Razorpay checkout
3. Webhook confirms payment → order status = confirmed
4. If payment fails → order status = cancelled

---

## 6. API Contracts

```
# Parent
GET /api/v1/parent/marketplace/products?category={category}&search={query}
GET /api/v1/parent/marketplace/products/{id}
POST /api/v1/parent/marketplace/orders  { items, fulfillment_type, delivery_address }
GET /api/v1/parent/marketplace/orders

# Admin
GET/POST /api/v1/school/marketplace/products
PATCH /api/v1/school/marketplace/products/{id}
GET /api/v1/school/marketplace/orders?status={status}
POST /api/v1/school/marketplace/orders/{id}/status  { status }
```

---

## 7. Acceptance Criteria

- [ ] Admin lists products with images, variants, stock
- [ ] Parent browses, searches, views product details
- [ ] Cart and checkout work with Razorpay payment
- [ ] Order status tracked (placed → confirmed → packed → ready → delivered)
- [ ] Admin order management dashboard
- [ ] Parent order history
- [ ] Low stock alerts
- [ ] Pickup and delivery options

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables |
| 2 | 2 days | ProductService + OrderService |
| 3 | 2 days | Razorpay payment integration (reuse from FEE_PAYMENT_SPEC) |
| 4 | 2 days | API endpoints |
| 5 | 4 days | Client UI (product browse, cart, checkout, order history, admin dashboard) |
| 6 | 2 days | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 2 marketplace tables |
| `server/.../feature/marketplace/*.kt` | New | Services + routing |
| `docs/db/migration_071_marketplace.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/parent/MarketplaceScreen.kt` | New | Product browse |
| `composeApp/.../ui/v2/screens/parent/CartScreen.kt` | New | Cart + checkout |
| `composeApp/.../ui/v2/screens/admin/MarketplaceAdminScreen.kt` | New | Product + order management |
