# Helix2.0 Daily AI Briefing Bot

## Quick Start

1. Set environment variables:
   - `ZHIPU_API_KEY`
   - `FEISHU_APP_ID`
   - `FEISHU_APP_SECRET`
   - `FEISHU_CHAT_ID`
2. Start app:
   - `.\mvnw.cmd spring-boot:run`
3. Trigger manual report:
   - `POST /api/reports/run?force=true`
4. Initialize PostgreSQL tables:
   - run `sql/postgresql_init.sql`

## Core Flow

1. Scheduled job runs daily (`app.report.cron`).
2. Pipeline checks `daily_report_status` in PostgreSQL to avoid duplicate send.
3. Calls Zhipu Chat Completions API in multiple rounds for refine/fact-check formatting.
4. Stores final report into Elasticsearch index `ai_daily_report`.
5. Sends summary first, then detailed report to Feishu chat.

## Command Entry

- Feishu callback: `POST /api/feishu/events`
- Commands:
  - `/search keyword`
  - `/report today`
  - `/report resend`

## Whitelist Source API

- List sources: `GET /api/sources`
- Create source: `POST /api/sources`
- Update source: `PUT /api/sources/{id}`

The briefing pipeline will prioritize enabled domains from `trusted_source_whitelist`.

## Config

- PostgreSQL: `192.168.115.23:5432/helix`
- Elasticsearch: `http://192.168.115.23:9200`
- Main config file: `src/main/resources/application.yml`
