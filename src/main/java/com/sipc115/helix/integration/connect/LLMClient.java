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
 * <h3>支持的模型配置：</h3>
 * <ul>
 *   <li>baseUrl - API 基础 URL</li>
 *   <li>model - 模型名称（默认 glm-5）</li>
 *   <li>temperature - 生成温度（默认 1.0）</li>
 *   <li>maxTokens - 最大 Token 数（默认 4096）</li>
 * </ul>
 *
 * @author Helix Team
 * @see ConnectionClient
 * @see ConnectionClientRegistry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LLMClient implements ConnectionClient {

    /**
     * 连接类型标识
     */
    private static final String CONNECTION_TYPE = "LLM";

    /**
     * JSON 序列化工具
     */
    private final ObjectMapper objectMapper;

    @Override
    public String getConnectionType() {
        return CONNECTION_TYPE;
    }

    /**
     * 测试 LLM 连接
     * <p>
     * 发送一条简单的测试对话验证连接是否可用。
     *
     * @param config 连接配置
     * @throws Exception 测试失败时抛出
     */
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

    /**
     * 发送 LLM 对话请求
     * <p>
     * 调用大语言模型对话接口。
     *
     * <h3>请求参数：</h3>
     * <ul>
     *   <li>systemPrompt - 系统提示词</li>
     *   <li>userPrompt - 用户输入</li>
     * </ul>
     *
     * @param config 连接配置
     * @param params 请求参数
     * @return 模型回复内容
     * @throws Exception 请求失败时抛出
     */
    @Override
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

    /**
     * 从 API 响应中提取内容
     */
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