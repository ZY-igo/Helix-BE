package com.sipc115.helix.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.config.BotProperties;
import com.sipc115.helix.model.dto.BriefingItem;
import com.sipc115.helix.model.dto.DailyBriefing;
import com.sipc115.helix.repository.es.AiDailyReportRepository;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class ZhipuBriefingService {

    private static final Logger log = LoggerFactory.getLogger(ZhipuBriefingService.class);

    private final BotProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final BriefingPromptBuilder promptBuilder;
    private final AiDailyReportRepository reportRepository;

    public ZhipuBriefingService(
            BotProperties properties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            BriefingPromptBuilder promptBuilder,
            AiDailyReportRepository reportRepository) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.promptBuilder = promptBuilder;
        this.reportRepository = reportRepository;
        this.restClient = restClientBuilder
                .baseUrl(properties.getZhipu().getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // Full AI workflow: retrieve -> optimize -> formatting -> deduplicate -> audit.
    public DailyBriefing generate(LocalDate date) {
        log.info("[AI] step=1/6 start briefing generation, date={}, next=build system prompt", date);
        validateConfig();

        String systemPrompt = promptBuilder.buildSystemPrompt(
                date,
                properties.getReport().getMaxItemsPerSection()
        );
        log.info("[AI] step=2/6 system prompt ready, next=retrieve candidates");
        log.info("[AI] system prompt built, chars={}", systemPrompt.length());
        log.debug("[AI] system prompt:\n{}", systemPrompt);

        String current = callModel("retrieve", systemPrompt, promptBuilder.buildRetrievePrompt(date));
        int rounds = properties.getReport().getLoopRounds();
        for (int i = 2; i <= rounds; i++) {
            log.info("[AI] optimize round={}/{}, next=apply optimization prompt", i, rounds);
            current = callModel("optimize-" + i, systemPrompt, promptBuilder.buildOptimizePrompt(i, current));
        }

        log.info("[AI] step=3/6 normalize JSON format, next=deduplicate by ES");
        current = callModel("formatting", systemPrompt, promptBuilder.buildFormattingPrompt(current));

        log.info("[AI] step=4/6 check duplicates against ES history");
        current = deduplicateByHistoricalUrls(systemPrompt, current);

        log.info("[AI] step=5/7 run quality audit loop");
        int auditLoop = properties.getReport().getAuditLoops();
        for (int i = 0; i < auditLoop; i++) {
            QualityAuditResult audit = runQualityAudit(systemPrompt, current);
            log.info("[AI][audit-{}] score={}, decision={}, next={}", i + 1, audit.score(), audit.decision(),
                    (audit.score() >= 85 || "pass".equalsIgnoreCase(audit.decision())) ? "parse briefing" : "revise and audit again");
            current = audit.briefingJson();
            if (audit.score() >= 85 || "pass".equalsIgnoreCase(audit.decision())) {
                break;
            }
        }

        log.info("[AI] step=6/7 translate final briefing JSON to Chinese");
        String translated = callModel("translate-zh", systemPrompt, promptBuilder.buildTranslateToChinesePrompt(current));

        log.info("[AI] step=7/7 parse final JSON to DTO");
        DailyBriefing briefing = parseBriefing(translated);
        briefing.setDate(date);
        log.info("[AI] completed, headline={}, remaining=send to formatter and pipeline", briefing.getHeadline());
        return briefing;
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getZhipu().getApiKey())) {
            throw new IllegalStateException("app.zhipu.api-key is empty.");
        }
    }

    // Sends one prompt to Zhipu and returns the assistant content text.
    private String callModel(String stage, String systemPrompt, String userPrompt) {
        log.info("[AI][{}] ask model, next=call /chat/completions", stage);
        log.info("[AI][{}] prompt chars={}", stage, userPrompt == null ? 0 : userPrompt.length());
        log.debug("[AI][{}] user prompt:\n{}", stage, userPrompt);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", properties.getZhipu().getModel());
        payload.put("messages", List.of(
                message("system", systemPrompt),
                message("user", userPrompt)));
        payload.put("temperature", properties.getZhipu().getTemperature());
        payload.put("max_tokens", stageMaxTokens(stage));
        payload.put("thinking", Map.of("type", properties.getZhipu().getThinking()));
        payload.put("response_format", Map.of("type", "json_object"));

        int maxAttempts = properties.getZhipu().getMaxRetries() + 1;
        String responseText = null;
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                responseText = restClient.post()
                        .uri("/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getZhipu().getApiKey())
                        .body(payload)
                        .retrieve()
                        .body(String.class);
                lastException = null;
                break;
            } catch (ResourceAccessException e) {
                lastException = e;
                boolean canRetry = attempt < maxAttempts && isNetworkTimeout(e);
                if (!canRetry) {
                    throw e;
                }
                long sleepMs = backoffMs(attempt);
                log.warn("[AI][{}] request timeout, retry {}/{}, backoff={}ms, cause={}",
                        stage, attempt, maxAttempts, sleepMs, e.getMessage());
                sleepQuietly(sleepMs);
            } catch (RestClientResponseException e) {
                lastException = e;
                boolean canRetry = attempt < maxAttempts && isRetriableStatus(e.getStatusCode().value());
                if (!canRetry) {
                    throw e;
                }
                long sleepMs = backoffMs(attempt);
                log.warn("[AI][{}] http status={}, retry {}/{}, backoff={}ms",
                        stage, e.getStatusCode().value(), attempt, maxAttempts, sleepMs);
                sleepQuietly(sleepMs);
            }
        }

        if (lastException != null) {
            throw new IllegalStateException("Zhipu API request failed after retries: " + lastException.getMessage(), lastException);
        }

        if (!StringUtils.hasText(responseText)) {
            throw new IllegalStateException("Zhipu API returned empty response.");
        }
        log.info("[AI][{}] raw response chars={}", stage, responseText.length());
        log.debug("[AI][{}] raw response:\n{}", stage, responseText);

        JsonNode response = parseJsonResponse(responseText);
        String output = extractOutputText(response);
        if (!StringUtils.hasText(output)) {
            throw new IllegalStateException("Zhipu API returned empty content.");
        }
        log.info("[AI][{}] model answer chars={}", stage, output.length());
        log.debug("[AI][{}] model answer:\n{}", stage, output);
        return output;
    }

    private int stageMaxTokens(String stage) {
        int configured = properties.getZhipu().getMaxTokens();
        if ("retrieve".equals(stage)) {
            return Math.min(configured, 4000);
        }
        if (stage != null && stage.startsWith("optimize-")) {
            return Math.min(configured, 3000);
        }
        if ("formatting".equals(stage) || "deduplicate".equals(stage) || "audit".equals(stage) || "translate-zh".equals(stage)) {
            return Math.min(configured, 2000);
        }
        return configured;
    }

    private Map<String, Object> message(String role, String text) {
        return Map.of("role", role, "content", text);
    }

    // Remove items whose URLs are already present in historical reports.
    private String deduplicateByHistoricalUrls(String systemPrompt, String candidateJson) {
        DailyBriefing candidate = parseBriefing(candidateJson);
        List<String> duplicateUrls = findDuplicateUrls(candidate);
        if (duplicateUrls.isEmpty()) {
            log.info("[AI][dedup] no duplicate URL found in ES, next=audit");
            return candidateJson;
        }

        log.info("[AI][dedup] duplicate URLs detected count={}, next=ask model to remove duplicates", duplicateUrls.size());
        String dedupPrompt = promptBuilder.buildDeduplicatePrompt(candidateJson, duplicateUrls);
        return callModel("deduplicate", systemPrompt, dedupPrompt);
    }

    private List<String> findDuplicateUrls(DailyBriefing briefing) {
        Set<String> urls = new LinkedHashSet<>();
        addUrls(urls, briefing.getHotSignals());
        addUrls(urls, briefing.getLatestUpdates());
        addUrls(urls, briefing.getClassicInsights());

        List<String> duplicates = new ArrayList<>();
        for (String url : urls) {
            boolean exists = reportRepository.existsBySummaryTextContainingOrDetailTextContaining(url, url);
            if (exists) {
                duplicates.add(url);
            }
        }
        return duplicates;
    }

    private void addUrls(Set<String> urls, List<BriefingItem> items) {
        for (BriefingItem item : items) {
            if (StringUtils.hasText(item.getUrl())) {
                urls.add(item.getUrl().trim());
            }
        }
    }

    private QualityAuditResult runQualityAudit(String systemPrompt, String candidateJson) {
        String auditRaw = callModel("audit", systemPrompt, promptBuilder.buildQualityAuditPrompt(candidateJson));
        try {
            JsonNode node = objectMapper.readTree(extractJson(auditRaw));
            int score = node.path("score").asInt(0);
            String decision = node.path("decision").asText("revise");
            JsonNode briefing = node.path("briefing");
            if (briefing == null || briefing.isMissingNode() || briefing.isNull()) {
                return new QualityAuditResult(score, decision, candidateJson);
            }
            return new QualityAuditResult(score, decision, objectMapper.writeValueAsString(briefing));
        } catch (Exception e) {
            log.warn("[AI][audit] failed to parse audit result, fallback to candidate. reason={}", e.getMessage());
            return new QualityAuditResult(0, "revise", candidateJson);
        }
    }

    private DailyBriefing parseBriefing(String raw) {
        String json = extractJson(raw);
        try {
            return objectMapper.readValue(json, DailyBriefing.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse Zhipu JSON output: " + e.getMessage(), e);
        }
    }

    private String extractOutputText(JsonNode response) {
        JsonNode choices = response.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode message = choices.get(0).path("message");
            JsonNode content = message.path("content");
            if (content.isTextual()) {
                return content.asText();
            }
            if (content.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode item : content) {
                    JsonNode text = item.path("text");
                    if (text.isTextual()) {
                        sb.append(text.asText());
                    }
                }
                if (!sb.isEmpty()) {
                    return sb.toString();
                }
            }
        }
        log.warn("[AI] unexpected response payload: {}", response);
        return "";
    }

    private JsonNode parseJsonResponse(String responseText) {
        try {
            return objectMapper.readTree(responseText);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse Zhipu API response: " + e.getMessage(), e);
        }
    }

    private String extractJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        throw new IllegalStateException("No JSON object found in model output.");
    }

    private boolean isNetworkTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isRetriableStatus(int status) {
        return status == 429 || status >= 500;
    }

    private long backoffMs(int attempt) {
        long base = properties.getZhipu().getRetryBackoffMs();
        long value = base * (1L << Math.max(0, attempt - 1));
        return Math.min(value, 10000L);
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted.", e);
        }
    }

    private record QualityAuditResult(int score, String decision, String briefingJson) {
    }
}
