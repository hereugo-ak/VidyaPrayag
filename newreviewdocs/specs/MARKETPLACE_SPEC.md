# School Marketplace — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §9.1
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

An in-app marketplace where schools can list and sell items (uniforms, books, stationery, merchandise) and parents can browse and purchase online. Includes order management, payment integration (Razorpay), and pickup/delivery options.

### Why — Product Rationale

Schools sell uniforms, books, stationery, and event tickets separately from fee collection. Currently, this is done manually (cash, physical shop). An in-app marketplace streamlines the process: parents browse and pay online, schools manage orders digitally. This creates a new revenue stream for schools and convenience for parents.

This is a **differentiating feature** (Priority P1, Phase 2, effort L, "High" value per `DIFFERENTIATING_FEATURES.md`). It extends the school ERP beyond fees into general commerce.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §9.1:
> "Marketplace — schools sell uniforms, books, stationery, event tickets; parents buy online with pickup/delivery."

No major school ERP includes an in-app marketplace. Schools use separate e-commerce platforms or manual processes. Integrating marketplace into the school app creates a one-stop solution.

### Goals

- School admin lists products (uniforms, books, stationery, event tickets, merchandise)
- Parent browses products by category, adds to cart, checks out
- Payment via Razorpay (reuses `FEE_PAYMENT_SPEC.md` payment infrastructure)
- Order management: admin processes orders (confirmed → packed → ready → delivered)
- Pickup at school or delivery (configurable)
- Order history for parents

### Non-goals

- [ ] Multi-school marketplace (each school has its own store)
- [ ] Third-party seller marketplace (only school admin lists products)
- [ ] Subscription products
- [ ] International shipping
- [ ] Inventory management with suppliers
- [ ] Return/refund workflow (manual via admin)
- [ ] Product reviews and ratings
- [ ] Wishlist

### Dependencies

- `FEE_PAYMENT_SPEC.md` — Razorpay payment integration (reusable)
- Supabase Storage — for product images
- `NotificationsTable` — for order status notifications
- `Notify.kt` — for sending order updates

### Related Modules

- `server/.../feature/marketplace/` — new marketplace module
- `server/.../feature/payments/` — existing Razorpay payment infrastructure
- `composeApp/.../ui/v2/screens/parent/` — parent marketplace UI
- `composeApp/.../ui/v2/screens/admin/` — admin marketplace UI

---

## 2. Current System Assessment

### Existing Code

- `FEE_PAYMENT_SPEC.md` — Razorpay integration for fees (reusable payment infrastructure)
- Supabase Storage — for product images
- No marketplace/e-commerce tables exist
- `DIFFERENTIATING_FEATURES.md` §9.1: Marketplace, effort L

### Existing Database

- No marketplace tables exist
- Payment infrastructure: `payments` table (from `FEE_PAYMENT_SPEC.md`)
- Supabase Storage: existing bucket for file uploads

### Existing APIs

- `FEE_PAYMENT_SPEC.md` — Razorpay order creation, payment verification, webhook handling
- No marketplace API endpoints exist

### Existing UI

- No marketplace UI exists
- Payment checkout UI exists (from fee payment)

### Existing Services

- `PaymentService` — Razorpay integration (reusable)
- `Notify.kt` — notification dispatch (reusable for order updates)

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §9.1 — Marketplace
- `FEE_PAYMENT_SPEC.md` — Payment infrastructure (prerequisite)

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No marketplace tables | No `marketplace_products` or `marketplace_orders` tables |
| TD-2 | No product management | No CRUD for products |
| TD-3 | No order management | No order placement, status tracking, or history |
| TD-4 | No cart | No cart service or UI |
| TD-5 | No marketplace UI | No browse, search, checkout, or admin dashboard screens |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No product listing | Schools cannot sell items online | **High** |
| G2 | No order management | No digital order tracking | **High** |
| G3 | No cart/checkout | Parents cannot purchase online | **High** |
| G4 | No admin dashboard | Schools cannot manage orders | **High** |
| G5 | No order history | Parents cannot track past purchases | **Medium** |
| G6 | No low stock alerts | Schools may run out of stock unknowingly | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Product Listing |
| **Description** | Admin lists products: name, description, category, price, images, stock, variants (size/color). |
| **Priority** | Critical |
| **User Roles** | School Admin |
| **Acceptance notes** | Product has name, description, category (uniform/books/stationery/merchandise/tickets/digital), price, compare_at_price (optional), images (Supabase URLs), stock, is_digital flag, variants (JSON). |

### FR-002
| Field | Value |
|---|---|
| **Title** | Product Browsing |
| **Description** | Parent browses by category, searches, views product details. |
| **Priority** | Critical |
| **User Roles** | Parent |
| **Acceptance notes** | Category filter, search by name, product detail page with images, variants, price, stock status. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Cart Management |
| **Description** | Cart: add/remove items, quantity selection. |
| **Priority** | Critical |
| **User Roles** | Parent |
| **Acceptance notes** | Client-side cart (in-memory or DataStore). Add/remove items, change quantity, variant selection. Cart total calculated. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Checkout with Razorpay |
| **Description** | Checkout: Razorpay payment, pickup or delivery option. |
| **Priority** | Critical |
| **User Roles** | Parent |
| **Acceptance notes** | Razorpay order created for cart total. Parent pays via Razorpay checkout. Pickup at school or delivery (with address). Webhook confirms payment. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Order Status Tracking |
| **Description** | Order status: placed → confirmed → packed → ready_for_pickup/delivered → completed. |
| **Priority** | High |
| **User Roles** | School Admin, Parent |
| **Acceptance notes** | Admin updates order status. Parent sees status in order history. Notifications sent on status change. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Admin Order Management |
| **Description** | Admin order management dashboard. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | View all orders, filter by status, update status, view order details (items, parent, student, payment). |

### FR-007
| Field | Value |
|---|---|
| **Title** | Order History |
| **Description** | Order history for parents. |
| **Priority** | Medium |
| **User Roles** | Parent |
| **Acceptance notes** | Paginated list of past orders with status, items, total, date. Order detail view. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Low Stock Alerts |
| **Description** | Low stock alerts for admin. |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | When stock falls below threshold (default 5), admin notified. Configurable per product. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Digital Products |
| **Description** | Optional: digital products (event tickets, e-books) with QR code delivery. |
| **Priority** | Low |
| **User Roles** | School Admin, Parent |
| **Acceptance notes** | `is_digital = true` products. No shipping. QR code generated for tickets. Delivered via notification + in-app. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Product list loads in < 1 second |
| NFR-2 | Order placement completes in < 5 seconds (including Razorpay) |
| NFR-3 | Admin dashboard loads orders in < 2 seconds |
| NFR-4 | Product images optimized (max 500KB per image, WebP format) |
| NFR-5 | Cart persists across app sessions (DataStore) |
| NFR-6 | Order number format: "ORD-YYYY-NNNN" (auto-increment per school per year) |

---

## 4. User Stories

### School Admin
- [ ] List a new product with name, description, category, price, images, stock, variants
- [ ] Edit or deactivate a product
- [ ] View all orders with filter by status
- [ ] Update order status (confirm, pack, ready, deliver, complete)
- [ ] Receive low stock alerts
- [ ] View order details (items, parent, student, payment status)

