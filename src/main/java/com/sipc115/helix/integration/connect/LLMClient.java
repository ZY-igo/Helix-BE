/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.connect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * LLM 连接客户端实现
 * <p>
 * 实现 ConnectionClient 接口，负责与大语言模型 API 交互。
 *
 * <h3>支持的连接类型：</h3>
 * <ul>
 *   <li>LLM - 通用大语言模型</li>
 * </ul>
 *
 * @author Helix Team
 * @see ConnectionClient
 * @see ConnectionClientRegistry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LLMClient implements ConnectionClient<Object> {

    private static final String CONNECTION_TYPE = "LLM";
    private final ObjectMapper objectMapper;

    @Override
    public String getConnectionType() {
        return CONNECTION_TYPE;
    }

    @Override
    public void test(Object config) throws Exception {
        Map<String, Object> configMap = toConfigMap(config);
        String baseUrl = getString(configMap, "baseUrl", "https://open.bigmodel.cn/api/paas/v4");
        String apiKey = getString(configMap, "apiKey", null);

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> payload = Map.of(
                "model", getString(configMap, "model", "glm-5"),
                "messages", java.util.List.of(
                        Map.of("role", "user", "content", "Hi")
                ),
                "max_tokens", 10
        );

        restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(payload)
                .retrieve()
                .body(String.class);
    }

    @Override
    public Object createClient(Object config) throws Exception {
        return this;
    }

    public Object send(Object config, Map<String, Object> params) throws Exception {
        Map<String, Object> configMap = toConfigMap(config);

        String baseUrl = getString(configMap, "baseUrl", "https://open.bigmodel.cn/api/paas/v4");
        String apiKey = getString(configMap, "apiKey", null);
        String model = getString(configMap, "model", "glm-5");
        double temperature = getDouble(configMap, "temperature", 1.0);
        int maxTokens = getInt(configMap, "maxTokens", 4096);
        String thinking = getString(configMap, "thinking", "disabled");

        String systemPrompt = getString(params, "systemPrompt", "You are a helpful assistant.");
        String userPrompt = getString(params, "userPrompt", "");

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", java.util.List.of(
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

    private String extractContent(String raw) throws Exception {
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(raw);
        com.fasterxml.jackson.databind.JsonNode choices = root.path("choices");

        if (choices.isArray() && !choices.isEmpty()) {
            return choices.get(0).path("message").path("content").asText();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toConfigMap(Object config) {
        if (config instanceof Map) {
            return (Map<String, Object>) config;
        }
        throw new IllegalArgumentException("Config must be a Map");
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}