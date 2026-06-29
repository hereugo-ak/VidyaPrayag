# ID Card Generation — Optimization Specification

> **Status:** Draft
> **Created:** 2026-06-29
> **Parent Spec:** `newreviewdocs/specs/ID_CARD_GENERATION_SPEC.md`
> **Scope:** Performance, UX, reliability, and architecture optimizations for the ID Card Generation feature identified during the Loop Review (5-iteration comprehensive review).

---

## 1. Problem Statement

The ID Card Generation feature is functionally complete and compiles across all modules (`server`, `shared`, `composeApp`). However, the Loop Review identified several optimization opportunities that should be addressed before the feature handles production-scale traffic (500+ students per school, multiple schools, daily expiry checks).

Key concerns:
- Bulk generation blocks for minutes due to sequential render + upload.
- Digital ID card screen shows text placeholders instead of the server-rendered card image.
- QR codes are displayed as text labels, not scannable images.
- PDF generation loads all card images into memory simultaneously.
- No timeout on remote image downloads during rendering.
- Staff ID lookup uses fragile email matching instead of a direct foreign key.
- Template configuration JSON is stored but never applied during rendering.

---

## 2. Goals

1. Reduce bulk card generation time from O(n) sequential to O(n/batch_size) parallel.
2. Display the actual server-rendered card image in the digital ID card screen.
3. Render scannable QR codes client-side or display the server-generated QR image.
4. Stream PDF generation to avoid high memory pressure for large batches.
5. Add timeouts to all remote image fetches during rendering.
6. Add `app_user_id` FK to `NonTeachingStaffTable` for direct staff lookup.
7. Parse and apply template `frontConfig`/`backConfig` in the renderer.
8. Add initial delay to `IdCardExpiryCheckJob` to avoid startup contention.
9. Add pagination/search to the admin card list.

---

## 3. Non-Goals

- Redesigning the ID card visual layout (covered by template config in §10).
- Changing the Supabase Storage provider.
- Adding new API endpoints (all endpoints are implemented).
- Modifying the database schema for `id_cards` or `id_card_templates` tables (only `non_teaching_staff` gets a new column).

---

## 4. Optimization Items

### OPT-01: Parallel Image Uploads During Bulk Generation

**Priority:** P0 (High)
**Effort:** Medium
**Risk:** Medium (concurrency on DB + storage)

#### Current State
`IdCardService.generateCards()` iterates each person sequentially: render front → render back → upload digital card → insert DB record. For 500 students, this means 500 sequential render + upload cycles.

```
@server/.../idcard/IdCardService.kt:157-200
for (person in persons) {
    val qrPng = QrCodeGenerator.generatePng(qrData)
    val frontPng = IdCardRenderer.renderFront(cardData)
    val backPng = IdCardRenderer.renderBack(cardData)
    val digitalUrl = uploadToStorage(schoolId, frontPng, "image/png")
    val cardId = dbQuery { IdCardsTable.insertAndGetId { ... } }
    cardPairs.add(frontPng to backPng)
}
```

#### Proposed Change
1. **Phase 1 — Parallel rendering:** Use `coroutineScope` with `Dispatchers.IO` to render all card pairs in parallel using `async`/`awaitAll`. Rendering is CPU-bound but `IdCardRenderer` uses `BufferedImage` which is thread-safe per-instance.

```kotlin
val cardPairs = coroutineScope {
    persons.map { person ->
        async(Dispatchers.IO) {
            val qrData = "$qrBaseUrl?id=${person.id}&type=${person.type}"
            val qrPng = QrCodeGenerator.generatePng(qrData)
            val cardData = IdCardRenderer.CardData(...)
            IdCardRenderer.renderFront(cardData) to IdCardRenderer.renderBack(cardData)
        }
    }.awaitAll()
}
```

2. **Phase 2 — Batch uploads:** Upload digital card images in batches of 10-20 concurrently using a semaphore-limited coroutine pool.