### Parent
- [ ] Browse products by category
- [ ] Search products by name
- [ ] View product details (images, price, variants, stock)
- [ ] Add products to cart with quantity and variant selection
- [ ] Remove items from cart
- [ ] Checkout with Razorpay payment
- [ ] Choose pickup or delivery option
- [ ] View order history
- [ ] Track order status
- [ ] Receive notifications on order status change

### System
- [ ] Generate unique order numbers (ORD-YYYY-NNNN)
- [ ] Process Razorpay payment and verify via webhook
- [ ] Send notifications on order status change
- [ ] Alert admin when stock falls below threshold
- [ ] Generate QR codes for digital products

---

## 5. Business Rules

### BR-001
**Rule:** Each school has its own marketplace.
**Enforcement:** `marketplace_products.school_id` and `marketplace_orders.school_id` — all queries scoped by school.

### BR-002
**Rule:** Order number format: "ORD-YYYY-NNNN".
**Enforcement:** Auto-generated, unique per school per year. Sequential numbering. e.g., "ORD-2026-0001".

### BR-003
**Rule:** Payment required before order confirmation.
**Enforcement:** Order status starts as 'placed' with `payment_status = 'pending'`. Razorpay webhook confirms payment → `payment_status = 'paid'`, `order.status = 'confirmed'`. Payment failure → `order.status = 'cancelled'`.

### BR-004
**Rule:** Stock decremented on order confirmation.
**Enforcement:** Stock decremented when order confirmed (not when placed). If payment fails, stock unchanged. Prevents overselling on payment failure.

### BR-005
**Rule:** Low stock threshold default 5.
**Enforcement:** When `stock < 5` (configurable per product), admin notified. Notification: "Low stock: [product name] has [N] units left."

### BR-006
**Rule:** Digital products have no shipping.
**Enforcement:** `is_digital = true` → `fulfillment_type = 'digital'`. No pickup/delivery option. QR code generated and delivered via notification + in-app.

### BR-007
**Rule:** Inactive products not visible to parents.
**Enforcement:** `is_active = false` → product hidden from parent browse. Admin can still view and reactivate.

### BR-008
**Rule:** Cart is client-side.
**Enforcement:** Cart stored in DataStore (local). No server-side cart table. Cart sent to server only at checkout.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Two new tables: `marketplace_products` (product catalog per school) and `marketplace_orders` (order records with items, payment, fulfillment). No modifications to existing tables.

### 6.2 New Tables

#### `marketplace_products` table

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
```

#### `marketplace_orders` table

```sql
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
    fulfillment_type VARCHAR(16) NOT NULL DEFAULT 'pickup', -- pickup | delivery | digital
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

### 6.3 Modified Tables

N/A — no existing tables modified.

### 6.4 Indexes

- `marketplace_products(school_id, category, is_active)` — for parent browse queries
- `marketplace_orders(school_id, status, placed_at DESC)` — for admin dashboard
- `marketplace_orders(parent_id, placed_at DESC)` — for parent order history
- `marketplace_orders(order_number)` — UNIQUE, for order number lookup

### 6.5 Constraints

- `marketplace_products.school_id` — NOT NULL
- `marketplace_products.name` — NOT NULL
- `marketplace_products.category` — NOT NULL, VARCHAR(32)
- `marketplace_products.price` — NOT NULL, DOUBLE PRECISION
- `marketplace_products.stock` — NOT NULL, default 0
- `marketplace_products.is_digital` — NOT NULL, default false
- `marketplace_products.is_active` — NOT NULL, default true
- `marketplace_orders.order_number` — NOT NULL, UNIQUE
- `marketplace_orders.school_id` — NOT NULL
- `marketplace_orders.parent_id` — NOT NULL
- `marketplace_orders.parent_name` — NOT NULL
- `marketplace_orders.items` — NOT NULL (JSON array)
- `marketplace_orders.total_amount` — NOT NULL
- `marketplace_orders.payment_status` — NOT NULL, default 'pending'
- `marketplace_orders.fulfillment_type` — NOT NULL, default 'pickup'
- `marketplace_orders.status` — NOT NULL, default 'placed'

### 6.6 Foreign Keys

- `marketplace_products.school_id` → `schools.id` (implicit)
- `marketplace_orders.school_id` → `schools.id` (implicit)
- `marketplace_orders.parent_id` → `app_users.id` (implicit)
- `marketplace_orders.student_id` → `students.id` (implicit, nullable)

### 6.7 Soft Delete Strategy

Products: `is_active = false` (soft delete — hidden from parents, visible to admin).
Orders: no soft delete — orders are permanent records.

### 6.8 Audit Fields

- `marketplace_products`: `created_at`, `updated_at`
- `marketplace_orders`: `placed_at`, `confirmed_at`, `delivered_at`, `created_at`

### 6.9 Migration Notes

Migration: `docs/db/migration_071_marketplace.sql`
- CREATE `marketplace_products` table
- CREATE `marketplace_orders` table
- CREATE indexes
- No data migration (new feature)

### 6.10 Exposed Mappings

```kotlin
object MarketplaceProductsTable : UUIDTable("marketplace_products", "id") {
    val schoolId        = uuid("school_id")
    val name            = text("name")
    val description     = text("description").nullable()
    val category        = varchar("category", 32)
    val price           = double("price")
    val compareAtPrice  = double("compare_at_price").nullable()
    val images          = text("images").default("[]")  // JSON array
    val stock           = integer("stock").default(0)
    val isDigital       = bool("is_digital").default(false)
    val variants        = text("variants").nullable()  // JSON
    val isActive        = bool("is_active").default(true)
    val createdAt       = timestamp("created_at")
    val updatedAt       = timestamp("updated_at")
}

object MarketplaceOrdersTable : UUIDTable("marketplace_orders", "id") {
    val schoolId        = uuid("school_id")
    val orderNumber     = varchar("order_number", 16).unique()
    val parentId        = uuid("parent_id")
    val parentName      = text("parent_name")
    val studentId       = uuid("student_id").nullable()
    val studentName     = text("student_name").nullable()
    val items           = text("items")  // JSON array
    val totalAmount     = double("total_amount")
    val paymentId       = text("payment_id").nullable()
    val paymentStatus   = varchar("payment_status", 16).default("pending")
    val fulfillmentType = varchar("fulfillment_type", 16).default("pickup")
    val deliveryAddress = text("delivery_address").nullable()
    val status          = varchar("status", 16).default("placed")
    val placedAt        = timestamp("placed_at")
    val confirmedAt     = timestamp("confirmed_at").nullable()
    val deliveredAt     = timestamp("delivered_at").nullable()
    val createdAt       = timestamp("created_at")
}
```

Register both tables in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — products created by school admin.

---

## 7. State Machines

### Order State Machine

