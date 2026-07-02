-- =====================================================================
-- migration_104_library.sql
-- Library Management (LIBRARY_MANAGEMENT_SPEC.md)
-- Creates 13 tables: library_books, library_book_copies, library_issues,
--   library_reservations, library_categories, library_settings,
--   library_audit_log, library_announcements, library_wishlist,
--   library_reading_goals, library_acquisition_requests,
--   library_reading_badges, library_book_discussions
-- Also: 28 indexes, seed data, librarian role addition
-- =====================================================================

-- =====================================================================
-- 1. library_books
-- =====================================================================
CREATE TABLE IF NOT EXISTS library_books (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id        UUID NOT NULL,
    isbn             VARCHAR(20),
    title            TEXT NOT NULL,
    author           TEXT,
    publisher        TEXT,
    category         VARCHAR(48),
    tags             TEXT[],
    total_copies     INTEGER NOT NULL DEFAULT 1,
    available_copies INTEGER NOT NULL DEFAULT 1,
    shelf_location   VARCHAR(32),
    cover_url        TEXT,
    replacement_cost DOUBLE PRECISION,
    series_name      VARCHAR(128),
    series_number    INTEGER,
    language         VARCHAR(8) NOT NULL DEFAULT 'en',
    is_archived      BOOLEAN NOT NULL DEFAULT false,
    synopsis         TEXT,
    page_count       INTEGER,
    deleted_at       TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
);

-- =====================================================================
-- 2. library_book_copies
-- =====================================================================
CREATE TABLE IF NOT EXISTS library_book_copies (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL,
    book_id     UUID NOT NULL REFERENCES library_books(id),
    copy_number INTEGER NOT NULL,
    barcode     VARCHAR(64),
    condition   VARCHAR(16) NOT NULL DEFAULT 'new',   -- new | good | fair | poor | damaged
    status      VARCHAR(16) NOT NULL DEFAULT 'available', -- available | issued | lost | repair
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(book_id, copy_number)
);

-- =====================================================================
-- 3. library_issues
-- =====================================================================
CREATE TABLE IF NOT EXISTS library_issues (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID NOT NULL,
    book_id           UUID NOT NULL REFERENCES library_books(id),
    copy_id           UUID REFERENCES library_book_copies(id),
    borrower_id       UUID NOT NULL,
    borrower_type     VARCHAR(16) NOT NULL,           -- student | teacher
    borrower_name     TEXT NOT NULL,
    issue_date        DATE NOT NULL,
    due_date          DATE NOT NULL,
    return_date       DATE,
    return_condition  VARCHAR(16),                    -- good | fair | damaged
    damage_notes      TEXT,
    renewal_count     INTEGER NOT NULL DEFAULT 0,
    fine_amount       DOUBLE PRECISION NOT NULL DEFAULT 0,
    fine_status       VARCHAR(16) NOT NULL DEFAULT 'none', -- none | pending | paid | waived
    fine_paid_at      TIMESTAMP,
    fine_waived_by    UUID,
    fine_waived_reason TEXT,
    status            VARCHAR(16) NOT NULL DEFAULT 'issued', -- issued | returned | lost
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now()
);

-- =====================================================================
-- 4. library_reservations
-- =====================================================================
CREATE TABLE IF NOT EXISTS library_reservations (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id        UUID NOT NULL,
    book_id          UUID NOT NULL REFERENCES library_books(id),
    reserved_by      UUID NOT NULL,
    reserved_by_name TEXT NOT NULL,
    reserved_by_type VARCHAR(16) NOT NULL DEFAULT 'student', -- student | teacher | parent
    status           VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending | notified | fulfilled | cancelled
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    fulfilled_at     TIMESTAMP
);

-- =====================================================================
-- 5. library_categories
-- =====================================================================
CREATE TABLE IF NOT EXISTS library_categories (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID NOT NULL,
    name          VARCHAR(48) NOT NULL,
    color         VARCHAR(7) NOT NULL DEFAULT '#6366f1',
    icon          VARCHAR(32) NOT NULL DEFAULT 'book',
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, name)
);

-- =====================================================================
-- 6. library_settings
-- =====================================================================
CREATE TABLE IF NOT EXISTS library_settings (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id                UUID NOT NULL,
    default_loan_days        INTEGER NOT NULL DEFAULT 14,
    fine_per_day             DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    max_books_per_student    INTEGER NOT NULL DEFAULT 3,
    max_renewals             INTEGER NOT NULL DEFAULT 2,
    reservation_timeout_days INTEGER NOT NULL DEFAULT 7,
    due_reminder_days        INTEGER NOT NULL DEFAULT 2,
    fine_cap_enabled         BOOLEAN NOT NULL DEFAULT true,
    quick_issue_enabled      BOOLEAN NOT NULL DEFAULT true,
    bulk_return_enabled      BOOLEAN NOT NULL DEFAULT true,
    featured_book_id         UUID,
    featured_type            VARCHAR(16),              -- WEEK | MONTH
    featured_updated_at      TIMESTAMP,
    leaderboard_enabled      BOOLEAN NOT NULL DEFAULT false,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id)
);