```kotlin
val semaphore = Semaphore(10)
val digitalUrls = cardPairs.mapIndexed { index, (front, _) ->
    async {
        semaphore.withPermit {
            uploadToStorage(schoolId, front, "image/png")
        }
    }
}.awaitAll()
```

3. **Phase 3 — Batch DB inserts:** Insert all card records in a single `batchInsert` statement instead of individual `insertAndGetId` calls.

#### Expected Impact
- 500 students: ~5 minutes → ~30 seconds (estimated 10x improvement)
- Memory: Slightly higher peak due to parallel rendering (mitigated by batch size limit)

#### Acceptance Criteria
- [ ] Bulk generation of 100 cards completes in < 60 seconds
- [ ] No `OutOfMemoryError` with 500+ cards
- [ ] All cards are correctly inserted with `digitalCardUrl` and `qrCodeData`
- [ ] PDF is still generated correctly after all cards are rendered

---

### OPT-02: Display Server-Rendered Card Image in DigitalIdCardScreen

**Priority:** P0 (High)
**Effort:** Low
**Risk:** Low

#### Current State
`DigitalIdCardScreen` shows a hand-drawn `DigitalCard` composable with only name and role text. The `digitalCardUrl` is shown as "Digital card image available" text. The server already generates a full card image with photo, school name, branding, and QR code.

```
@composeApp/.../screens/parent/DigitalIdCardScreen.kt:89-95
card.digitalCardUrl?.let { url ->
    Text(
        text = "Digital card image available",
        style = VTheme.type.bodySmall,
        color = VTheme.colors.success,
    )
}
```

#### Proposed Change
1. Add `coil3` (Coil for Compose Multiplatform) as a dependency in `composeApp/build.gradle.kts`.
2. Replace the `DigitalCard` composable with an `AsyncImage` call that loads `card.digitalCardUrl`.
3. Keep the "Show Back" toggle to switch between `digitalCardUrl` (front) and a back image URL (if available, or derive from front URL by appending `/back`).
4. Fall back to the hand-drawn `DigitalCard` composable if `digitalCardUrl` is null.

```kotlin
card.digitalCardUrl?.let { url ->
    AsyncImage(
        model = url,
        contentDescription = "Digital ID Card",
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .aspectRatio(54f / 86f),
    )
} ?: run {
    DigitalCard(card = card, showFront = showFront)
}
```

#### Acceptance Criteria
- [ ] Card image loads and displays within 2 seconds on a stable connection
- [ ] Placeholder shown while loading (shimmer or spinner)
- [ ] Error state shows fallback `DigitalCard` composable
- [ ] Image is properly scaled and not distorted

---

### OPT-03: Client-Side QR Code Rendering

**Priority:** P1 (Medium)
**Effort:** Medium
**Risk:** Low

#### Current State
The digital ID card screen shows "Scan to verify profile" text instead of an actual scannable QR code. The `qrCodeData` field contains the deep-link URL but no QR image is rendered on the client.

#### Proposed Change
1. Add `qrcode-kotlin` (multiplatform QR generation library) to `shared/build.gradle.kts` commonMain dependencies.
   - Library: `io.github.g0dkar:qrcode-kotlin:4.1.1` (KMP-compatible, Apache-2.0)
2. Create a `QrCodeImage` composable in `composeApp` commonMain that takes a data string and renders a QR code as a Compose `Image`.
3. Use `QrCodeImage` in `DigitalIdCardScreen` back view to show the actual scannable QR.

```kotlin
@Composable
fun QrCodeImage(data: String, modifier: Modifier = Modifier) {
    val qrCode = remember(data) {
        QRCode.ofSquares()
            .build(data)
            .renderToImage()
    }
    Image(
        bitmap = qrCode.toComposeImageBitmap(),
        contentDescription = "QR Code",
        modifier = modifier.size(200.dp),
    )
}
```

#### Acceptance Criteria
- [ ] QR code is rendered as a scannable image (verifiable with a phone camera)
- [ ] QR encodes the `qrCodeData` URL correctly
- [ ] Rendering takes < 100ms
- [ ] Works on both Android and iOS targets