```
placed ──payment_confirmed──> confirmed ──packed──> packed ──ready──> ready ──delivered──> delivered ──completed──> completed
  │                                │
  │──payment_failed──> cancelled   │──cancelled──> cancelled
  │
  └──cancelled──> cancelled
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `placed` | Razorpay payment confirmed (webhook) | `confirmed` | `payment_status = 'paid'` |
| `placed` | Razorpay payment failed (webhook) | `cancelled` | `payment_status = 'failed'` |
| `placed` | Admin cancels | `cancelled` | Admin action |
| `confirmed` | Admin marks packed | `packed` | Admin action |
| `packed` | Admin marks ready | `ready` | Admin action (pickup) |
| `packed` | Admin marks delivered | `delivered` | Admin action (delivery) |
| `ready` | Parent picks up / Admin marks delivered | `delivered` | Admin action |
| `delivered` | Auto-complete (24h) or admin marks | `completed` | Timer or admin action |
| `confirmed` | Admin cancels | `cancelled` | Admin action (refund) |
| `packed` | Admin cancels | `cancelled` | Admin action (refund) |

### Payment State Machine

```
pending ──razorpay_success──> paid ──refund──> refunded
  │
  └──razorpay_failure──> failed
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `pending` | Razorpay webhook: payment.captured | `paid` | Payment verified |
| `pending` | Razorpay webhook: payment.failed | `failed` | Payment failed |
| `paid` | Admin initiates refund | `refunded` | Razorpay refund processed |

### Product Stock State Machine

```
in_stock ──stock_decremented──> low_stock ──stock_depleted──> out_of_stock
  │                                  │
  │──admin_restocks──>               │──admin_restocks──> in_stock
  │  in_stock                        │
  └──stock_decremented──>            └──admin_restocks──> in_stock
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `in_stock` | Order confirmed (stock decremented) | `in_stock` or `low_stock` | stock > threshold → in_stock; stock ≤ threshold → low_stock |
| `low_stock` | Order confirmed (stock decremented) | `low_stock` or `out_of_stock` | stock > 0 → low_stock; stock = 0 → out_of_stock |
| `out_of_stock` | Admin restocks | `in_stock` | stock > threshold |
| `low_stock` | Admin restocks | `in_stock` | stock > threshold |

---

## 8. Backend Architecture

### 8.1 Component Overview

`ProductService` handles product CRUD and stock management. `OrderService` handles order placement, status updates, and history. `CartService` is client-side (no server component). Payment reuses existing Razorpay integration from `FEE_PAYMENT_SPEC.md`.

### 8.2 Design Principles

1. **Reuse payment infrastructure** — Razorpay integration from `FEE_PAYMENT_SPEC.md` reused, not duplicated
2. **Cart is client-side** — no server-side cart table; cart sent to server only at checkout
3. **Stock on confirmation** — stock decremented when order confirmed (payment success), not when placed
4. **School-scoped** — each school has its own marketplace; all queries filtered by `school_id`
5. **Order numbers auto-generated** — "ORD-YYYY-NNNN" format, unique per school per year

### 8.3 Core Types

#### ProductService

```kotlin
class ProductService {
    suspend fun createProduct(schoolId: UUID, request: CreateProductRequest): ProductDto
    suspend fun updateProduct(productId: UUID, request: UpdateProductRequest): ProductDto
    suspend fun deactivateProduct(productId: UUID)
    suspend fun getProducts(schoolId: UUID, category: String?, search: String?): List<ProductDto>
    suspend fun getProduct(productId: UUID): ProductDto
    suspend fun checkLowStock(schoolId: UUID): List<ProductDto>  // returns products below threshold
}
```

#### OrderService

```kotlin
class OrderService {
    suspend fun placeOrder(parentId: UUID, items: List<CartItem>, fulfillment: String): OrderDto
    suspend fun updateOrderStatus(orderId: UUID, status: String)
    suspend fun getOrderHistory(parentId: UUID): List<OrderDto>
    suspend fun getSchoolOrders(schoolId: UUID, status: String?): PaginatedResult<OrderDto>
    suspend fun confirmPayment(orderId: UUID, razorpayPaymentId: String)  // called by webhook
}
```

### 8.4 Repositories

- `ProductRepository` — CRUD for `marketplace_products`
- `OrderRepository` — CRUD for `marketplace_orders`

### 8.5 Mappers

- `ProductMapper` — maps `marketplace_products` rows to `ProductDto`
- `OrderMapper` — maps `marketplace_orders` rows to `OrderDto`

### 8.6 Permission Checks

- Product management: School Admin only
- Order placement: Parent only
- Order status update: School Admin only
- Order history: Parent (own orders only), School Admin (school orders)

### 8.7 Background Jobs

- **Low Stock Check** — daily
  1. Query `marketplace_products` where `stock < 5` (default threshold) AND `is_active = true`
  2. Send notification to school admin for each low-stock product
  3. Return count

- **Order Auto-Complete** — hourly
  1. Query `marketplace_orders` where `status = 'delivered'` AND `delivered_at < now() - 24 hours`
  2. Update status to 'completed'
  3. Return count

### 8.8 Domain Events

- `OrderPlaced` — emitted when order placed (before payment)
- `PaymentConfirmed` — emitted when Razorpay webhook confirms payment
- `OrderStatusChanged` — emitted when admin updates order status
- `LowStockAlert` — emitted when product stock falls below threshold
- `OrderCancelled` — emitted when order cancelled (payment failure or admin action)

### 8.9 Caching

- Product list: cached per school per category, 5-minute TTL
- Product detail: cached per product, 5-minute TTL
- Order history: not cached (real-time)

### 8.10 Transactions

- Order placement: single transaction (create order, generate order number)
- Payment confirmation: single transaction (update payment_status, update order status, decrement stock)
- Order status update: single transaction (update status, update timestamp)
- Product update: single transaction (update product fields)

### 8.11 Rate Limiting

- Product browse: standard API rate limiting
- Order placement: 10 orders per parent per day
- Product creation: 100 products per school per day

### 8.12 Configuration

- `MARKETPLACE_ENABLED` — default `true`; enable/disable feature
- `MARKETPLACE_LOW_STOCK_THRESHOLD` — default `5`; stock level for low stock alert
- `MARKETPLACE_ORDER_AUTO_COMPLETE_HOURS` — default `24`; hours after delivery to auto-complete
- `MARKETPLACE_MAX_IMAGES_PER_PRODUCT` — default `8`; max product images
- `MARKETPLACE_MAX_IMAGE_SIZE_KB` — default `500`; max image size
- `MARKETPLACE_ORDER_RATE_LIMIT_PER_DAY` — default `10`; max orders per parent per day

---

## 9. API Contracts

### 9.1 Parent Endpoints

```
GET /api/v1/parent/marketplace/products?category={category}&search={query}
  → 200: { products: [ProductDto], total: Int }

GET /api/v1/parent/marketplace/products/{id}
  → 200: ProductDto
  → 404: Product not found

POST /api/v1/parent/marketplace/orders
  Body: {
    items: [
      { product_id: "uuid", quantity: 2, variant: "M" }
    ],
    fulfillment_type: "pickup",  // pickup | delivery | digital
    delivery_address: "123 Main St, City",  // if delivery
    student_id: "uuid"  // optional
  }
  → 201: { order: OrderDto, razorpay_order_id: "ord_xxx", razorpay_key: "key_xxx" }
  → 400: Invalid items, out of stock

GET /api/v1/parent/marketplace/orders
  → 200: { orders: [OrderDto], total: Int }
```

### 9.2 Admin Endpoints

```
GET /api/v1/school/marketplace/products
  → 200: { products: [ProductDto], total: Int }