-- =====================================================================
-- 7. library_audit_log
-- =====================================================================
CREATE TABLE IF NOT EXISTS library_audit_log (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID NOT NULL,
    actor_id      UUID,
    actor_name    TEXT NOT NULL,
    action        VARCHAR(64) NOT NULL,
    entity_type   VARCHAR(32) NOT NULL,
    entity_id     UUID,
    metadata      JSONB,
    previous_state JSONB,
    new_state     JSONB,
    hash          VARCHAR(64) NOT NULL,               -- SHA-256 hash chain
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

-- =====================================================================
-- 8. library_announcements
-- =====================================================================
CREATE TABLE IF NOT EXISTS library_announcements (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id      UUID NOT NULL,
    title          TEXT NOT NULL,
    message        TEXT NOT NULL,
    audience       VARCHAR(16) NOT NULL DEFAULT 'all', -- all | students | parents
    created_by     UUID,
    created_by_name TEXT NOT NULL,
    expires_at     TIMESTAMP,
    is_active      BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now()
);

-- =====================================================================
-- 9. library_wishlist
-- =====================================================================
CREATE TABLE IF NOT EXISTS library_wishlist (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL,
    student_id  UUID NOT NULL,
    book_id     UUID NOT NULL REFERENCES library_books(id),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, student_id, book_id)
);

-- =====================================================================
-- 10. library_reading_goals
-- =====================================================================
CREATE TABLE IF NOT EXISTS library_reading_goals (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL,
    student_id  UUID NOT NULL,
    goal_count  INTEGER NOT NULL,
    period      VARCHAR(16) NOT NULL,                 -- monthly | quarterly | yearly
    target_year INTEGER NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, student_id, period, target_year)
);

-- =====================================================================
-- 11. library_acquisition_requests
-- =====================================================================
CREATE TABLE IF NOT EXISTS library_acquisition_requests (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID NOT NULL,
    requested_by      UUID NOT NULL,
    requested_by_name TEXT NOT NULL,
    requested_by_type VARCHAR(16) NOT NULL,           -- student | teacher | parent | librarian
    title             TEXT NOT NULL,
    author            TEXT,
    isbn              VARCHAR(20),
    publisher         TEXT,
    reason            TEXT,
    estimated_cost    DOUBLE PRECISION,
    status            VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending | approved | rejected | ordered | received
    approved_by       UUID,
    approved_at       TIMESTAMP,
    order_link        TEXT,
    ordered_at        TIMESTAMP,
    received_at       TIMESTAMP,
    converted_book_id UUID,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now()
);

-- =====================================================================
-- 12. library_reading_badges
-- =====================================================================
CREATE TABLE IF NOT EXISTS library_reading_badges (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL,
    student_id  UUID NOT NULL,
    badge_type  VARCHAR(32) NOT NULL,                 -- first_book | 5_books | 10_books | 25_books | 50_books | 100_books | speed_reader | genre_explorer | streak_7 | streak_30
    earned_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, student_id, badge_type)
);

-- =====================================================================
-- 13. library_book_discussions
-- =====================================================================
CREATE TABLE IF NOT EXISTS library_book_discussions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id    UUID NOT NULL,
    book_id      UUID NOT NULL REFERENCES library_books(id),
    student_id   UUID NOT NULL,
    student_name TEXT NOT NULL,
    message      TEXT NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMP,
    deleted_by   UUID
);

-- =====================================================================
-- INDEXES (28)
-- =====================================================================

