-- 1) Create database (run by superuser)
-- CREATE DATABASE helix WITH ENCODING 'UTF8';
-- \c helix

-- 2) Daily report status (one row per day)
CREATE TABLE IF NOT EXISTS daily_report_status (
    id BIGSERIAL PRIMARY KEY,
    report_date DATE NOT NULL UNIQUE,
    sent BOOLEAN NOT NULL DEFAULT FALSE,
    es_document_id VARCHAR(128),
    summary_message_id VARCHAR(128),
    detail_message_id VARCHAR(128),
    sent_at TIMESTAMP,
    error_message VARCHAR(1200),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_daily_report_status_report_date
    ON daily_report_status (report_date DESC);

-- 3) Trusted whitelist sources (AI should prioritize these domains)
CREATE TABLE IF NOT EXISTS trusted_source_whitelist (
    id BIGSERIAL PRIMARY KEY,
    domain VARCHAR(255) NOT NULL UNIQUE,
    source_name VARCHAR(255) NOT NULL,
    source_type VARCHAR(32) NOT NULL DEFAULT 'official',
    priority INT NOT NULL DEFAULT 100,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trusted_source_enabled_priority
    ON trusted_source_whitelist (enabled, priority DESC, domain ASC);

-- 4) Run-level trace for each report generation
CREATE TABLE IF NOT EXISTS daily_report_run (
    id BIGSERIAL PRIMARY KEY,
    report_date DATE NOT NULL,
    run_status VARCHAR(32) NOT NULL,
    rounds_planned INT NOT NULL,
    rounds_executed INT NOT NULL DEFAULT 0,
    final_score INT,
    model_name VARCHAR(128),
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMP,
    error_message VARCHAR(1200)
);

CREATE INDEX IF NOT EXISTS idx_daily_report_run_report_date
    ON daily_report_run (report_date DESC, started_at DESC);

-- 5) Round-level trace for quality optimization loop
CREATE TABLE IF NOT EXISTS daily_report_round_trace (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES daily_report_run(id) ON DELETE CASCADE,
    round_no INT NOT NULL,
    stage VARCHAR(32) NOT NULL, -- retrieve/optimize/format/audit
    score INT,
    prompt_text TEXT,
    output_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_round_trace_run_round
    ON daily_report_round_trace (run_id, round_no);

-- 6) Optional: seed trusted domains
INSERT INTO trusted_source_whitelist (domain, source_name, source_type, priority, enabled, notes)
VALUES
    ('platform.openai.com', 'OpenAI Platform', 'official', 100, TRUE, 'default seed'),
    ('ai.google.dev', 'Google AI Developers', 'official', 99, TRUE, 'default seed'),
    ('docs.anthropic.com', 'Anthropic Docs', 'official', 98, TRUE, 'default seed'),
    ('huggingface.co', 'Hugging Face', 'official', 97, TRUE, 'default seed'),
    ('github.com', 'GitHub', 'official', 96, TRUE, 'default seed'),
    ('info.arxiv.org', 'arXiv API', 'official', 95, TRUE, 'default seed'),
    ('arxiv.org', 'arXiv', 'official', 94, TRUE, 'default seed'),
    ('semanticscholar.org', 'Semantic Scholar', 'official', 93, TRUE, 'default seed')
ON CONFLICT (domain) DO NOTHING;