---

### OPT-04: Stream PDF Generation in Chunks

**Priority:** P1 (Medium)
**Effort:** Medium
**Risk:** Medium

#### Current State
`PdfGenerator.generate()` receives all card pairs upfront and processes them in a single loop. All `ByteArray` image data is held in memory simultaneously.

```
@server/.../idcard/PdfGenerator.kt:25-53
fun generate(cardPairs: List<Pair<ByteArray, ByteArray>>): ByteArray {
    for ((front, back) in cardPairs) {
        // Both front and back bytes are in memory for the entire duration
    }
}
```

#### Proposed Change
1. **Option A — Streaming API:** Change `generate()` to accept an `InputStream` or iterator of card pairs, writing each page and releasing image bytes immediately.

```kotlin
fun generate(cardPairProvider: () -> Pair<ByteArray, ByteArray>?, totalCards: Int): ByteArray {
    val document = Document(PageSize.A4, 36f, 36f, 36f, 36f)
    val baos = ByteArrayOutputStream()
    PdfWriter.getInstance(document, baos)
    document.open()

    var pair = cardPairProvider()
    while (pair != null) {
        val (front, back) = pair
        // Add images to document
        document.newPage()
        pair = cardPairProvider() // Fetch next pair, previous bytes can be GC'd
    }

    document.close()
    return baos.toByteArray()
}
```

2. **Option B — Split PDFs:** Generate one PDF per 50 cards, upload each separately, and return a list of PDF URLs. Admin downloads a zip or individual PDFs.

3. **Option C — Write to file:** Write the PDF to a temporary file on disk instead of `ByteArrayOutputStream`, then upload the file. This avoids holding the entire PDF in memory.

**Recommended:** Option C (write to temp file) for simplicity and reliability.

#### Acceptance Criteria
- [ ] PDF generation of 500 cards uses < 200MB peak memory
- [ ] No `OutOfMemoryError` with 1000+ cards
- [ ] Generated PDF is valid and printable
- [ ] PDF file is cleaned up from temp storage after upload

---

### OPT-05: Add Timeout to Remote Image Fetch in Renderer

**Priority:** P1 (Medium)
**Effort:** Low
**Risk:** Low

#### Current State
`IdCardRenderer.loadRemoteImage()` calls `ImageIO.read(URI(url).toURL())` with no timeout. A slow or unresponsive URL will block the entire generation indefinitely.

```
@server/.../idcard/IdCardRenderer.kt:174-181
private fun loadRemoteImage(url: String, w: Int, h: Int): BufferedImage {
    val img = ImageIO.read(java.net.URI(url).toURL())
    ...
}
```

#### Proposed Change
1. Use `java.net.URLConnection` with explicit `connectTimeout` and `readTimeout`.

```kotlin
private fun loadRemoteImage(url: String, w: Int, h: Int): BufferedImage {
    val connection = java.net.URI(url).toURL().openConnection()
    connection.connectTimeout = 10_000  // 10 seconds
    connection.readTimeout = 15_000     // 15 seconds
    val img = ImageIO.read(connection.getInputStream())
    ...
}
```

2. Alternatively, wrap the call in `runBlocking { withTimeout(15.seconds) { ... } }` if called from a suspend context.

#### Acceptance Criteria
- [ ] Image fetch times out after 15 seconds if the URL is unresponsive
- [ ] Timeout falls back to placeholder image (existing behavior)
- [ ] Generation continues for remaining cards after a timeout

---

### OPT-06: Add `app_user_id` Column to `NonTeachingStaffTable`

**Priority:** P1 (Medium)
**Effort:** Low
**Risk:** Low (additive migration)

#### Current State
`NonTeachingStaffTable` has no `userId`/`appUserId` FK. The staff digital ID card endpoint looks up staff by email match with the app user, which is fragile (case sensitivity, null emails, duplicate emails).