POST /api/v1/school/marketplace/products
  Body: {
    name: "School Uniform - Shirt",
    description: "White cotton shirt",
    category: "uniform",
    price: 500,
    compare_at_price: 600,
    images: ["https://supabase.url/img1.webp"],
    stock: 100,
    is_digital: false,
    variants: [{"name": "Size", "options": ["S", "M", "L", "XL"]}]
  }
  → 201: ProductDto

PATCH /api/v1/school/marketplace/products/{id}
  Body: { ...fields to update }
  → 200: ProductDto

GET /api/v1/school/marketplace/orders?status={status}
  → 200: { orders: [OrderDto], total: Int, page: Int, pageSize: Int }

POST /api/v1/school/marketplace/orders/{id}/status
  Body: { status: "confirmed" }  // confirmed | packed | ready | delivered | completed | cancelled
  → 200: OrderDto
```

### 9.3 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class ProductDto(
    val id: String,
    val name: String,
    val description: String?,
    val category: String,
    val price: Double,
    val compareAtPrice: Double?,
    val images: List<String>,
    val stock: Int,
    val isDigital: Boolean,
    val variants: List<VariantDto>?,
    val isActive: Boolean,
)

@Serializable data class VariantDto(
    val name: String,
    val options: List<String>,
)

@Serializable data class OrderDto(
    val id: String,
    val orderNumber: String,
    val items: List<OrderItemDto>,
    val totalAmount: Double,
    val paymentStatus: String,
    val fulfillmentType: String,
    val deliveryAddress: String?,
    val status: String,
    val placedAt: String,
    val confirmedAt: String?,
    val deliveredAt: String?,
)

@Serializable data class OrderItemDto(
    val productId: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val variant: String?,
)

@Serializable data class CartItem(
    val productId: String,
    val quantity: Int,
    val variant: String?,
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `MarketplaceScreen` | Compose | Parent | Product browse with category filter and search |
| `ProductDetailScreen` | Compose | Parent | Product details with images, variants, add to cart |
| `CartScreen` | Compose | Parent | Cart items, quantity, checkout with Razorpay |
| `OrderHistoryScreen` | Compose | Parent | Past orders with status tracking |
| `MarketplaceAdminScreen` | Compose | School Admin | Product management (list, create, edit, deactivate) |
| `OrderManagementScreen` | Compose | School Admin | Order list with status filter, order detail, status update |

### 10.2 Navigation

- Parent: Home tab → Marketplace → Product Detail → Cart → Checkout → Order Confirmation
- Parent: Profile → Order History → Order Detail
- Admin: Admin tab → Marketplace Admin → Product Create/Edit
- Admin: Admin tab → Order Management → Order Detail

### 10.3 UX Flows

#### Parent: Browse and Purchase
1. Parent opens Marketplace
2. Browses products by category or searches
3. Taps product → sees details (images, price, variants, stock)
4. Selects variant (size/color) and quantity
5. Adds to cart
6. Opens cart → reviews items
7. Checks out → selects pickup or delivery
8. Pays via Razorpay checkout
9. Sees order confirmation with order number
10. Receives notification on status changes

#### Admin: Manage Products
1. Admin opens Marketplace Admin
2. Sees product list with stock levels
3. Taps "Add Product" → fills form (name, description, category, price, images, stock, variants)
4. Saves product → visible to parents
5. Edits product → updates price, stock, etc.
6. Deactivates product → hidden from parents

#### Admin: Manage Orders
1. Admin opens Order Management
2. Sees orders filtered by status
3. Taps order → sees details (items, parent, student, payment, fulfillment)
4. Updates status: confirm → pack → ready/deliver → complete
5. Parent receives notification on each status change

### 10.4 State Management

```kotlin
data class MarketplaceState(
    val products: List<ProductDto>,
    val selectedCategory: String?,
    val searchQuery: String,
    val isLoading: Boolean,
    val error: String?,
)

data class CartState(
    val items: List<CartItem>,
    val totalAmount: Double,
)

