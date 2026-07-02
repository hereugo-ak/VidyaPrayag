# Library Management — QA Manual Test Scenarios

> **Status:** Ready for manual execution
> **Spec reference:** `LIBRARY_MANAGEMENT_SPEC.md` §QA-001 to QA-030
> **Pre-requisites:** Server running, test school provisioned, at least one librarian/admin account

---

## QA-001: Librarian adds a book with 5 copies
- **Steps:** Library → Add Book → fill title, author, ISBN, 5 copies → Save
- [ ] **Expected:** Book created; 5 copies auto-generated with barcodes; visible in catalog

## QA-002: Librarian issues book to student
- **Steps:** Library → search book → Issue → select student → confirm
- [ ] **Expected:** Issue created; available_copies decremented; due date shown

## QA-003: Librarian uses quick issue
- **Steps:** Quick Issue → scan barcode → select borrower → Issue
- [ ] **Expected:** Instant issue; toast with due date; input re-focuses

## QA-004: Librarian processes bulk return
- **Steps:** Bulk Return → scan 5 barcodes sequentially → End Session
- [ ] **Expected:** 5 returns processed; session summary with total fines

## QA-005: Student renews own book
- **Steps:** Student app → My Books → Renew
- [ ] **Expected:** New due date shown; renewal count incremented

## QA-006: Student tries to renew max-renewed book
- **Steps:** Student app → My Books → Renew on book with max renewals (2)
- [ ] **Expected:** Error: "Max renewals reached"

## QA-007: Parent reserves unavailable book
- **Steps:** Parent portal → Search → click unavailable book → Reserve
- [ ] **Expected:** Reservation created; waitlist position shown

## QA-008: Librarian returns damaged book
- **Steps:** Library → find active issue → Return → condition: damaged → notes
- [ ] **Expected:** Copy status=repair; available_copies NOT incremented

## QA-009: Librarian marks book as lost
- **Steps:** Library → active issue → Mark Lost
- [ ] **Expected:** Fine=replacement_cost; copy status=lost; total_copies decremented

## QA-010: Librarian imports 100 books via CSV
- **Steps:** Library → Import → upload CSV → wait
- [ ] **Expected:** Progress bar; success/failure count; errors with row numbers

## QA-011: Admin configures per-school settings
- **Steps:** Library → Settings → change loan days to 21 → Save
- [ ] **Expected:** New issues use 21-day loan; existing issues unchanged

## QA-012: Librarian creates and reorders categories
- **Steps:** Library → Categories → create 3 → drag to reorder
- [ ] **Expected:** Categories displayed in new order on search screen

## QA-013: Librarian posts announcement
- **Steps:** Library → Announcements → create → save
- [ ] **Expected:** Announcement visible on student/parent library home

## QA-014: Student sets reading goal of 20 books
- **Steps:** Student app → Profile → Set Goal → 20
- [ ] **Expected:** Progress bar shows 0/20; updates as books returned

## QA-015: Student adds book to wishlist
- **Steps:** Student app → Search → book → Add to Wishlist
- [ ] **Expected:** Book in wishlist; max 50 enforced

## QA-016: Librarian exports catalog
- **Steps:** Library → Export → Catalog → CSV
- [ ] **Expected:** CSV downloaded with all books

## QA-017: Teacher submits acquisition request
- **Steps:** Library → Request Book → fill form → submit
- [ ] **Expected:** Request created with status=pending; librarian notified

## QA-018: Librarian approves and converts acquisition request
- **Steps:** Library → Requests → approve → Convert to Book
- [ ] **Expected:** Book entry created from request; requester notified

## QA-019: Librarian archives old-edition book
- **Steps:** Library → book → Archive
- [ ] **Expected:** Book hidden from search; visible in admin with "Archived" badge

## QA-020: Librarian searches with multiple filters
- **Steps:** Library → search "science" → filter: category=Science, language=en, sort=popularity
- [ ] **Expected:** Filtered results matching all criteria

## QA-021: Admin runs onboarding wizard
- **Steps:** First-time Library access → Onboarding → complete all steps
- [ ] **Expected:** Categories seeded; settings configured; ready to use

## QA-022: Student views trending books
- **Steps:** Student app → Library home
- [ ] **Expected:** Trending carousel with top 10 books and "Trending" badge

## QA-023: Librarian views audit trail
- **Steps:** Library → Audit → filter by action=ISSUE, date=today
- [ ] **Expected:** All issues from today listed with actor and details

## QA-024: Two librarians issue same copy simultaneously
- **Steps:** Both scan same barcode → both click Issue
- [ ] **Expected:** First succeeds; second gets COPY_ALREADY_ISSUED error

## QA-025: Student views own library profile
- **Steps:** Student app → Profile
- [ ] **Expected:** Active issues, fines, total read, categories, reading goal

## QA-026: Librarian uploads book cover
- **Steps:** Library → book → Upload Cover → select image
- [ ] **Expected:** Cover displayed on search results and book detail

## QA-027: Dark mode toggle
- **Steps:** Switch to dark mode
- [ ] **Expected:** All library screens render correctly in dark theme

## QA-028: Offline browsing
- **Steps:** Disable network → open library search
- [ ] **Expected:** Cached catalog browsable; offline banner shown

## QA-029: Empty state verification
- **Steps:** New school with no books → open Library
- [ ] **Expected:** "Your library is empty" with Add Books CTA

## QA-030: Keyboard shortcuts (desktop)
- **Steps:** Press Ctrl+K
- [ ] **Expected:** Search bar focused; Ctrl+I opens quick issue

---

## Test Execution Summary

| Field | Value |
|---|---|
| Tester | |
| Date | |
| Build / Commit | |
| Environment | |
| Pass count | __ / 30 |
| Fail count | __ / 30 |
| Blocked count | __ / 30 |
| Notes | |