-- library_books indexes
CREATE INDEX IF NOT EXISTS idx_library_books_school ON library_books(school_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_library_books_category ON library_books(school_id, category);
CREATE INDEX IF NOT EXISTS idx_library_books_isbn ON library_books(school_id, isbn) WHERE isbn IS NOT NULL;

-- library_book_copies indexes
CREATE INDEX IF NOT EXISTS idx_library_copies_book ON library_book_copies(book_id, status);
CREATE INDEX IF NOT EXISTS idx_library_copies_barcode ON library_book_copies(school_id, barcode) WHERE barcode IS NOT NULL;

-- library_issues indexes
CREATE INDEX IF NOT EXISTS idx_library_issues_borrower ON library_issues(borrower_id, status);
CREATE INDEX IF NOT EXISTS idx_library_issues_book ON library_issues(book_id, status);
CREATE INDEX IF NOT EXISTS idx_library_issues_school_status ON library_issues(school_id, status);
CREATE INDEX IF NOT EXISTS idx_library_issues_overdue ON library_issues(school_id, due_date) WHERE status = 'issued';
CREATE INDEX IF NOT EXISTS idx_library_issues_due_date ON library_issues(due_date) WHERE status = 'issued';

-- library_reservations indexes
CREATE INDEX IF NOT EXISTS idx_library_reservations_book ON library_reservations(book_id, status);
CREATE INDEX IF NOT EXISTS idx_library_reservations_user ON library_reservations(reserved_by, status);

-- library_categories indexes
CREATE INDEX IF NOT EXISTS idx_library_categories_school ON library_categories(school_id, display_order);

-- library_audit_log indexes
CREATE INDEX IF NOT EXISTS idx_library_audit_school ON library_audit_log(school_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_library_audit_entity ON library_audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_library_audit_actor ON library_audit_log(actor_id, created_at DESC);

-- library_announcements indexes
CREATE INDEX IF NOT EXISTS idx_library_announcements_school ON library_announcements(school_id, is_active, expires_at);

-- library_wishlist indexes
CREATE INDEX IF NOT EXISTS idx_library_wishlist_student ON library_wishlist(student_id);
CREATE INDEX IF NOT EXISTS idx_library_wishlist_school ON library_wishlist(school_id);

-- library_reading_goals indexes
CREATE INDEX IF NOT EXISTS idx_library_reading_goals_student ON library_reading_goals(student_id, target_year);

-- library_acquisition_requests indexes
CREATE INDEX IF NOT EXISTS idx_library_acquisition_school ON library_acquisition_requests(school_id, status);
CREATE INDEX IF NOT EXISTS idx_library_acquisition_requester ON library_acquisition_requests(requested_by);

-- library_reading_badges indexes
CREATE INDEX IF NOT EXISTS idx_library_badges_student ON library_reading_badges(student_id);

-- library_book_discussions indexes
CREATE INDEX IF NOT EXISTS idx_library_discussions_book ON library_book_discussions(book_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_library_discussions_school ON library_book_discussions(school_id);

-- GIN indexes for full-text search and tags (Postgres only)
-- These are created separately because SQLite doesn't support GIN
DO $$
BEGIN
    -- Full-text search index on title + author
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_library_books_search ON library_books USING gin(to_tsvector(''english'', coalesce(title, '''') || '' '' || coalesce(author, '''')))';
    -- Tags array index
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_library_books_tags ON library_books USING gin(tags)';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'GIN index creation skipped: %', SQLERRM;
END $$;

-- =====================================================================
-- SEED DATA
-- =====================================================================

-- Seed default categories for existing schools
INSERT INTO library_categories (school_id, name, color, icon, display_order)
SELECT s.id, c.name, c.color, c.icon, c.display_order
FROM schools s
CROSS JOIN (VALUES
    ('Fiction', '#6366f1', 'book', 1),
    ('Science', '#10b981', 'science', 2),
    ('History', '#f59e0b', 'history', 3),
    ('Mathematics', '#3b82f6', 'calculate', 4),
    ('Literature', '#8b5cf6', 'menu_book', 5),
    ('Reference', '#ef4444', 'library_books', 6),
    ('Biography', '#ec4899', 'person', 7),
    ('Children', '#14b8a6', 'child_care', 8)
) AS c(name, color, icon, display_order)
WHERE NOT EXISTS (
    SELECT 1 FROM library_categories lc WHERE lc.school_id = s.id AND lc.name = c.name
);

-- Seed default library_settings for existing schools
INSERT INTO library_settings (school_id)
SELECT s.id
FROM schools s
WHERE NOT EXISTS (
    SELECT 1 FROM library_settings ls WHERE ls.school_id = s.id
);

-- =====================================================================
-- ROLLBACK (run in reverse order if needed)
-- =====================================================================
-- DROP TABLE IF EXISTS library_book_discussions;
-- DROP TABLE IF EXISTS library_reading_badges;
-- DROP TABLE IF EXISTS library_acquisition_requests;
-- DROP TABLE IF EXISTS library_reading_goals;
-- DROP TABLE IF EXISTS library_wishlist;
-- DROP TABLE IF EXISTS library_announcements;
-- DROP TABLE IF EXISTS library_audit_log;
-- DROP TABLE IF EXISTS library_settings;
-- DROP TABLE IF EXISTS library_categories;
-- DROP TABLE IF EXISTS library_reservations;
-- DROP TABLE IF EXISTS library_issues;
-- DROP TABLE IF EXISTS library_book_copies;
-- DROP TABLE IF EXISTS library_books;
