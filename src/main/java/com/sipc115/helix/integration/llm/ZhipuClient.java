package com.sipc115.helix.integration.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.config.BotProperties;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 智谱 AI 客户端
 * <p>
 * 封装与智谱 AI API 的所有交互
 * </p>
 * 
 * @author system
 * @since 1.0.0
 */
@Component
public class ZhipuClient implements AiClient{

    private static final Logger log = LoggerFactory.getLogger(ZhipuClient.class);

    private final BotProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public ZhipuClient(
            BotProperties properties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(properties.getZhipu().getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String getClientType() {
        return "zhipu";
    }

    /**
     * 向智谱 AI 发送聊天请求
     * @param stage 处理阶段标识
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return 模型响应文本
     */
    public String chat(String stage, String systemPrompt, String userPrompt) {
        log.info("[AI][{}] ask model, next=call /chat/completions", stage);
        log.info("[AI][{}] prompt chars={}", stage, userPrompt == null ? 0 : userPrompt.length());
        log.debug("[AI][{}] user prompt:\n{}", stage, userPrompt);

        Map<String, Object> payload = buildPayload(systemPrompt, userPrompt, stage);

        int maxAttempts = properties.getZhipu().getMaxRetries() + 1;
        String responseText = null;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                responseText = executeRequest(payload);
                lastException = null;
                break;
            } catch (ResourceAccessException e) {
                lastException = e;
                if (!handleRetryableException(stage, attempt, maxAttempts, e, "request timeout")) {
                    throw e;
                }
            } catch (RestClientResponseException e) {
                lastException = e;
                if (!handleRetryableException(stage, attempt, maxAttempts, e, "http status=" + e.getStatusCode().value())) {
                    throw e;
                }
            }
        }

        if (lastException != null) {
            throw new IllegalStateException("Zhipu API request failed after retries: " + lastException.getMessage(), lastException);
        }

        return processResponse(stage, responseText);
    }

    @Override
    public boolean supportsFeature(String feature) {
        return AiClient.super.supportsFeature(feature);
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildPayload(String systemPrompt, String userPrompt, String stage) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", properties.getZhipu().getModel());
        payload.put("messages", List.of(
                message("system", systemPrompt),
                message("user", userPrompt)));
        payload.put("temperature", properties.getZhipu().getTemperature());
        payload.put("max_tokens", getStageMaxTokens(stage));
        payload.put("thinking", Map.of("type", properties.getZhipu().getThinking()));
        payload.put("response_format", Map.of("type", "json_object"));
        return payload;
    }

    /**
     * 执行 HTTP 请求
     */
    private String executeRequest(Map<String, Object> payload) {
        return restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getZhipu().getApiKey())
                .body(payload)
                .retrieve()
                .body(String.class);
    }

    /**
     * 处理可重试的异常
     */
    private boolean handleRetryableException(String stage, int attempt, int maxAttempts,
                                             Exception e, String reason) {
        boolean canRetry = attempt < maxAttempts && isRetryableException(e);
        if (!canRetry) {
            return false;
        }
        long sleepMs = calculateBackoffMs(attempt);
        log.warn("[AI][{}] {}, retry {}/{}, backoff={}ms",
                stage, reason, attempt, maxAttempts, sleepMs);
        sleepQuietly(sleepMs);
        return true;
    }

    /**
     * 判断异常是否可重试
     */
    private boolean isRetryableException(Exception e) {
        if (e instanceof ResourceAccessException) {
            return isNetworkTimeout(e);
        }
        if (e instanceof RestClientResponseException) {
            int status = ((RestClientResponseException) e).getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return false;
    }

    /**
     * 处理响应并提取文本
     */
    private String processResponse(String stage, String responseText) {
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

    /**
     * 构建消息对象
     */
    private Map<String, Object> message(String role, String text) {
        return Map.of("role", role, "content", text);
    }

    /**
     * 根据阶段获取最大 token 数
     */
    private int getStageMaxTokens(String stage) {
        int configured = properties.getZhipu().getMaxTokens();
        if ("retrieve".equals(stage)) {
            return Math.min(configured, 4000);
        }
        if (stage != null && stage.startsWith("optimize-")) {
            return Math.min(configured, 3000);
        }
        if ("formatting".equals(stage) || "deduplicate".equals(stage) ||
            "audit".equals(stage) || "translate-zh".equals(stage)) {
            return Math.min(configured, 2000);
        }
        return configured;
    }

    /**
     * 解析 JSON 响应
     */
    private JsonNode parseJsonResponse(String responseText) {
        try {
            return objectMapper.readTree(responseText);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Zhipu API response: " + e.getMessage(), e);
        }
    }

    /**
     * 从响应中提取输出文本
     */
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

    /**
     * 判断是否为网络超时
     */
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

    /**
     * 计算指数退避时间
     */
    private long calculateBackoffMs(int attempt) {
        long base = properties.getZhipu().getRetryBackoffMs();
        long value = base * (1L << Math.max(0, attempt - 1));
        return Math.min(value, 10000L);
    }

    /**
     * 静默休眠
     */
    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted.", e);
        }
    }
}
