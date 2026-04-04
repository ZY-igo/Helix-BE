/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.integration.IntegrationConnection;
import com.sipc115.helix.integration.llm.ZhipuClient;
import com.sipc115.helix.repository.jpa.IntegrationConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationConnectionFactory {

    private final IntegrationConnectionRepository connectionRepository;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    private final Map<Long, FeishuClientProxy> feishuClientCache = new ConcurrentHashMap<>();
    private final Map<Long, ZhipuClient> llmClientCache = new ConcurrentHashMap<>();

    public FeishuClientProxy getFeishuClient(Long connectionId) {
        return feishuClientCache.computeIfAbsent(connectionId, id -> {
            IntegrationConnection conn = connectionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("连接不存在: " + id));

            if (!IntegrationConnection.Type.FEISHU.equals(conn.getType())) {
                throw new IllegalArgumentException("不是飞书连接: " + id);
            }

            return createFeishuClient(conn);
        });
    }

    public ZhipuClient getLlmClient(Long connectionId) {
        return llmClientCache.computeIfAbsent(connectionId, id -> {
            IntegrationConnection conn = connectionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("连接不存在: " + id));

            if (!IntegrationConnection.Type.LLM.equals(conn.getType())) {
                throw new IllegalArgumentException("不是 LLM 连接: " + id);
            }

            return createZhipuClient(conn);
        });
    }

    public FeishuClientProxy createFeishuClient(IntegrationConnection conn) {
        Map<String, Object> config = conn.getConfig();
        String appId = (String) config.get("appId");
        String appSecret = encryptionService.decrypt((String) config.get("appSecret"));
        String apiBaseUrl = (String) config.getOrDefault("apiBaseUrl", "https://open.feishu.cn/open-apis");
        String docxUrlPrefix = (String) config.getOrDefault("docxUrlPrefix", "https://feishu.cn/docx/");

        return new FeishuClientProxy(appId, appSecret, apiBaseUrl, docxUrlPrefix, objectMapper);
    }

    public ZhipuClient createZhipuClient(IntegrationConnection conn) {
        Map<String, Object> config = conn.getConfig();
        String apiKey = encryptionService.decrypt((String) config.get("apiKey"));
        String baseUrl = (String) config.getOrDefault("baseUrl", "https://open.bigmodel.cn/api/paas/v4");
        String model = (String) config.getOrDefault("model", "glm-5");
        double temperature = getDoubleValue(config, "temperature", 1.0);
        int maxTokens = getIntValue(config, "maxTokens", 4096);
        String thinking = (String) config.getOrDefault("thinking", "disabled");

        return new ZhipuClient(apiKey, baseUrl, model, temperature, maxTokens, thinking, objectMapper);
    }

    public void clearCache() {
        feishuClientCache.clear();
        llmClientCache.clear();
        log.info("IntegrationConnectionFactory 缓存已清空");
    }

    public void evictConnection(Long connectionId) {
        feishuClientCache.remove(connectionId);
        llmClientCache.remove(connectionId);
        log.info("连接 {} 的缓存已清除", connectionId);
    }

    private double getDoubleValue(Map<String, Object> config, String key, double defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private int getIntValue(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    @lombok.extern.slf4j.Slf4j
    public static class FeishuClientProxy {
        private final String appId;
        private final String appSecret;
        private final String apiBaseUrl;
        private final String docxUrlPrefix;
        private final ObjectMapper objectMapper;
        private volatile String cachedTenantAccessToken;
        private volatile long tenantTokenExpireAtEpochSecond;

        public FeishuClientProxy(String appId, String appSecret, String apiBaseUrl,
                                 String docxUrlPrefix, ObjectMapper objectMapper) {
            this.appId = appId;
            this.appSecret = appSecret;
            this.apiBaseUrl = apiBaseUrl;
            this.docxUrlPrefix = docxUrlPrefix;
            this.objectMapper = objectMapper;
        }

        public String sendText(String chatId, String text) {
            String token = getTenantAccessToken();
            RestClient restClient = RestClient.builder()
                    .baseUrl(apiBaseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> payload = Map.of(
                    "receive_id", chatId,
                    "msg_type", "text",
                    "content", toJsonString(Map.of("text", text))
            );

            String raw = restClient.post()
                    .uri("/im/v1/messages?receive_id_type=chat_id")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return parseMessageId(raw);
        }

        private String getTenantAccessToken() {
            long now = java.time.Instant.now().getEpochSecond();
            String cached = cachedTenantAccessToken;
            if (cached != null && now < tenantTokenExpireAtEpochSecond) {
                return cached;
            }

            synchronized (this) {
                now = java.time.Instant.now().getEpochSecond();
                cached = cachedTenantAccessToken;
                if (cached != null && now < tenantTokenExpireAtEpochSecond) {
                    return cached;
                }

                RestClient restClient = RestClient.builder()
                        .baseUrl(apiBaseUrl)
                        .build();

                Map<String, Object> payload = Map.of(
                        "app_id", appId,
                        "app_secret", appSecret
                );

                String raw = restClient.post()
                        .uri("/auth/v3/tenant_access_token/internal")
                        .body(payload)
                        .retrieve()
                        .body(String.class);

                try {
                    com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(raw);
                    String token = root.path("tenant_access_token").asText();
                    int expire = root.path("expire").asInt(7200);

                    cachedTenantAccessToken = token;
                    tenantTokenExpireAtEpochSecond = java.time.Instant.now().getEpochSecond() + Math.max(expire - 60, 300);
                    return token;
                } catch (Exception e) {
                    throw new RuntimeException("获取飞书访问令牌失败", e);
                }
            }
        }

        private String parseMessageId(String raw) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(raw);
                int code = root.path("code").asInt(-1);
                if (code != 0) {
                    throw new RuntimeException("飞书 API 错误: " + root.path("msg").asText());
                }
                return root.path("data").path("message_id").asText();
            } catch (Exception e) {
                throw new RuntimeException("解析飞书响应失败", e);
            }
        }

        private String toJsonString(Object value) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (Exception e) {
                throw new RuntimeException("JSON 序列化失败", e);
            }
        }
    }

    public static class ZhipuClient {
        private final String apiKey;
        private final String baseUrl;
        private final String model;
        private final double temperature;
        private final int maxTokens;
        private final String thinking;
        private final ObjectMapper objectMapper;

        public ZhipuClient(String apiKey, String baseUrl, String model, double temperature,
                          int maxTokens, String thinking, ObjectMapper objectMapper) {
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            this.model = model;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
            this.thinking = thinking;
            this.objectMapper = objectMapper;
        }

        public String chat(String stage, String systemPrompt, String userPrompt) {
            RestClient restClient = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", temperature,
                    "max_tokens", maxTokens,
                    "thinking", Map.of("type", thinking),
                    "response_format", Map.of("type", "json_object")
            );

            String raw = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return extractContent(raw);
        }

        private String extractContent(String raw) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(raw);
                com.fasterxml.jackson.databind.JsonNode choices = root.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    return choices.get(0).path("message").path("content").asText();
                }
                return "";
            } catch (Exception e) {
                throw new RuntimeException("解析 LLM 响应失败", e);
            }
        }
    }
}