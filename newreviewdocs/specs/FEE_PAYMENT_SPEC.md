# Fee Payment Gateway — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Audience:** Senior engineer / AI agent implementing the system
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Unblocks:** `AI_FEE_REMINDER_SPEC.md`, `FEE_EXPENSE_TRANSPARENCY_SPEC.md`

---

## Table of Contents

1. [Feature Overview](#1-feature-overview)
2. [Current System Assessment](#2-current-system-assessment)
3. [Gap Analysis](#3-gap-analysis)
4. [Functional Requirements](#4-functional-requirements)
5. [User Roles & Permissions](#5-user-roles--permissions)
6. [User Stories](#6-user-stories)
7. [UX Flow](#7-ux-flow)
8. [Screen Flow](#8-screen-flow)
9. [Database Design](#9-database-design)
10. [Backend Architecture](#10-backend-architecture)
11. [Frontend Architecture](#11-frontend-architecture)
12. [API Contracts](#12-api-contracts)
13. [Notifications](#13-notifications)
14. [Security](#14-security)
15. [Validation Rules](#15-validation-rules)
16. [Error Handling](#16-error-handling)
17. [Edge Cases](#17-edge-cases)
18. [Performance Considerations](#18-performance-considerations)
19. [Infrastructure Requirements](#19-infrastructure-requirements)
20. [Background Processing](#20-background-processing)
21. [Monitoring](#21-monitoring)
22. [Feature Flags](#22-feature-flags)
23. [Configuration](#23-configuration)
24. [Migration Strategy](#24-migration-strategy)
25. [Testing Strategy](#25-testing-strategy)
26. [Acceptance Criteria](#26-acceptance-criteria)
27. [Implementation Roadmap](#27-implementation-roadmap)
28. [File-Level Impact Analysis](#28-file-level-impact-analysis)
29. [Risks & Mitigations](#29-risks--mitigations)
30. [Future Extensibility](#30-future-extensibility)

---

## 1. Feature Overview

Online fee payment via Razorpay (primary, India-first) with UPI, cards, net banking, and wallet support. Includes fee installment plans, flexible payment schedules, automatic receipt generation, refund processing, and WhatsApp payment links.

### Goals

- Parents can pay fees online via UPI/cards/netbanking directly in the app
- School admins can create fee structures, installments, and due dates
- Automatic receipt generation (PDF) with school branding
- Payment status syncs to existing `FeeRecordsTable` in real-time
- WhatsApp payment link delivery for offline-to-online conversion
- Refund workflow for cancelled payments
- Dashboard for admins: collected, outstanding, overdue, daily collection

---

## 2. Current System Assessment

### 2.1 What Exists

- **`FeeRecordsTable`** (`Tables.kt:630-645`) — stores fee line items with `amount`, `status` (PAID/DUE/OVERDUE), `category`, `dueDate`, `lastRemindedAt`
- **`GET /api/v1/parent/fees`** — parent fee listing endpoint (read-only)
- Fee stats computed on-the-fly: `total_collected`, `outstanding`, `overdue_count`, `progress`
- **No payment gateway** — `feature_audit.csv` L135: "🔴 Missing, 0%"
- No payment table, no transaction records, no receipt generation

### 2.2 Existing Infrastructure to Reuse

- `SupabaseStorage.kt` — for receipt PDF storage
- `NotificationsTable` + `Notify.kt` — for payment confirmation notifications
- `WhatsAppCloudProvider` — for sending payment links via WhatsApp
- `AppConfigTable` — for Razorpay key configuration
- JWT auth + school isolation — existing pattern

---

## 3. Gap Analysis

| # | Gap | Impact |
|---|---|---|
| G1 | No payment gateway integration | Zero online revenue collection |
| G2 | No transaction records | Cannot reconcile payments |
| G3 | No receipt generation | Manual receipt process |
| G4 | No installment plans | Inflexible fee structure |
| G5 | No refund workflow | Cannot process cancellations |
| G6 | No admin fee dashboard | No visibility into collection |
| G7 | No fee structure creation UI | Admins cannot define fees |
| G8 | No payment link via WhatsApp | Parents must be in-app to pay |

---

## 4. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Integrate Razorpay as primary payment gateway (UPI, cards, netbanking, wallets) |
| FR-2 | School admin can create fee structures (term fees, transport, exam, etc.) per class |
| FR-3 | Support installment plans (e.g., 3 installments per term with due dates) |
| FR-4 | Parent can view outstanding fees and pay online |
| FR-5 | Payment success updates `FeeRecordsTable.status` to PAID in real-time |
| FR-6 | Generate PDF receipt with school logo, student details, payment breakdown |
| FR-7 | Email + WhatsApp + in-app notification on successful payment |
| FR-8 | Admin dashboard: total collected, outstanding, overdue, daily/monthly trends |
| FR-9 | Refund workflow: admin initiates, Razorpay processes, status tracked |
| FR-10 | Payment link generation for WhatsApp (Razorpay Payment Links API) |
| FR-11 | Webhook handler for async payment status updates from Razorpay |
| FR-12 | Support partial payments (parent pays part of outstanding amount) |
| FR-13 | Late fee auto-calculation (configurable: flat or percentage, after due date) |

---

## 5. User Roles & Permissions

| Action | Parent | School Admin | Super Admin |
|---|---|---|---|
| View own child's fees | ✅ | N/A | N/A |
| Pay fees online | ✅ | N/A | N/A |
| Download receipts | ✅ (own) | ✅ (any) | ✅ |
| Create fee structures | ❌ | ✅ | ✅ |
| Create installment plans | ❌ | ✅ | ✅ |
| View collection dashboard | ❌ | ✅ | ✅ |
| Initiate refund | ❌ | ✅ | ✅ |
| Configure Razorpay keys | ❌ | ❌ | ✅ |

---

## 6. User Stories

- As a **parent**, I want to see all outstanding fees for my child so I know what to pay
- As a **parent**, I want to pay via UPI so I don't need cash or bank visits
- As a **parent**, I want to download a receipt after payment so I have proof
- As a **parent**, I want to pay in installments so I can manage my budget
- As a **school admin**, I want to create fee structures per class so fees are standardized
- As a **school admin**, I want to see daily collection so I can track revenue
- As a **school admin**, I want to send a WhatsApp payment link to a parent who hasn't paid
- As a **school admin**, I want to process refunds for cancelled admissions

---

## 7. UX Flow

### 7.1 Parent Payment Flow

```
Fees Screen → View Outstanding Fees → Tap "Pay Now"
  → Razorpay Checkout (UPI/Card/Netbanking)
  → Payment Success → Receipt Generated
  → Notification (in-app + WhatsApp + email)
  → Fee status updated to PAID
  → Receipt available for download
```

### 7.2 Admin Fee Structure Flow

```
Admin Dashboard → Fees tab → "Create Fee Structure"
  → Select class → Enter fee type (tuition/transport/exam)
  → Enter amount → Set due date → Add installments (optional)
  → Save → Fees auto-created for all students in class
```

### 7.3 Refund Flow

```
Admin → Fees → Transaction History → Select payment
  → "Initiate Refund" → Enter reason → Confirm
  → Razorpay refund API → Status: pending → processed
  → Fee record reverts to DUE → Notification to parent
```

---

## 8. Screen Flow

| Screen | Role | Description |
|---|---|---|
| `ParentFeesScreenV2` | Parent | List of fee items with pay button, payment history |
| `FeePaymentScreen` | Parent | Razorpay checkout + payment confirmation |
| `ReceiptViewerScreen` | Parent/Admin | PDF receipt viewer with download/share |
| `AdminFeeDashboardScreen` | Admin | Collection summary, outstanding, trends |
| `AdminFeeStructureScreen` | Admin | Create/edit fee structures per class |
| `AdminTransactionsScreen` | Admin | Transaction log with filters, refund action |
| `AdminInstallmentScreen` | Admin | Configure installment plans |

---

## 9. Database Design

### 9.1 New Table: `fee_structures`

```sql
CREATE TABLE fee_structures (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    class_id        UUID,                          -- FK school_classes.id (null = all classes)
    class_name      TEXT,                          -- denormalized for display
    academic_year_id UUID,                         -- FK academic_years.id
    title           TEXT NOT NULL,                 -- "Tuition Fee Q1 2026-27"
    category        VARCHAR(32) NOT NULL,          -- Tuition | Transport | Exam | Library | Lab | Sports | Misc
    amount          DOUBLE PRECISION NOT NULL,
    currency        VARCHAR(8) NOT NULL DEFAULT 'INR',
    due_date        DATE NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_fee_structures_school_class ON fee_structures(school_id, class_id);
```

### 9.2 New Table: `fee_installments`

```sql
CREATE TABLE fee_installments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fee_structure_id UUID NOT NULL REFERENCES fee_structures(id) ON DELETE CASCADE,
    installment_number INTEGER NOT NULL,
    label           TEXT NOT NULL,                 -- "Installment 1", "Early Bird", etc.
    amount          DOUBLE PRECISION NOT NULL,
    due_date        DATE NOT NULL,
    late_fee_type   VARCHAR(8) NOT NULL DEFAULT 'flat',  -- flat | percentage
    late_fee_amount DOUBLE PRECISION NOT NULL DEFAULT 0,
    late_fee_after_days INTEGER NOT NULL DEFAULT 0,       -- grace period
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_fee_installments_structure ON fee_installments(fee_structure_id);
```

### 9.3 New Table: `payments`

```sql
CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    fee_record_id   UUID NOT NULL REFERENCES fee_records(id),
    student_id      UUID,                          -- FK students.id
    student_code    TEXT,                          -- denormalized
    parent_id       UUID NOT NULL,                 -- FK app_users.id
    razorpay_order_id   TEXT,
    razorpay_payment_id TEXT,
    razorpay_signature  TEXT,
    amount          DOUBLE PRECISION NOT NULL,
    currency        VARCHAR(8) NOT NULL DEFAULT 'INR',
    method          VARCHAR(32),                   -- upi | card | netbanking | wallet
    status          VARCHAR(16) NOT NULL DEFAULT 'created', -- created | attempted | paid | failed | refunded
    receipt_number  TEXT UNIQUE,                   -- generated receipt no.
    receipt_url     TEXT,                          -- Supabase Storage URL for PDF
    paid_at         TIMESTAMP,
    refunded_at     TIMESTAMP,
    refund_amount   DOUBLE PRECISION,
    refund_reason   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_payments_school_status ON payments(school_id, status, created_at DESC);
CREATE INDEX idx_payments_parent ON payments(parent_id, created_at DESC);
CREATE INDEX idx_payments_fee_record ON payments(fee_record_id);
```

### 9.4 Modify Existing: `fee_records`

```sql
ALTER TABLE fee_records ADD COLUMN fee_structure_id UUID;
ALTER TABLE fee_records ADD COLUMN installment_id UUID;
ALTER TABLE fee_records ADD COLUMN late_fee_amount DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE fee_records ADD COLUMN paid_amount DOUBLE PRECISION NOT NULL DEFAULT 0;
ALTER TABLE fee_records ADD COLUMN payment_id UUID;
```

### 9.5 Exposed Mappings

```kotlin
object FeeStructuresTable : UUIDTable("fee_structures", "id") {
    val schoolId       = uuid("school_id")
    val classId        = uuid("class_id").nullable()
    val className      = text("class_name").nullable()
    val academicYearId = uuid("academic_year_id").nullable()
    val title          = text("title")
    val category       = varchar("category", 32)
    val amount         = double("amount")
    val currency       = varchar("currency", 8).default("INR")
    val dueDate        = date("due_date")
    val isActive       = bool("is_active").default(true)
    val createdBy      = uuid("created_by").nullable()
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
    init { index("idx_fee_structures_school_class", false, schoolId, classId) }
}

object FeeInstallmentsTable : UUIDTable("fee_installments", "id") {
    val feeStructureId   = uuid("fee_structure_id")
    val installmentNumber = integer("installment_number")
    val label            = text("label")
    val amount           = double("amount")
    val dueDate          = date("due_date")
    val lateFeeType      = varchar("late_fee_type", 8).default("flat")
    val lateFeeAmount    = double("late_fee_amount").default(0.0)
    val lateFeeAfterDays = integer("late_fee_after_days").default(0)
    val createdAt        = timestamp("created_at")
    init { index("idx_fee_installments_structure", false, feeStructureId) }
}

object PaymentsTable : UUIDTable("payments", "id") {
    val schoolId          = uuid("school_id")
    val feeRecordId       = uuid("fee_record_id")
    val studentId         = uuid("student_id").nullable()
    val studentCode       = text("student_code").nullable()
    val parentId          = uuid("parent_id")
    val razorpayOrderId   = text("razorpay_order_id").nullable()
    val razorpayPaymentId = text("razorpay_payment_id").nullable()
    val razorpaySignature = text("razorpay_signature").nullable()
    val amount            = double("amount")
    val currency          = varchar("currency", 8).default("INR")
    val method            = varchar("method", 32).nullable()
    val status            = varchar("status", 16).default("created")
    val receiptNumber     = text("receipt_number").nullable().uniqueIndex()
    val receiptUrl        = text("receipt_url").nullable()
    val paidAt            = timestamp("paid_at").nullable()
    val refundedAt        = timestamp("refunded_at").nullable()
    val refundAmount      = double("refund_amount").nullable()
    val refundReason      = text("refund_reason").nullable()
    val createdAt         = timestamp("created_at")
    val updatedAt         = timestamp("updated_at")
    init {
        index("idx_payments_school_status", false, schoolId, status, createdAt)
        index("idx_payments_parent", false, parentId, createdAt)
        index("idx_payments_fee_record", false, feeRecordId)
    }
}
```

---

## 10. Backend Architecture

### 10.1 Component Overview

```
┌─────────────────────────────────────────────────┐
│                 Client (KMP)                     │
│  FeesScreen → FeePaymentApi → Razorpay SDK       │
└──────────────────┬──────────────────────────────┘
                   │ 1. Create Order
                   ▼
┌─────────────────────────────────────────────────┐
│              Backend (Ktor)                      │
│                                                  │
│  FeePaymentRouting                               │
│    ├── POST /fees/pay → create Razorpay order    │
│    ├── POST /fees/verify → verify signature      │
│    ├── POST /webhooks/razorpay → async updates   │
│    ├── POST /fees/refund → initiate refund       │
│    └── GET /fees/receipt/{id} → download PDF     │
│                                                  │
│  FeeStructureService                             │
│    ├── Create fee structure + auto-generate      │
│    │   fee_records for all students in class     │
│    ├── Create installment plans                  │
│    └── Apply late fees (scheduled job)           │
│                                                  │
│  RazorpayClient                                  │
│    ├── createOrder(amount, receipt)              │
│    ├── verifyPayment(paymentId, signature)       │
│    ├── fetchPayment(paymentId)                   │
│    ├── refund(paymentId, amount)                 │
│    └── createPaymentLink(amount, description)    │
│                                                  │
│  ReceiptGenerator                                │
│    └── Generate PDF → upload to Supabase Storage │
│                                                  │
│  FeeDashboardService                             │
│    └── Aggregate: collected, outstanding, trends │
└──────────────────────────────────────────────────┘
```

### 10.2 RazorpayClient

```kotlin
class RazorpayClient(
    private val httpClient: HttpClient,
    private val keyId: String,
    private val keySecret: String
) {
    suspend fun createOrder(amountPaise: Long, receipt: String, notes: Map<String, String>): RazorpayOrder
    suspend fun verifyPayment(razorpayOrderId: String, razorpayPaymentId: String, signature: String): Boolean
    suspend fun fetchPayment(paymentId: String): RazorpayPayment
    suspend fun refund(paymentId: String, amountPaise: Long, notes: Map<String, String>): RazorpayRefund
    suspend fun createPaymentLink(amountPaise: Long, description: String, customerName: String, customerPhone: String): RazorpayPaymentLink
}
```

**Razorpay API endpoints:**
- `POST https://api.razorpay.com/v1/orders` — create order
- `POST https://api.razorpay.com/v1/payments/{id}/capture` — capture payment
- `POST https://api.razorpay.com/v1/payments/{id}/refund` — refund
- `POST https://api.razorpay.com/v1/payment_links` — create payment link
- Webhook: `POST /api/v1/webhooks/razorpay` — payment.captured, payment.failed, refund.processed

### 10.3 Payment Flow (Sequence)

```
Client                    Backend                  Razorpay
  │                          │                        │
  │ 1. POST /fees/pay        │                        │
  │  (fee_record_id, amount) │                        │
  │─────────────────────────>│                        │
  │                          │ 2. createOrder()       │
  │                          │───────────────────────>│
  │                          │<───────────────────────│
  │                          │  (order_id)            │
  │                          │                        │
  │ 3. Return order_id       │                        │
  │<─────────────────────────│                        │
  │                          │                        │
  │ 4. Open Razorpay Checkout│                        │
  │  (client-side SDK)       │                        │
  │──────────────────────────────────────────────────>│
  │<──────────────────────────────────────────────────│
  │  (payment_id, signature) │                        │
  │                          │                        │
  │ 5. POST /fees/verify     │                        │
  │  (order_id, payment_id,  │                        │
  │   signature)             │                        │
  │─────────────────────────>│                        │
  │                          │ 6. Verify signature    │
  │                          │    (HMAC-SHA256)       │
  │                          │ 7. UPDATE payments     │
  │                          │    SET status='paid'   │
  │                          │ 8. UPDATE fee_records  │
  │                          │    SET status='PAID'   │
  │                          │ 9. Generate receipt PDF│
  │                          │10. Send notifications  │
  │                          │                        │
  │ 11. Return success +     │                        │
  │     receipt_url          │                        │
  │<─────────────────────────│                        │
```

### 10.4 Receipt Generator

```kotlin
class ReceiptGenerator(storage: SupabaseStorage) {
    suspend fun generate(
        payment: Payment,
        school: School,
        student: Student,
        feeRecord: FeeRecord
    ): String {
        // 1. Build PDF using a PDF library (e.g., Apache PDFBox or HTML-to-PDF)
        // 2. Upload to Supabase Storage: {schoolId}/receipts/{receiptNumber}.pdf
        // 3. Return public URL
    }
}
```

Receipt contains: school logo, school name + address, receipt number, date, student name, class, fee category, amount, payment method, transaction ID, signature line.

### 10.5 Late Fee Job

Daily scheduled job:
1. For each `fee_records` row where `status IN ('DUE', 'OVERDUE')` and `due_date < today`
2. Check if late fee already applied
3. If `late_fee_after_days` grace period passed, calculate late fee
4. Update `late_fee_amount` and set `status = 'OVERDUE'`

### 10.6 Fee Structure Auto-Generation

When admin creates a fee structure for a class:
1. Fetch all active students in that class (via `enrollments` table)
2. For each student, create a `fee_records` row linked to the `fee_structure_id`
3. If installments exist, create one `fee_records` row per installment

---

## 11. Frontend Architecture

### 11.1 Client API

```kotlin
class FeePaymentApi(httpClient: HttpClient) {
    suspend fun createOrder(feeRecordId: UUID, amount: Double): NetworkResult<CreateOrderResponse>
    suspend fun verifyPayment(orderId: String, paymentId: String, signature: String): NetworkResult<PaymentVerificationResponse>
    suspend fun getReceipt(paymentId: UUID): NetworkResult<ReceiptDto>
    suspend fun getFeeStructures(schoolId: UUID): NetworkResult<List<FeeStructureDto>>
    suspend fun createFeeStructure(request: CreateFeeStructureRequest): NetworkResult<FeeStructureDto>
    suspend fun getDashboard(): NetworkResult<FeeDashboardDto>
    suspend fun initiateRefund(paymentId: UUID, reason: String): NetworkResult<RefundDto>
    suspend fun sendPaymentLink(feeRecordId: UUID): NetworkResult<Unit>
}
```

### 11.2 Razorpay SDK Integration

- **Android:** Razorpay Android SDK (checkout via bottom sheet)
- **iOS:** Razorpay iOS SDK
- **Web (future):** Razorpay.js checkout

The client creates a Razorpay order via backend, then opens the Razorpay checkout with the order_id. On success, the client sends the payment_id + signature to the backend for verification.

### 11.3 ViewModel State

```kotlin
sealed class FeePaymentState {
    object Idle : FeePaymentState()
    object Loading : FeePaymentState()
    data class OrderCreated(val orderId: String, val amount: Double) : FeePaymentState()
    object Processing : FeePaymentState()
    data class Success(val receiptUrl: String) : FeePaymentState()
    data class Error(val message: String) : FeePaymentState()
}
```

---

## 12. API Contracts

### 12.1 Create Payment Order

```
POST /api/v1/parent/fees/pay
{
  "fee_record_id": "uuid",
  "amount": 5000.00
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "order_id": "order_Nk1234567890",
    "amount": 500000,
    "currency": "INR",
    "key": "rzp_test_1234567890"
  }
}
```

### 12.2 Verify Payment

```
POST /api/v1/parent/fees/verify
{
  "razorpay_order_id": "order_Nk1234567890",
  "razorpay_payment_id": "pay_Nk1234567890",
  "razorpay_signature": "abc123..."
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "payment_id": "uuid",
    "receipt_number": "RCP-2026-00001",
    "receipt_url": "https://supabase.co/storage/v1/...",
    "fee_status": "PAID"
  }
}
```

### 12.3 Razorpay Webhook

```
POST /api/v1/webhooks/razorpay
X-Razorpay-Signature: abc123...
{
  "event": "payment.captured",
  "payload": {
    "payment": {
      "entity": {
        "id": "pay_Nk1234567890",
        "order_id": "order_Nk1234567890",
        "amount": 500000,
        "status": "captured",
        "method": "upi"
      }
    }
  }
}
```

### 12.4 Admin Fee Dashboard

```
GET /api/v1/school/fees/dashboard?academic_year_id={uuid}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "total_collected": 450000.00,
    "total_outstanding": 120000.00,
    "total_overdue": 30000.00,
    "collection_rate": 0.789,
    "daily_collection": [
      {"date": "2026-06-27", "amount": 15000.00, "count": 3}
    ],
    "by_category": [
      {"category": "Tuition", "collected": 300000, "outstanding": 80000},
      {"category": "Transport", "collected": 100000, "outstanding": 20000}
    ],
    "recent_payments": [
      {"student_name": "Aarav Sharma", "amount": 5000, "method": "upi", "date": "2026-06-27T10:30:00Z"}
    ]
  }
}
```

### 12.5 Create Fee Structure

```
POST /api/v1/school/fees/structures
{
  "class_id": "uuid",
  "title": "Tuition Fee Q1 2026-27",
  "category": "Tuition",
  "amount": 15000.00,
  "due_date": "2026-07-15",
  "installments": [
    {"label": "Installment 1", "amount": 5000, "due_date": "2026-07-15"},
    {"label": "Installment 2", "amount": 5000, "due_date": "2026-08-15"},
    {"label": "Installment 3", "amount": 5000, "due_date": "2026-09-15"}
  ]
}
```

### 12.6 Send Payment Link (WhatsApp)

```
POST /api/v1/school/fees/payment-link
{
  "fee_record_id": "uuid"
}
```

Creates a Razorpay Payment Link and sends it via WhatsApp to the parent.

### 12.7 Initiate Refund

```
POST /api/v1/school/fees/refund
{
  "payment_id": "uuid",
  "reason": "Admission cancelled"
}
```

---

## 13. Notifications

| Event | Channel | Template |
|---|---|---|
| Payment success | In-app + WhatsApp + FCM | "Fee payment of ₹{amount} received for {student_name}. Receipt: {receipt_number}" |
| Payment failure | In-app + FCM | "Payment of ₹{amount} failed. Please try again." |
| Fee due reminder | WhatsApp + FCM | "Fee of ₹{amount} for {student_name} is due on {date}. Pay now: {payment_link}" |
| Fee overdue | WhatsApp + FCM | "Fee of ₹{amount} is overdue. Late fee of ₹{late_fee} applies. Pay now: {link}" |
| Refund processed | In-app + WhatsApp | "Refund of ₹{amount} processed for {student_name}. Will credit in 5-7 days." |
| New fee structure created | In-app + FCM | "New fee '{title}' of ₹{amount} assigned. Due: {date}" |

---

## 14. Security

- Razorpay key_id and key_secret stored encrypted in `AppConfigTable` (AES-256)
- Webhook signature verification using HMAC-SHA256 with webhook secret
- Payment verification uses server-side signature check (not client-side)
- Amount validation: server re-checks amount matches fee_record before creating order
- Idempotency: webhook events deduplicated by event ID
- No card/UPI details stored — Razorpay handles all PCI-DSS compliance
- Receipt URLs are public but contain UUIDs (unguessable)
- Admin refund requires 2FA (if `TWO_FACTOR_AUTH_SPEC.md` implemented)

---

## 15. Validation Rules

| Field | Rule |
|---|---|
| amount | > 0, ≤ 10,00,000 (10 lakh per transaction) |
| fee_record_id | Must exist and belong to parent's child |
| due_date | Must be a valid date, not in the past (on creation) |
| installment amounts | Sum of installments must equal total amount |
| late_fee_amount | ≥ 0 |
| late_fee_after_days | ≥ 0, ≤ 30 |
| refund_amount | ≤ original payment amount |

---

## 16. Error Handling

| Code | HTTP | Message |
|---|---|---|
| `FEE_NOT_FOUND` | 404 | Fee record not found |
| `FEE_ALREADY_PAID` | 409 | This fee has already been paid |
| `PAYMENT_VERIFICATION_FAILED` | 400 | Payment signature verification failed |
| `PAYMENT_FAILED` | 402 | Payment was not successful |
| `RAZORPAY_ERROR` | 502 | Razorpay API error: {message} |
| `REFUND_FAILED` | 502 | Refund processing failed |
| `AMOUNT_MISMATCH` | 400 | Payment amount does not match fee amount |
| `RAZORPAY_NOT_CONFIGURED` | 503 | Payment gateway not configured for this school |

---

## 17. Edge Cases

- **Double payment:** Parent pays via app and via WhatsApp link simultaneously → webhook dedup via `razorpay_payment_id` unique check
- **Partial payment:** Parent pays ₹3000 of ₹5000 → `fee_records.paid_amount = 3000`, status remains DUE
- **Razorpay webhook delayed:** Payment captured but webhook not received → client-side verify still works; webhook is backup
- **Student transferred mid-term:** Fee structure applies to students enrolled at creation time; transferred student keeps their fee_records
- **Class with 0 students:** Fee structure created but no fee_records generated (valid, no error)
- **Razorpay downtime:** Order creation fails → return 503 with retry message; parent can try later
- **Currency:** INR only for now; multi-currency is future extensibility

---

## 18. Performance Considerations

- Fee dashboard aggregates are cached for 5 minutes (TTL in memory)
- Payment verification is synchronous (< 500ms)
- Receipt PDF generation is async (don't block verify response)
- Razorpay API calls have 10s timeout
- Fee structure auto-generation for large classes (500+ students) is async via job queue
- Webhook handler returns 200 immediately, processes async

---

## 19. Infrastructure Requirements

- Razorpay account (live + test mode)
- Razorpay webhook URL configured in dashboard: `https://api.vidyaprayag.com/api/v1/webhooks/razorpay`
- Supabase Storage bucket for receipts: `{schoolId}/receipts/`
- PDF generation library (server-side)
- No additional server resources needed (uses existing Ktor + Supabase)

---

## 20. Background Processing

| Job | Schedule | Description |
|---|---|---|
| Late fee calculation | Daily 6 AM IST | Apply late fees to overdue records |
| Overdue status update | Daily 6 AM IST | Update DUE → OVERDUE past due_date |
| Fee reminder notifications | Daily 9 AM IST | Send reminders for DUE/OVERDUE fees (throttled by `lastRemindedAt`) |
| Webhook retry | On failure | Retry failed webhook processing 3x with backoff |
| Receipt regeneration | On demand | Admin can regenerate receipt with updated school branding |

---

## 21. Monitoring

| Metric | Type |
|---|---|
| `payments.created_total` | Counter |
| `payments.success_total` | Counter |
| `payments.failed_total` | Counter |
| `payments.amount_total` | Counter (INR) |
| `payments.refund_total` | Counter |
| `razorpay.api_latency_ms` | Histogram |
| `razorpay.webhook_received_total` | Counter |
| `receipt.generation_latency_ms` | Histogram |
| `fees.outstanding_amount` | Gauge |

**Alerts:**
- Payment failure rate > 10% in 15 min → Critical
- Razorpay API latency > 5s → Warning
- Webhook not received in 24h (but payments occurring) → Warning
- Receipt generation failure → Warning

---

## 22. Feature Flags

| Flag | Default | Description |
|---|---|---|
| `FEE_PAYMENT_ENABLED` | false | Enable/disable online payment |
| `FEE_INSTALLMENTS_ENABLED` | false | Enable installment plans |
| `FEE_LATE_FEE_ENABLED` | false | Enable automatic late fee |
| `FEE_WHATSAPP_LINK_ENABLED` | false | Enable WhatsApp payment links |
| `FEE_REFUND_ENABLED` | false | Enable refund workflow |

---

## 23. Configuration

### 23.1 Environment Variables

| Variable | Description |
|---|---|
| `RAZORPAY_KEY_ID` | Razorpay API key ID |
| `RAZORPAY_KEY_SECRET` | Razorpay API key secret |
| `RAZORPAY_WEBHOOK_SECRET` | Webhook signature verification secret |

### 23.2 AppConfigTable Keys

| Key | Description |
|---|---|
| `razorpay_key_id_{schoolId}` | Per-school Razorpay key (if multi-account) |
| `razorpay_key_secret_{schoolId}` | Per-school Razorpay secret |
| `fee_receipt_prefix_{schoolId}` | Receipt number prefix (default: "RCP") |
| `fee_late_fee_grace_days` | Default grace period (default: 7) |

---

## 24. Migration Strategy

### 24.1 Migration File

`docs/db/migration_032_fee_payment.sql`

Creates `fee_structures`, `fee_installments`, `payments` tables and adds columns to `fee_records`.

### 24.2 Rollback

```sql
-- ROLLBACK:
-- DROP TABLE IF EXISTS payments;
-- DROP TABLE IF EXISTS fee_installments;
-- DROP TABLE IF EXISTS fee_structures;
-- ALTER TABLE fee_records DROP COLUMN IF EXISTS fee_structure_id;
-- ALTER TABLE fee_records DROP COLUMN IF EXISTS installment_id;
-- ALTER TABLE fee_records DROP COLUMN IF EXISTS late_fee_amount;
-- ALTER TABLE fee_records DROP COLUMN IF EXISTS paid_amount;
-- ALTER TABLE fee_records DROP COLUMN IF EXISTS payment_id;
```

---

## 25. Testing Strategy

### 25.1 Unit Tests

- RazorpayClient — mock HTTP, verify request/response shapes
- Signature verification — valid/invalid signatures
- Late fee calculation — flat and percentage, grace period edge cases
- Fee structure auto-generation — correct number of fee_records created
- Receipt number generation — unique, sequential
- Dashboard aggregation — correct sums and rates

### 25.2 Integration Tests

- Create order → verify payment → fee status PAID (Razorpay test mode)
- Webhook handler — payment.captured event updates status
- Webhook dedup — same event ID processed twice → single update
- Refund flow — initiate → status refunded → fee reverts to DUE
- Payment link creation → WhatsApp send
- Installment plan — 3 installments → 3 fee_records created
- Cross-school isolation — parent cannot pay another school's fee

### 25.3 Razorpay Test Mode

Use Razorpay test keys for all integration tests. Test UPI ID: `success@razorpay`. Test card: `4111 1111 1111 1111`.

---

## 26. Acceptance Criteria

- [ ] Parent can view outstanding fees and pay via UPI/card
- [ ] Payment success updates fee_records to PAID within 5 seconds
- [ ] PDF receipt is generated and downloadable
- [ ] Admin can create fee structures with installments
- [ ] Fee structures auto-generate fee_records for all students in class
- [ ] Admin dashboard shows collection, outstanding, overdue, trends
- [ ] WhatsApp payment link works for offline parents
- [ ] Refund workflow reverts fee to DUE
- [ ] Late fee auto-applied after grace period
- [ ] Webhook handler processes Razorpay events correctly
- [ ] Razorpay test mode integration passes end-to-end
- [ ] All feature flags default to false (safe rollout)

---

## 27. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 3 days | DB migration, Exposed tables, RazorpayClient |
| 2 | 3 days | Payment order + verify + webhook endpoints |
| 3 | 2 days | Receipt generator (PDF + Supabase upload) |
| 4 | 3 days | Fee structure CRUD + auto-generation + installments |
| 5 | 2 days | Admin dashboard service + endpoint |
| 6 | 2 days | Late fee job + overdue status job |
| 7 | 2 days | WhatsApp payment link integration |
| 8 | 3 days | Client UI (parent payment, admin dashboard, fee structure) |
| 9 | 2 days | Razorpay SDK integration (Android + iOS) |
| 10 | 3 days | Tests (unit + integration with Razorpay test mode) |

---

## 28. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add + Modify | 3 new tables + columns on FeeRecordsTable |
| `server/.../db/DatabaseFactory.kt` | Modify | Register new tables |
| `server/.../feature/fees/RazorpayClient.kt` | New | Razorpay API client |
| `server/.../feature/fees/FeePaymentService.kt` | New | Payment orchestration |
| `server/.../feature/fees/FeeStructureService.kt` | New | Fee structure + installments |
| `server/.../feature/fees/ReceiptGenerator.kt` | New | PDF receipt generation |
| `server/.../feature/fees/FeeDashboardService.kt` | New | Dashboard aggregation |
| `server/.../feature/fees/FeePaymentRouting.kt` | New | Parent payment endpoints |
| `server/.../feature/fees/FeeStructureRouting.kt` | New | Admin fee structure endpoints |
| `server/.../feature/fees/FeeDashboardRouting.kt` | New | Admin dashboard endpoint |
| `server/.../feature/fees/RazorpayWebhookRouting.kt` | New | Webhook handler |
| `server/.../feature/fees/LateFeeJob.kt` | New | Scheduled late fee job |
| `server/.../Application.kt` | Modify | Register fee routes + webhook |
| `docs/db/migration_032_fee_payment.sql` | New | DDL migration |
| `shared/.../feature/fees/FeePaymentApi.kt` | New | Client API |
| `shared/.../feature/fees/FeeStructureApi.kt` | New | Client admin API |
| `composeApp/.../ui/v2/screens/parent/ParentFeesScreenV2.kt` | Modify | Add pay button + Razorpay checkout |
| `composeApp/.../ui/v2/screens/parent/FeePaymentViewModel.kt` | New | Payment state machine |
| `composeApp/.../ui/v2/screens/admin/AdminFeeDashboardScreen.kt` | New | Dashboard UI |
| `composeApp/.../ui/v2/screens/admin/AdminFeeStructureScreen.kt` | New | Fee structure creation |
| `composeApp/.../ui/v2/screens/admin/AdminTransactionsScreen.kt` | New | Transaction log + refund |

---

## 29. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Razorpay API downtime | Low | High | Show retry message; webhook will catch up |
| Signature verification bypass | Low | Critical | Server-side HMAC-SHA256; never trust client |
| Double payment (app + WhatsApp) | Medium | Medium | Dedup on razorpay_payment_id unique constraint |
| Receipt PDF generation fails | Medium | Low | Async; retry 3x; payment still succeeds |
| Large class fee generation OOM | Low | Medium | Batch student processing; async job |
| Razorpay key compromise | Low | Critical | Encrypted storage; separate test/live keys; IP whitelist |
| Webhook spoofing | Low | High | Signature verification with webhook secret |

---

## 30. Future Extensibility

- **Stripe integration** for international schools (multi-currency)
- **Auto-recurring payments** (Razorpay Subscriptions) for monthly fees
- **Fee waivers & discounts** (percentage/flat, per student)
- **Fee reconciliation report** (match bank settlement with payments)
- **Parent fee wallet** (prepaid balance for quick payment)
- **Multi-gateway routing** (Razorpay + Cashfree for redundancy)