```
@server/.../idcard/IdCardService.kt:257-268
suspend fun getStaffIdByAppUserId(appUserId: UUID): UUID? = dbQuery {
    val email = AppUsersTable.selectAll()
        .where { AppUsersTable.id eq appUserId }
        .singleOrNull()
        ?.get(AppUsersTable.email) ?: return@dbQuery null

    NonTeachingStaffTable.selectAll()
        .where { (NonTeachingStaffTable.email eq email) and (NonTeachingStaffTable.isActive eq true) }
        .singleOrNull()
        ?.get(NonTeachingStaffTable.id)?.value
}
```

#### Proposed Change
1. **Migration:** Add `app_user_id` column to `non_teaching_staff` table.

```sql
-- migration_103_staff_app_user_id.sql
ALTER TABLE non_teaching_staff
ADD COLUMN IF NOT EXISTS app_user_id UUID REFERENCES app_users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_non_teaching_staff_app_user_id
ON non_teaching_staff(app_user_id)
WHERE app_user_id IS NOT NULL;
```

2. **Exposed table:** Add `val appUserId = uuid("app_user_id").nullable()` to `NonTeachingStaffTable`.

3. **Backfill:** Run a one-time script to match existing staff records to app users by email and populate `app_user_id`.

4. **Update lookup:** Replace email-based matching with direct UUID lookup.

```kotlin
suspend fun getStaffIdByAppUserId(appUserId: UUID): UUID? = dbQuery {
    NonTeachingStaffTable.selectAll()
        .where { (NonTeachingStaffTable.appUserId eq appUserId) and (NonTeachingStaffTable.isActive eq true) }
        .singleOrNull()
        ?.get(NonTeachingStaffTable.id)?.value
}
```

5. **Fallback:** Keep email-based matching as a fallback for records not yet backfilled.

#### Acceptance Criteria
- [ ] Migration applies cleanly on existing database
- [ ] Staff ID lookup works via `app_user_id` FK
- [ ] Email-based fallback is used when `app_user_id` is null
- [ ] Staff creation flow populates `app_user_id` when a linked app user exists

---

### OPT-07: Apply Template Config in Renderer

**Priority:** P2 (Low)
**Effort:** High
**Risk:** Medium

#### Current State
Templates store `frontConfig` and `backConfig` as JSON strings, but `IdCardRenderer` uses a completely hardcoded layout. The template config is never parsed or applied.

#### Proposed Change
1. Define a config schema:

```kotlin
@Serializable
data class CardTemplateConfig(
    val fields: List<String> = listOf("name", "role", "class", "school"),
    val backgroundColor: String = "#FFFFFF",
    val textColor: String = "#000000",
    val accentColor: String = "#2563EB",
    val showPhoto: Boolean = true,
    val showLogo: Boolean = true,
    val showQrOnFront: Boolean = false,
    val fontSize: FontSizeConfig = FontSizeConfig(),
)

@Serializable
data class FontSizeConfig(
    val title: Int = 32,
    val body: Int = 22,
    val small: Int = 18,
)
```

2. Parse `frontConfig`/`backConfig` in `generateCards()` and pass to `IdCardRenderer.CardData`.

3. Update `IdCardRenderer.renderFront()` and `renderBack()` to use config values for:
   - Field selection (which fields appear on the card)
   - Colors (background, text, accent)
   - Font sizes
   - Show/hide photo, logo, QR on front

4. Update `IdCardScreen` template creation UI to allow editing the config (field toggles, color pickers).

#### Acceptance Criteria
- [ ] Template config JSON is parsed and applied during rendering
- [ ] Changing `backgroundColor` in config changes the rendered card background
- [ ] Toggling `showPhoto` shows/hides the photo area
- [ ] Invalid config JSON falls back to defaults with a logged warning
- [ ] Admin UI allows editing config fields (not just raw JSON)

---

### OPT-08: Add Initial Delay to `IdCardExpiryCheckJob`

**Priority:** P2 (Low)
**Effort:** Trivial
**Risk:** None

#### Current State
The job runs immediately on server startup, then every 24 hours. Multiple server restarts cause redundant checks.