data class OrderHistoryState(
    val orders: List<OrderDto>,
    val isLoading: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Product list cached locally (last fetched)
- Cart persisted in DataStore (survives app restart)
- Order placement requires online (Razorpay)
- Order history cached locally (last fetched)

### 10.6 Loading States

- Product list: "Loading products..."
- Product detail: "Loading..."
- Cart: no loading (local)
- Checkout: "Processing payment..."
- Order history: "Loading orders..."

### 10.7 Error Handling (UI)

- Product load failure: "Failed to load products. Pull to refresh."
- Out of stock: "This item is out of stock."
- Payment failure: "Payment failed. Please try again."
- Order placement failure: "Failed to place order. Please try again."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Product images use Coil (async loading, WebP support) |
| **R2** | Category filter as chips/tabs |
| **R3** | Cart badge on navigation icon showing item count |
| **R4** | Razorpay checkout opens native SDK (not webview) |
| **R5** | Order status shown as stepper/timeline |
| **R6** | Admin product form with image picker (Supabase upload) |
| **R7** | Low stock badge on admin product list |
| **R8** | Order number prominently displayed in confirmation and history |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.3, placed in `shared/.../marketplace/domain/model/MarketplaceModels.kt`.

### 11.2 Domain Models

```kotlin
data class Product(
    val id: String,
    val name: String,
    val description: String?,
    val category: ProductCategory,
    val price: Double,
    val compareAtPrice: Double?,
    val images: List<String>,
    val stock: Int,
    val isDigital: Boolean,
    val variants: List<Variant>?,
    val isActive: Boolean,
)

enum class ProductCategory { UNIFORM, BOOKS, STATIONERY, MERCHANDISE, TICKETS, DIGITAL }

data class Order(
    val id: String,
    val orderNumber: String,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val paymentStatus: PaymentStatus,
    val fulfillmentType: FulfillmentType,
    val status: OrderStatus,
    val placedAt: Instant,
)

enum class PaymentStatus { PENDING, PAID, FAILED, REFUNDED }
enum class FulfillmentType { PICKUP, DELIVERY, DIGITAL }
enum class OrderStatus { PLACED, CONFIRMED, PACKED, READY, DELIVERED, COMPLETED, CANCELLED }
```

### 11.3 Repository Interfaces

```kotlin
interface ProductRepository {
    suspend fun getProducts(token: String, category: String?, search: String?): NetworkResult<List<ProductDto>>
    suspend fun getProduct(token: String, id: String): NetworkResult<ProductDto>
    suspend fun createProduct(token: String, request: CreateProductRequest): NetworkResult<ProductDto>
    suspend fun updateProduct(token: String, id: String, request: UpdateProductRequest): NetworkResult<ProductDto>
}

interface OrderRepository {
    suspend fun placeOrder(token: String, request: PlaceOrderRequest): NetworkResult<PlaceOrderResponse>
    suspend fun getOrderHistory(token: String): NetworkResult<List<OrderDto>>
    suspend fun getSchoolOrders(token: String, status: String?): NetworkResult<PaginatedResult<OrderDto>>
    suspend fun updateOrderStatus(token: String, orderId: String, status: String): NetworkResult<OrderDto>
}
```

### 11.4 UseCases

- `GetProductsUseCase`
- `GetProductDetailUseCase`
- `PlaceOrderUseCase`
- `GetOrderHistoryUseCase`
- `CreateProductUseCase`
- `UpdateProductUseCase`
- `UpdateOrderStatusUseCase`

### 11.5 Validation

- Product name: non-empty, max 200 characters
- Product price: > 0
- Product stock: ≥ 0
- Order items: non-empty list, each with valid product_id and quantity > 0
- Delivery address: required if fulfillment_type = 'delivery'

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `MarketplaceApi.kt`:
- GET/POST products (parent + admin)
- GET products/{id}
- PATCH products/{id}
- POST orders
- GET orders
- POST orders/{id}/status

### 11.8 Database Models (Local Cache)

- Product list cached in local DB (last fetched timestamp)
- Cart stored in DataStore (key-value, JSON)
- Order history cached in local DB

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Teacher | Parent |
|---|---|---|---|---|
| Browse products | N/A | ✅ | ❌ | ✅ |
| View product details | N/A | ✅ | ❌ | ✅ |
| Add to cart / checkout | N/A | ❌ | ❌ | ✅ |
| View own order history | N/A | ✅ | ❌ | ✅ |
| Create/edit products | N/A | ✅ | ❌ | ❌ |
| Deactivate products | N/A | ✅ | ❌ | ❌ |
| View school orders | ✅ | ✅ | ❌ | ❌ |
| Update order status | N/A | ✅ | ❌ | ❌ |
| View low stock alerts | N/A | ✅ | ❌ | ❌ |
| Initiate refund | N/A | ✅ | ❌ | ❌ |

---

## 13. Notifications

### Order Status Notifications

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| Order placed | Parent | FCM + in-app | "Order {orderNumber} placed. Total: ₹{total}." |
| Payment confirmed | Parent | FCM + in-app | "Payment confirmed for order {orderNumber}." |
| Order confirmed | Parent | FCM + in-app | "Order {orderNumber} confirmed. School is processing your order." |
| Order packed | Parent | FCM + in-app | "Order {orderNumber} packed and ready for dispatch." |
| Order ready for pickup | Parent | FCM + in-app | "Order {orderNumber} ready for pickup at school." |
| Order delivered | Parent | FCM + in-app | "Order {orderNumber} delivered." |
| Order completed | Parent | FCM + in-app | "Order {orderNumber} completed. Thank you!" |
| Order cancelled | Parent | FCM + in-app | "Order {orderNumber} cancelled." |
| Low stock alert | School Admin | FCM + in-app | "Low stock: {productName} has {stock} units left." |
| New order received | School Admin | FCM + in-app | "New order {orderNumber} received from {parentName}." |

### Notification Integration

Uses existing `Notify.kt` dispatch. Notifications created with category "marketplace" and sent via FCM + in-app.

---

## 14. Background Jobs

### Low Stock Check Job

| Property | Value |
|---|---|
| **Name** | `LowStockCheckJob` |
| **Schedule** | Daily (9 AM IST) |
| **Duration** | < 1 minute |
| **Retry** | None (next day) |

#### Job Flow

1. Query `marketplace_products` where `stock < threshold` AND `is_active = true`
2. For each low-stock product, send notification to school admin
3. Return count

### Order Auto-Complete Job

| Property | Value |
|---|---|
| **Name** | `OrderAutoCompleteJob` |
| **Schedule** | Hourly |
| **Duration** | < 1 minute |
| **Retry** | None (next hour) |

#### Job Flow

1. Query `marketplace_orders` where `status = 'delivered'` AND `delivered_at < now() - 24 hours`
2. Update status to 'completed'
3. Send completion notification to parent
4. Return count

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `FEE_PAYMENT_SPEC.md` (Razorpay) | Payment processing | Call | HTTP API | Existing error handling |
| `Notify.kt` | Order notifications | Call | Direct call | Log on failure |
| `NotificationService.kt` | FCM push for order updates | Call | Direct call | Existing error handling |
| Supabase Storage | Product image storage | Upload/Read | HTTP API | Log on failure |
| `AppUsersTable` | Parent info (name) | Read | Direct DB | Default "Unknown" if not found |
| `StudentsTable` | Student info (name) | Read | Direct DB | Nullable |

### External Integrations

| System | Purpose | Direction | Protocol | Authentication | Error Handling |
|---|---|---|---|---|---|
| Razorpay | Payment processing | Outbound | HTTP API | API key (existing) | Existing retry + webhook |
| Supabase Storage | Product image hosting | Outbound | HTTP API | Service key (existing) | Log on failure |

### Integration Patterns

- **Razorpay:** Create order → parent pays via checkout → webhook confirms payment. Reuses existing `PaymentService` from `FEE_PAYMENT_SPEC.md`.
- **Supabase Storage:** Admin uploads images → Supabase URLs stored in `marketplace_products.images` JSON array.
- **Notifications:** `Notify.kt` called with category "marketplace" for all order status changes.

---

## 16. Security

### Authentication

- All marketplace APIs: JWT auth via `requireAuth()`
- Parent endpoints: parent role required
- Admin endpoints: school admin role required

### Authorization

- Parent can only view own orders
- School admin can only manage products/orders for own school
- Super admin can view all schools' marketplace data

### Data Protection

- Product data — public within school (all parents can see)
- Order data — private per parent (only own orders)
- Payment data — Razorpay payment ID stored, no card details

### Input Validation

- Product name: non-empty, max 200 characters
- Product price: > 0
- Product stock: ≥ 0
- Order items: non-empty, valid product IDs, quantity > 0
- Delivery address: required if fulfillment_type = 'delivery'
- Images: max 8 per product, max 500KB each, WebP format

### Rate Limiting

- Product browse: standard API rate limiting
- Order placement: 10 per parent per day
- Product creation: 100 per school per day

### Audit Logging

- Product created/updated/deactivated: admin ID, product ID, changes
- Order placed: parent ID, order ID, items, total
- Order status changed: admin ID, order ID, old status, new status
- Payment confirmed: order ID, Razorpay payment ID

### PII Handling

- Parent name and student name stored in order for admin reference
- Delivery address stored if delivery selected
- No card/bank details stored (Razorpay handles)

### Multi-tenant Isolation

- `marketplace_products.school_id` — school-scoped
- `marketplace_orders.school_id` — school-scoped
- All queries filtered by `school_id`
- No cross-school marketplace access

---

## 17. Performance & Scalability

### Expected Scale

- Small school: 50 products, 20 orders/day
- Medium school: 200 products, 100 orders/day
- Large school: 500 products, 500 orders/day
- Multi-school: 5,000 products, 5,000 orders/day across 10 schools

### Query Optimization

- Product browse: `idx_marketplace_products_school(school_id, category, is_active)` — O(log n) lookup
- Admin orders: `idx_marketplace_orders_school(school_id, status, placed_at DESC)` — sorted by date
- Parent orders: `idx_marketplace_orders_parent(parent_id, placed_at DESC)` — sorted by date

### Indexing Strategy

- `marketplace_products(school_id, category, is_active)` — browse filter
- `marketplace_orders(school_id, status, placed_at DESC)` — admin dashboard
- `marketplace_orders(parent_id, placed_at DESC)` — parent history
- `marketplace_orders(order_number)` — UNIQUE, order number lookup

### Caching Strategy

- Product list: cached per school per category, 5-minute TTL
- Product detail: cached per product, 5-minute TTL
- Order history: not cached (real-time)

### Pagination

- Product list: 20 per page
- Admin orders: 20 per page
- Parent orders: 20 per page

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Product CRUD: synchronous
- Order placement: synchronous (Razorpay order creation)
- Payment webhook: asynchronous (webhook handler)
- Notifications: async (fire-and-forget via `Notify.kt`)

### Scalability Concerns

- Product images: stored in Supabase Storage, not DB. No DB impact.
- Order items: stored as JSON in `marketplace_orders.items`. No join needed.
- Order number generation: sequential per school per year. No contention.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Product out of stock during checkout | Return 400 "Item out of stock." |
| EC-2 | Stock reduced between cart add and checkout | Return 400 "Only {N} units available." |
| EC-3 | Razorpay payment timeout | Order remains 'placed' with 'pending' payment. Parent can retry. |
| EC-4 | Razorpay webhook delayed | Order remains 'placed'. Webhook handler updates when received. |
| EC-5 | Admin deactivates product while in cart | Product still checkoutable (cart has snapshot). Show "Product no longer available" warning. |
| EC-6 | Parent places order for digital product with delivery | Ignore delivery address. Set fulfillment_type = 'digital'. |
| EC-7 | Order number sequence exhausted (9999 per year) | Wrap to 5 digits: "ORD-2026-10000". |
| EC-8 | Admin cancels confirmed order | Initiate Razorpay refund. Set payment_status = 'refunded'. |
| EC-9 | Parent tries to view another parent's order | Return 403 "Access denied." |
| EC-10 | Product image upload fails | Product saved without image. Admin can retry upload. |
| EC-11 | Variant selected but product has no variants | Ignore variant. Order item has null variant. |
| EC-12 | Cart empty at checkout | Return 400 "Cart is empty." |
| EC-13 | Razorpay webhook received for unknown order | Log error. Ignore webhook. |
| EC-14 | Duplicate webhook received | Idempotent handler. Check if order already confirmed. Skip if so. |
| EC-15 | Product price changed after added to cart | Cart uses price at checkout time, not add time. Show updated price. |
| EC-16 | School admin from school A tries to manage school B products | Return 403 "Access denied." |
| EC-17 | Order auto-complete runs but order already completed | Skip. No error. |
| EC-18 | Low stock alert for product with threshold 0 | No alert sent. Threshold 0 = disabled. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format. Errors returned with HTTP status code and message.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `PRODUCT_NOT_FOUND` | 404 | Product ID not found | "Product not found." |
| `OUT_OF_STOCK` | 400 | Product stock is 0 | "This item is out of stock." |
| `INSUFFICIENT_STOCK` | 400 | Requested quantity exceeds stock | "Only {N} units available." |
| `ORDER_NOT_FOUND` | 404 | Order ID not found | "Order not found." |
| `PAYMENT_FAILED` | 400 | Razorpay payment failed | "Payment failed. Please try again." |
| `INVALID_ORDER_STATUS` | 400 | Invalid status transition | "Cannot change order status from {old} to {new}." |
| `CART_EMPTY` | 400 | No items in cart | "Cart is empty." |
| `DELIVERY_ADDRESS_REQUIRED` | 400 | Delivery selected but no address | "Delivery address is required." |
| `ACCESS_DENIED` | 403 | Cross-school or cross-parent access | "Access denied." |

### Error Handling Strategy

- **Payment errors:** Order created with 'pending' status. Parent can retry payment. No stock decrement.
- **Webhook errors:** Log error. Idempotent handler. Razorpay retries webhook.
- **Stock errors:** Return 400 with available stock. Cart updated.
- **Order status errors:** Return 400 if invalid transition. No state change.

### Retry Strategy

- Razorpay payment: parent can retry from order detail (if order is 'placed' with 'pending' payment)
- Razorpay webhook: Razorpay retries automatically (existing infrastructure)
- Product image upload: admin can retry upload

### Fallback Behavior

- Razorpay unavailable: order cannot be placed. Show "Payment service unavailable."
- Supabase Storage unavailable: product saved without image. Admin can add image later.
- Notification failure: order still processed. Notification logged as failed.

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total products by category | `marketplace_products.category` | Group by category, count |
| Active vs inactive products | `marketplace_products.is_active` | Count by status |
| Total orders by status | `marketplace_orders.status` | Group by status, count |
| Total revenue | `marketplace_orders.total_amount` WHERE `payment_status = 'paid'` | Sum |
| Average order value | `marketplace_orders.total_amount` WHERE `payment_status = 'paid'` | Avg |
| Orders per day | `marketplace_orders.placed_at` | Count by date |
| Low stock products | `marketplace_products.stock < threshold` | Count |
| Top selling products | `marketplace_orders.items` JSON | Parse and aggregate |
| Payment success rate | `payment_status = 'paid'` / total orders | Percentage |
| Fulfillment type distribution | `marketplace_orders.fulfillment_type` | Group by type, count |

### Export Capabilities

- Order export (CSV) — order number, date, parent, items, total, status, payment status
- Product export (CSV) — name, category, price, stock, active status

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Sales summary | JSON (API) | On-demand | School Admin |
| Order log | CSV export | On-demand | School Admin |
| Product inventory | CSV export | On-demand | School Admin |
| Low stock report | JSON (API) | Daily | School Admin |

---

## 21. Testing Strategy

### Unit Tests

- `ProductService.createProduct()` — validation, image upload, DB insert
- `ProductService.updateProduct()` — partial updates, stock changes
- `OrderService.placeOrder()` — order number generation, stock check, total calculation
- `OrderService.confirmPayment()` — payment verification, stock decrement, status update
- `OrderService.updateOrderStatus()` — valid transitions, invalid transitions rejected
- Order number generation — sequential, unique per school per year
- Stock decrement — atomic, prevents overselling

### Integration Tests

- Full order lifecycle: place → pay → confirm → pack → ready → deliver → complete
- Payment failure: place → pay fails → cancelled
- Low stock: product stock decremented to below threshold → alert sent
- Cart checkout: add items → checkout → Razorpay → order created
- Admin product management: create → edit → deactivate → reactivate

### E2E Tests

- Parent browses products → adds to cart → checks out → pays → receives order confirmation → tracks status
- Admin creates product → parent sees it → orders → admin processes order → parent receives notifications

### Performance Tests

- Product list: 500 products loads in < 1 second
- Order placement: < 5 seconds including Razorpay
- Admin dashboard: 1000 orders loads in < 2 seconds
- Concurrent orders: 50 simultaneous checkouts without stock issues

### Test Data

- 20 sample products across all categories
- 5 sample orders in different statuses
- Mock Razorpay (returns success/failure)
- Mock Supabase Storage (returns URLs)

### Test Environment

- Test database with marketplace tables
- Mock Razorpay gateway
- Mock Supabase Storage
- Test JWT tokens for parent and admin roles

---

## 22. Acceptance Criteria

- [ ] Admin lists products with images, variants, stock
- [ ] Parent browses, searches, views product details
- [ ] Cart and checkout work with Razorpay payment
- [ ] Order status tracked (placed → confirmed → packed → ready → delivered)
- [ ] Admin order management dashboard
- [ ] Parent order history
- [ ] Low stock alerts
- [ ] Pickup and delivery options
- [ ] Order number auto-generated (ORD-YYYY-NNNN)
- [ ] Stock decremented on order confirmation
- [ ] Notifications sent on order status change
- [ ] Digital products supported with QR code delivery
- [ ] Cart persists across app sessions

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration `migration_071_marketplace.sql`, Exposed tables, register in `DatabaseFactory` |
| 2 | 2 days | `ProductService` + `OrderService` (CRUD, order placement, status management) |
| 3 | 2 days | Razorpay payment integration (reuse from `FEE_PAYMENT_SPEC.md`) + webhook handler |
| 4 | 2 days | API endpoints: parent (browse, order, history) + admin (products, orders, status) |
| 5 | 4 days | Client UI: `MarketplaceScreen`, `ProductDetailScreen`, `CartScreen`, `OrderHistoryScreen`, `MarketplaceAdminScreen`, `OrderManagementScreen` |
| 6 | 2 days | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify Razorpay integration from `FEE_PAYMENT_SPEC.md` is reusable
- [ ] Verify Supabase Storage bucket for product images
- [ ] Verify `Notify.kt` supports custom categories ("marketplace")
- [ ] Verify Razorpay webhook handler can be extended for marketplace orders

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `MarketplaceProductsTable`, `MarketplaceOrdersTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register marketplace tables in `allTables` |
| `server/.../feature/marketplace/ProductService.kt` | **New** | Product CRUD, stock management |
| `server/.../feature/marketplace/OrderService.kt` | **New** | Order placement, status, history |
| `server/.../feature/marketplace/MarketplaceRouting.kt` | **New** | API endpoints (parent + admin) |
| `server/.../feature/marketplace/MarketplaceWebhookHandler.kt` | **New** | Razorpay webhook for marketplace orders |
| `server/.../feature/marketplace/LowStockCheckJob.kt` | **New** | Daily low stock alert job |
| `server/.../feature/marketplace/OrderAutoCompleteJob.kt` | **New** | Hourly auto-complete job |
| `docs/db/migration_071_marketplace.sql` | **New** | DDL: `marketplace_products` + `marketplace_orders` |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../marketplace/domain/model/MarketplaceModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../marketplace/domain/repository/ProductRepository.kt` | **New** | Product repository interface |
| `shared/.../marketplace/domain/repository/OrderRepository.kt` | **New** | Order repository interface |
| `shared/.../marketplace/data/remote/MarketplaceApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/parent/MarketplaceScreen.kt` | **New** | Product browse with category filter and search |
| `composeApp/.../ui/v2/screens/parent/ProductDetailScreen.kt` | **New** | Product details, variant selection, add to cart |
| `composeApp/.../ui/v2/screens/parent/CartScreen.kt` | **New** | Cart items, checkout with Razorpay |
| `composeApp/.../ui/v2/screens/parent/OrderHistoryScreen.kt` | **New** | Parent order history with status tracking |
| `composeApp/.../ui/v2/screens/admin/MarketplaceAdminScreen.kt` | **New** | Product management (list, create, edit) |
| `composeApp/.../ui/v2/screens/admin/OrderManagementScreen.kt` | **New** | Order management dashboard |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Product reviews and ratings | Medium | M | Parents rate and review products |
| F-2 | Wishlist | Low | S | Parents save products for later |
| F-3 | Return/refund workflow | Medium | M | Automated return request and refund |
| F-4 | Discount coupons | Medium | S | Promo codes for discounts |
| F-5 | Bundle products | Low | M | Sell multiple products as a bundle |
| F-6 | Multi-school marketplace | Low | L | Aggregate products from multiple schools |
| F-7 | Subscription products | Low | L | Recurring deliveries (e.g., monthly stationery kit) |
| F-8 | Product recommendations | Medium | M | AI-powered product recommendations |
| F-9 | Inventory import/export | Low | S | Bulk product import via CSV |
| F-10 | Delivery tracking | Medium | M | Real-time delivery tracking with partner integration |

---

## Appendix A: Sequence Diagrams

### A.1 Order Placement Flow

```
Parent (app)       Server              Razorpay          Webhook
  │                  │                    │                 │
  │  POST /orders    │                    │                 │
  │  {items, fulfillment}                 │                 │
  │  ──────────────> │                    │                 │
  │                  │──validate items──> │                 │
  │                  │──check stock─────> │                 │
  │                  │──create order────> │                 │
  │                  │──create Razorpay order─────────────>│                 │
  │                  │←──razorpay_order_id────────────────│                 │
  │  ←──201: order + razorpay_order_id    │                 │
  │                  │                    │                 │
  │  ──Razorpay checkout (native SDK)──>  │                 │
  │  ←──payment success──                 │                 │
  │                  │                    │                 │
  │                  │                    │──webhook: payment.captured─────>│
  │                  │←──payment confirmed─────────────────────────────────│
  │                  │──update order status = confirmed                    │
  │                  │──decrement stock──>│                                │
  │                  │──send notification─>│                               │
  │  ←──FCM: "Order confirmed"            │                                │
  │                  │                    │                 │
```

### A.2 Admin Order Management Flow

```
Admin (app)        Server              Notify.kt          Parent
  │                  │                    │                 │
  │  POST /orders/{id}/status             │                 │
  │  { status: "packed" }                 │                 │
  │  ──────────────> │                    │                 │
  │                  │──validate transition                  │
  │                  │──update order status                  │
  │                  │──send notification─>│                 │
  │                  │                    │──FCM + in-app──>│
  │                  │                    │                 │
  │  ←──200: updated order                 │                 │
  │                  │                    │                 │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                    marketplace_products (new)                          │
│  id (PK)                                                              │
│  school_id, name, description                                         │
│  category (uniform|books|stationery|merchandise|tickets|digital)      │
│  price, compare_at_price                                              │
│  images (JSON array of Supabase URLs)                                 │
│  stock, is_digital                                                    │
│  variants (JSON: [{name, options}])                                   │
│  is_active, created_at, updated_at                                    │
│  INDEX: (school_id, category, is_active)                              │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           │ 1:N (via items JSON)
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                     marketplace_orders (new)                           │
│  id (PK)                                                              │
│  school_id, order_number (UNIQUE)                                     │
│  parent_id, parent_name                                               │
│  student_id, student_name (nullable)                                  │
│  items (JSON: [{product_id, name, price, quantity, variant}])         │
│  total_amount                                                         │
│  payment_id, payment_status (pending|paid|failed|refunded)            │
│  fulfillment_type (pickup|delivery|digital), delivery_address         │
│  status (placed|confirmed|packed|ready|delivered|completed|cancelled) │
│  placed_at, confirmed_at, delivered_at, created_at                    │
│  INDEX: (school_id, status, placed_at DESC)                           │
│  INDEX: (parent_id, placed_at DESC)                                   │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `OrderPlaced` | `OrderService.placeOrder()` | None (logged) | `orderId, orderNumber, parentId, totalAmount` | Razorpay order created |
| `PaymentConfirmed` | `MarketplaceWebhookHandler` | `OrderService.confirmPayment()` | `orderId, razorpayPaymentId` | Order confirmed, stock decremented |
| `OrderStatusChanged` | `OrderService.updateOrderStatus()` | `Notify.kt` | `orderId, oldStatus, newStatus` | Notification sent to parent |
| `LowStockAlert` | `LowStockCheckJob` | `Notify.kt` | `productId, productName, stock` | Notification sent to admin |
| `OrderCancelled` | `OrderService.updateOrderStatus()` | `Notify.kt` | `orderId, reason` | Notification sent, refund initiated |
| `OrderAutoCompleted` | `OrderAutoCompleteJob` | `Notify.kt` | `orderId` | Notification sent to parent |

### Event Delivery Guarantees

- Events emitted synchronously within service methods
- All events logged for audit
- Notification dispatch is async (fire-and-forget)

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `MARKETPLACE_ENABLED` | `true` | Enable/disable marketplace feature |
| `MARKETPLACE_LOW_STOCK_THRESHOLD` | `5` | Stock level for low stock alert |
| `MARKETPLACE_ORDER_AUTO_COMPLETE_HOURS` | `24` | Hours after delivery to auto-complete |
| `MARKETPLACE_MAX_IMAGES_PER_PRODUCT` | `8` | Max product images |
| `MARKETPLACE_MAX_IMAGE_SIZE_KB` | `500` | Max image size in KB |
| `MARKETPLACE_ORDER_RATE_LIMIT_PER_DAY` | `10` | Max orders per parent per day |
| `MARKETPLACE_PRODUCT_RATE_LIMIT_PER_DAY` | `100` | Max products created per school per day |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `MARKETPLACE_ENABLED` | `true` | Enable/disable marketplace |
| `MARKETPLACE_DIGITAL_PRODUCTS_ENABLED` | `true` | Enable/disable digital products |
| `MARKETPLACE_DELIVERY_ENABLED` | `true` | Enable/disable delivery option |
| `MARKETPLACE_LOW_STOCK_ALERTS_ENABLED` | `true` | Enable/disable low stock alerts |

### School-Level Settings

N/A — marketplace is per-school by design. No additional school-level configuration needed.

---

## Appendix E: Migration & Rollback

### Migration: `migration_071_marketplace.sql`

```sql
-- Migration 071: School Marketplace
-- Creates marketplace_products and marketplace_orders tables

BEGIN;

CREATE TABLE IF NOT EXISTS marketplace_products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,
    description     TEXT,
    category        VARCHAR(32) NOT NULL,
    price           DOUBLE PRECISION NOT NULL,
    compare_at_price DOUBLE PRECISION,
    images          TEXT NOT NULL DEFAULT '[]',
    stock           INTEGER NOT NULL DEFAULT 0,
    is_digital      BOOLEAN NOT NULL DEFAULT false,
    variants        TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_marketplace_products_school
    ON marketplace_products (school_id, category, is_active);

CREATE TABLE IF NOT EXISTS marketplace_orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    order_number    VARCHAR(16) NOT NULL UNIQUE,
    parent_id       UUID NOT NULL,
    parent_name     TEXT NOT NULL,
    student_id      UUID,
    student_name    TEXT,
    items           TEXT NOT NULL,
    total_amount    DOUBLE PRECISION NOT NULL,
    payment_id      TEXT,
    payment_status  VARCHAR(16) NOT NULL DEFAULT 'pending',
    fulfillment_type VARCHAR(16) NOT NULL DEFAULT 'pickup',
    delivery_address TEXT,
    status          VARCHAR(16) NOT NULL DEFAULT 'placed',
    placed_at       TIMESTAMP NOT NULL DEFAULT now(),
    confirmed_at    TIMESTAMP,
    delivered_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_marketplace_orders_school
    ON marketplace_orders (school_id, status, placed_at DESC);
CREATE INDEX IF NOT EXISTS idx_marketplace_orders_parent
    ON marketplace_orders (parent_id, placed_at DESC);

COMMIT;
```

### Rollback: `migration_071_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS marketplace_orders;
DROP TABLE IF EXISTS marketplace_products;
COMMIT;
```

### Migration Validation

- Verify `marketplace_products` table created with correct columns
- Verify `marketplace_orders` table created with correct columns
- Verify `order_number` UNIQUE constraint created
- Verify indexes created
- Run `SELECT count(*) FROM marketplace_products` — should be 0 (new feature)
- Run `SELECT count(*) FROM marketplace_orders` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Product created | `productId, schoolId, name, category, price` |
| INFO | Product updated | `productId, schoolId, changes` |
| INFO | Product deactivated | `productId, schoolId` |
| INFO | Order placed | `orderId, orderNumber, schoolId, parentId, totalAmount, itemCount` |
| INFO | Payment confirmed | `orderId, orderNumber, razorpayPaymentId, amount` |
| INFO | Order status changed | `orderId, orderNumber, oldStatus, newStatus, adminId` |
| INFO | Order auto-completed | `orderId, orderNumber, deliveredAt` |
| WARN | Low stock alert | `productId, schoolId, productName, stock, threshold` |
| WARN | Payment failed | `orderId, orderNumber, razorpayError` |
| WARN | Stock insufficient | `productId, requestedQty, availableStock` |
| ERROR | Razorpay order creation failed | `orderId, error` |
| ERROR | Webhook verification failed | `razorpayPaymentId, error` |
| ERROR | Order status transition invalid | `orderId, oldStatus, newStatus` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `marketplace_products_total` | Gauge | `school_id, category` | Total products by category |
| `marketplace_orders_total` | Counter | `school_id, status` | Total orders by status |
| `marketplace_revenue_total` | Counter | `school_id` | Total revenue (paid orders) |
| `marketplace_order_value` | Histogram | — | Order value distribution |
| `marketplace_payment_success_rate` | Gauge | — | Payment success percentage |
| `marketplace_low_stock_products` | Gauge | `school_id` | Products below stock threshold |
| `marketplace_orders_per_day` | Counter | `school_id` | Orders per day |
| `marketplace_fulfillment_distribution` | Gauge | `fulfillment_type` | Pickup vs delivery vs digital |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Marketplace enabled | `/health/marketplace` | Verify feature flag enabled and tables accessible |
| Razorpay integration | `/health/razorpay` | Verify Razorpay API reachable (existing) |
| Supabase Storage | `/health/storage` | Verify Supabase Storage accessible (existing) |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Payment success rate low | Success rate < 90% | Warning | Email to dev team |
| Razorpay webhook failures | Webhook error rate > 5% | Critical | Email + Slack to dev team |
| Low stock products > 10 | More than 10 products low stock | Info | Email to school admin |
| No orders in 24h | Zero orders in 24 hours | Info | Email to school admin (engagement) |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Marketplace Overview | Revenue, orders/day, top products, payment success | School Admin |
| Order Management | Orders by status, fulfillment distribution, avg order value | School Admin |
| Product Performance | Top sellers, low stock, category distribution | School Admin |
| Payment Metrics | Success rate, failure reasons, refund count | Dev Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Razorpay downtime | Low | High | Show "Payment service unavailable." Retry later. |
| Stock overselling | Low | Medium | Stock decremented atomically on confirmation. DB transaction. |
| Order number collision | Very Low | Low | UNIQUE constraint. Sequential generation per school. |
| Image upload failure | Medium | Low | Product saved without image. Admin can retry. |
| Webhook not received | Low | Medium | Order remains 'pending'. Admin can manually confirm. |
| Cart data loss | Low | Low | Cart in DataStore (persistent). Not in memory. |
| Large product catalog slow | Low | Medium | Pagination (20 per page). Caching (5-min TTL). |
| Refund processing delay | Medium | Low | Razorpay refund is async. Admin notified on completion. |
