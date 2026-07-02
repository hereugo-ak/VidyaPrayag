-- ──────────────────────────────────────────────────────────────────────────────
-- Migration 105: Materialized view for library trending books (spec §17)
--
-- Pre-computes issue counts per book over the last 30 days so the trending
-- endpoint doesn't need to scan library_issues on every request.
-- Refreshed hourly by LibraryJobScheduler.runTrendingRefresh().
--
-- Run manually in Supabase SQL Editor (AUTO_CREATE_TABLES is OFF in prod).
-- ──────────────────────────────────────────────────────────────────────────────

CREATE MATERIALIZED VIEW IF NOT EXISTS library_trending_mv AS
SELECT
    i.school_id,
    i.book_id,
    COUNT(*)::int AS issue_count_30d
FROM library_issues i
WHERE i.created_at >= NOW() - INTERVAL '30 days'
GROUP BY i.school_id, i.book_id;

-- Unique index so we can REFRESH CONCURRENTLY (no read lock)
CREATE UNIQUE INDEX IF NOT EXISTS idx_library_trending_mv_school_book
    ON library_trending_mv (school_id, book_id);

-- Index for fast lookup by school
CREATE INDEX IF NOT EXISTS idx_library_trending_mv_school
    ON library_trending_mv (school_id);