```
@server/.../idcard/IdCardExpiryCheckJob.kt:27-37
fun start() {
    scope.launch {
        while (true) {
            try {
                checkExpiringCards()
            } catch (e: Exception) {
                logger.warning("Expiry check failed: ${e.message}")
            }
            delay(24 * 60 * 60 * 1000L)
        }
    }
}
```

#### Proposed Change
Add an initial delay of 5 minutes before the first check, allowing the server to fully initialize.

```kotlin
fun start() {
    scope.launch {
        delay(5 * 60 * 1000L) // 5-minute initial delay
        while (true) {
            try {
                checkExpiringCards()
            } catch (e: Exception) {
                logger.warning("Expiry check failed: ${e.message}")
            }
            delay(24 * 60 * 60 * 1000L)
        }
    }
}
```

#### Acceptance Criteria
- [ ] First expiry check runs 5 minutes after server startup
- [ ] Subsequent checks run every 24 hours
- [ ] No duplicate checks from multiple startup cycles (idempotent)

---

### OPT-09: Add Pagination and Search to Admin Card List

**Priority:** P2 (Low)
**Effort:** Medium
**Risk:** Low

#### Current State
`IdCardScreen` shows only the first 20 cards with "... and N more" text. No search, no filter, no pagination.

```
@composeApp/.../screens/school/IdCardScreen.kt:248-276
state.cards.take(20).forEach { card -> ... }
if (state.cards.size > 20) {
    Text(text = "... and ${state.cards.size - 20} more", ...)
}
```

#### Proposed Change
1. Add a search bar to filter cards by name or person type.
2. Replace `take(20)` with a `LazyColumn` that loads all cards efficiently.
3. Add filter chips: All / Students / Teachers / Staff.
4. Add server-side pagination to `GET /api/v1/school/id-cards` with `?page=1&limit=50&search=query`.

```kotlin
// Server endpoint
get {
    val ctx = call.requireSchoolAdmin() ?: return@get
    val page = call.parameters["page"]?.toIntOrNull() ?: 1
    val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
    val search = call.parameters["search"]
    val cards = service.getCardsBySchool(ctx.schoolId, page, limit, search)
    call.ok(cards)
}
```

#### Acceptance Criteria
- [ ] Admin can search cards by person name
- [ ] Admin can filter cards by person type (student/teacher/staff)
- [ ] Card list uses `LazyColumn` for efficient rendering of 500+ items
- [ ] Server-side pagination returns 50 cards per page
- [ ] Scroll-to-load triggers next page fetch

---

### OPT-10: `IdCardExpiryCheckJob` — Group by School and Send Notifications

**Priority:** P2 (Low)
**Effort:** Medium
**Risk:** Low

#### Current State
The expiry check job logs warnings but doesn't group by school or send any notifications to admins.

```
@server/.../idcard/IdCardExpiryCheckJob.kt:36-47
private suspend fun checkExpiringCards() {
    val expiring = service.getExpiringCards(withinDays = 30)
    if (expiring.isEmpty()) return
    logger.info("ID Card Expiry Check: ${expiring.size} cards expiring within 30 days")
    for (card in expiring) {
        logger.info("ID Card expiring: ${card.personName} (${card.personType}) — valid till: ${card.validTill}")
    }
}
```

#### Proposed Change
1. Add `schoolId` to `getExpiringCards()` return data (join with `IdCardsTable.schoolId`).
2. Group expiring cards by `schoolId`.
3. For each school, send a notification to school admins via the existing notification system (PEWS or in-app notifications).

```kotlin
private suspend fun checkExpiringCards() {
    val expiring = service.getExpiringCards(withinDays = 30)
    if (expiring.isEmpty()) return

    val bySchool = expiring.groupBy { it.schoolId }
    for ((schoolId, cards) in bySchool) {
        logger.info("School $schoolId: ${cards.size} cards expiring within 30 days")
        notificationService.sendExpiryNotification(schoolId, cards)
    }
}
```

4. Add `schoolId` field to `IdCardDto` (server-side only, not exposed to client).

