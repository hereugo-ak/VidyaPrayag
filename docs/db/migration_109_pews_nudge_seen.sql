-- Migration 109: pews_nudge_seen
-- Tracks when a parent has viewed the PEWS nudge for a child on a given run date.
-- Once a row exists, the nudge card is no longer shown for that (child, parent, run_date).
-- This ensures the parent sees the nudge once, reads the explanation, and the card
-- disappears instead of persisting indefinitely.

CREATE TABLE IF NOT EXISTS pews_nudge_seen (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id          UUID NOT NULL,
    parent_id         UUID NOT NULL,
    snapshot_run_date DATE NOT NULL,
    seen_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(child_id, parent_id, snapshot_run_date)
);

-- Index for the lookup: "has this parent seen this child's nudge for this run date?"
CREATE INDEX IF NOT EXISTS idx_pews_nudge_seen
    ON pews_nudge_seen(child_id, parent_id, snapshot_run_date);
