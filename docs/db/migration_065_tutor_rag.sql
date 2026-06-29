-- =============================================================
-- migration_065_tutor_rag.sql
-- AI Tutor 2.0 — RAG knowledge chunks (specced now, populated Phase 5)
--
-- This table is INERT until Phase 5. The retrieveKnowledge tool reads it,
-- but it will be empty until the NCERT corpus ingestion pipeline lands.
-- pgvector extension is required for the embedding column; if not installed,
-- the table can be created without it and the column added later.
-- =============================================================

-- Create pgvector extension if available (safe no-op if already exists)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS tutor_knowledge_chunks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID REFERENCES schools(id),   -- null = shared NCERT corpus
    source      VARCHAR(128) NOT NULL DEFAULT 'NCERT',
    board       VARCHAR(32) NOT NULL DEFAULT 'CBSE',
    class_label VARCHAR(16) NOT NULL,
    subject     VARCHAR(64) NOT NULL,
    topic_id    UUID REFERENCES curriculum_units(id),
    chunk_text  TEXT NOT NULL DEFAULT '',
    embedding   vector(768),                    -- pgvector; populated when RAG lands
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_tutor_kc_school_board_class ON tutor_knowledge_chunks(school_id, board, class_label, subject);
CREATE INDEX IF NOT EXISTS idx_tutor_kc_topic ON tutor_knowledge_chunks(topic_id);
-- IVFFlat index for cosine similarity search (created when data is populated in Phase 5)
-- CREATE INDEX IF NOT EXISTS idx_tutor_kc_embedding ON tutor_knowledge_chunks USING ivfflat(embedding vector_cosine_ops) WITH (lists = 100);