#### Acceptance Criteria
- [ ] Expiring cards are grouped by school
- [ ] Each school's admin receives a notification with the list of expiring cards
- [ ] Notification includes card holder name, type, and expiry date
- [ ] Job logs are structured by school for easy filtering

---

## 5. Implementation Priority

| ID | Title | Priority | Effort | Sprint |
|---|---|---|---|---|
| OPT-01 | Parallel image uploads | P0 | Medium | 1 |
| OPT-02 | Display card image in app | P0 | Low | 1 |
| OPT-03 | Client-side QR rendering | P1 | Medium | 2 |
| OPT-04 | Stream PDF generation | P1 | Medium | 2 |
| OPT-05 | Image fetch timeout | P1 | Low | 1 |
| OPT-06 | Staff `app_user_id` FK | P1 | Low | 2 |
| OPT-07 | Apply template config | P2 | High | 3 |
| OPT-08 | Expiry job initial delay | P2 | Trivial | 1 |
| OPT-09 | Card list pagination | P2 | Medium | 3 |
| OPT-10 | Expiry notifications | P2 | Medium | 3 |

---

## 6. Dependencies

### New Libraries

| Library | Module | Purpose | License |
|---|---|---|---|
| `io.github.g0dkar:qrcode-kotlin:4.1.1` | `shared` commonMain | Multiplatform QR code generation | Apache-2.0 |
| `io.coil-kt.coil3:coil-compose` | `composeApp` commonMain | Compose Multiplatform image loading | Apache-2.0 |

### Database Migrations

| Migration | Description |
|---|---|
| `migration_103_staff_app_user_id.sql` | Add `app_user_id` column to `non_teaching_staff` |

---

## 7. Testing Strategy

### Performance Tests
- Generate 500 cards in a test environment and measure total time.
- Monitor peak memory usage during generation with VisualVM or JFR.
- Verify no `OutOfMemoryError` with 1000 cards.

### UX Tests
- Open `DigitalIdCardScreen` on a slow 3G connection and verify image loads with placeholder.
- Scan QR code rendered on-screen with a physical phone camera.
- Verify PDF downloads and prints correctly on A4 paper.

### Regression Tests
- All existing ID card endpoints continue to work after changes.
- Staff lookup works for both `app_user_id` (new) and email fallback (old records).
- Template CRUD operations unaffected.
- Expiry job still runs and logs correctly with initial delay.

---

## 8. Acceptance Criteria Summary

- [ ] Bulk generation of 500 cards completes in < 60 seconds (OPT-01)
- [ ] Digital ID card screen displays the server-rendered card image (OPT-02)
- [ ] QR code is scannable from the device screen (OPT-03)
- [ ] PDF generation of 1000 cards uses < 200MB peak memory (OPT-04)
- [ ] Remote image fetch times out after 15 seconds (OPT-05)
- [ ] Staff ID lookup uses `app_user_id` FK with email fallback (OPT-06)
- [ ] Template config drives rendering output (OPT-07)
- [ ] Expiry job waits 5 minutes before first run (OPT-08)
- [ ] Admin card list supports search and pagination (OPT-09)
- [ ] Expiry notifications sent to school admins (OPT-10)
- [ ] All modules compile successfully
- [ ] No regressions in existing ID card flows

---

## 9. Rollback Plan

Each optimization is independently deployable:
- **OPT-01/04/05:** Server-only changes, rollback by redeploying previous server build.
- **OPT-02/03/09:** Client-only changes, rollback by reverting composeApp changes.
- **OPT-06:** Database migration is additive (nullable column), no rollback needed.
- **OPT-07/08/10:** Server-only changes, feature-flagged if needed.

---

## 10. Future Considerations (Out of Scope)

- **AI-powered card design suggestions** — Generate template configs from school branding.
- **NFC-based ID verification** — Tap physical card to verify identity.
- **Biometric card activation** — Activate digital card only after biometric verification.
- **Multi-language card rendering** — Support regional languages on card text.
- **Card revocation flow** — Admin can revoke a card (status = "revoked"), immediately invalidating the QR deep link.
