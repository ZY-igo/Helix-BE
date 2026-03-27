package com.sipc115.helix.service;

import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class BriefingPromptBuilder {

    public String buildSystemPrompt(LocalDate date, int maxItemsPerSection) {
        return """
                You are an AI engineering editor for a software development team.
                Today is %s.

                Scope goal:
                - Focus on developer-facing AI engineering topics: Agents, Agent runtime/orchestration, MCP, Skills, tool-calling, memory, planning, guardrails, eval, and production integration patterns.
                - Content must help real software development and system implementation.
                - You are NOT restricted by any domain whitelist.

                Hard constraints:
                1) Every item must be truthful, verifiable, and useful.
                2) Prefer primary sources (official docs, papers, release notes, standards, authoritative reports).
                3) URL must be a direct, valid, and accessible http/https link.
                4) If evidence is weak or uncertain, remove the item.
                5) Output JSON only (no markdown, no code block).
                6) Max %d items per section.
                7) Each item must include: title, source, url, coreContent, functionImpact.
                8) source must be a concise domain name (e.g., arxiv.org, openai.com).
                9) Exclude generic AI news, opinion posts, social chatter, marketing content, and non-engineering trend reports.
                10) Exclude topics not aligned with agent engineering (e.g., generic vision-model updates, generic model training framework updates like PyTorch unless directly tied to Agent/MCP/Skill implementation).
                """.formatted(date, maxItemsPerSection);
    }

    public String buildRetrievePrompt(LocalDate date) {
        return """
                Task: Build today's developer-oriented AI briefing in exactly 3 sections:
                - hotSignals: high-impact engineering signals related to Agent/MCP/Skill ecosystem
                - latestUpdates: recent technical updates from official docs/specs/repos relevant to Agent engineering
                - classicInsights: evergreen implementation knowledge for building production-grade agents

                Return JSON with this structure:
                {
                  "headline":"string",
                  "hotSignals":[{"title":"","source":"","url":"","coreContent":"","functionImpact":""}],
                  "latestUpdates":[...],
                  "classicInsights":[...]
                }

                Requirements:
                - coreContent: key facts and essence (<= 120 chars).
                - functionImpact: practical implementation value for developers/architects (<= 80 chars).
                - No role segmentation or business-direction tags.
                - Do NOT include pure news digest items; each item must contain actionable technical insight.

                Today's date: %s
                """.formatted(date);
    }

    public String buildOptimizePrompt(int round, String previousJson) {
        return """
                Optimization round %d:
                1) Remove low-value, speculative, unverifiable, or duplicated items.
                2) Keep only items with reliable evidence and clear implementation value for Agent/MCP/Skill engineering.
                3) Keep coreContent <= 120 chars and functionImpact <= 80 chars.
                4) Ensure all links are direct and accessible.
                5) Keep the 3-section schema unchanged.
                6) Remove generic AI news or topics outside the target scope.

                Previous JSON:
                %s
                """.formatted(round, previousJson);
    }

    public String buildFormattingPrompt(String optimizedJson) {
        return """
                Normalize the JSON below:
                1) Keep all required fields and field names unchanged.
                2) headline <= 40 chars.
                3) URL must be valid http/https.
                4) If a section is empty, return [] instead of removing the field.

                Input:
                %s
                """.formatted(optimizedJson);
    }

    public String buildDeduplicatePrompt(String candidateJson, java.util.List<String> duplicatedUrls) {
        return """
                Deduplication task:
                The following URLs already exist in historical reports and MUST be removed:
                %s

                Please revise the briefing JSON:
                - Remove items whose url exactly matches any URL above.
                - Keep JSON schema unchanged.
                - If a section becomes empty, keep it as [].

                Current JSON:
                %s
                """.formatted(String.join("\n", duplicatedUrls), candidateJson);
    }

    public String buildQualityAuditPrompt(String candidateJson) {
        return """
                You are a quality auditor.
                Audit the following briefing JSON and output only JSON:
                {
                  "score": 0-100,
                  "decision": "pass|revise",
                  "issues": ["..."],
                  "briefing": { ...revised full briefing JSON... }
                }

                Scoring dimensions:
                - factual reliability
                - source quality
                - link accessibility
                - practical implementation value
                - relevance to Agent/MCP/Skill engineering
                - conciseness and structural correctness

                Candidate JSON:
                %s
                """.formatted(candidateJson);
    }

    public String buildTranslateToChinesePrompt(String finalJson) {
        return """
                Translation task:
                Translate the briefing JSON to Chinese while keeping EXACTLY the same JSON schema and field names.

                Rules:
                1) Translate headline/title/coreContent/functionImpact into Chinese.
                2) For key concepts or professional terms, keep Chinese first and append English term in parentheses.
                3) Keep source and url unchanged.
                4) Output JSON only.
                5) Do not add or remove fields.

                Input JSON:
                %s
                """.formatted(finalJson);
    }
}
