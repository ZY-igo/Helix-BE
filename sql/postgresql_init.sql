-- 1) Create database (run by superuser)
-- CREATE DATABASE helix WITH ENCODING 'UTF8';
-- \c helix

-- 2) Task metadata
DROP TABLE IF EXISTS task;
CREATE TABLE IF NOT EXISTS task (
    task_id CHAR(8) PRIMARY KEY,
    task_intro VARCHAR(1000) NOT NULL,
    session_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    execution_cycle VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO task (task_id, task_intro, session_ids, execution_cycle, password)
VALUES (1, 'legacy default task', '[]'::jsonb, 'TODO', NULL)
ON CONFLICT (task_id) DO NOTHING;

-- 3) Prompt definitions
CREATE TABLE IF NOT EXISTS prompt (
    id BIGSERIAL PRIMARY KEY,
    prompt_content TEXT NOT NULL,
    task_id CHAR(8) NOT NULL REFERENCES task(task_id) ON DELETE CASCADE,
    tag VARCHAR(64) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_prompt_task_order
    ON prompt (task_id, tag);

CREATE INDEX IF NOT EXISTS idx_prompt_task_id
    ON prompt (task_id, tag);

-- 4) Daily report status (one row per task per cycle)
CREATE TABLE IF NOT EXISTS daily_report_status (
    id BIGSERIAL PRIMARY KEY,
    task_id CHAR(8) NOT NULL REFERENCES task(task_id),
    report_date DATE NOT NULL,
    sent BOOLEAN NOT NULL DEFAULT FALSE,
    es_document_id VARCHAR(128),
    summary_message_id VARCHAR(128),
    detail_message_id VARCHAR(128),
    sent_at TIMESTAMP,
    error_message VARCHAR(1200),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE daily_report_status
    ADD COLUMN IF NOT EXISTS task_id BIGINT;
