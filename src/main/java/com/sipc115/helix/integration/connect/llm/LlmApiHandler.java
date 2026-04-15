/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.connect.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmApiHandler {

    private final RestClient.Builder restClientBuilder;

    public String chat(String baseUrl, String apiKey, String model, String systemPrompt, String userPrompt,
                       double temperature, int maxTokens, String thinking) {
        return chatMessages(
                baseUrl,
                apiKey,
                model,
                List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                temperature,
                maxTokens,
                thinking,
                false
        );
    }

    public String chatJson(String baseUrl, String apiKey, String model, List<Map<String, Object>> messages,
                           double temperature, int maxTokens, String thinking) {
        return chatMessages(baseUrl, apiKey, model, messages, temperature, maxTokens, thinking, true);
    }

    public String chatMessages(String baseUrl, String apiKey, String model, List<Map<String, Object>> messages,
                               double temperature, int maxTokens, String thinking, boolean jsonResponse) {
        log.info("[LLM] Calling chat API. baseUrl={}, model={}, jsonResponse={}", baseUrl, model, jsonResponse);

        RestClient restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", temperature);
        payload.put("max_tokens", maxTokens);
        if (thinking != null && !thinking.isEmpty()) {
            payload.put("thinking", Map.of("type", thinking));
        }
        if (jsonResponse) {
            payload.put("response_format", Map.of("type", "json_object"));
        }

        String raw = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(payload)
                .retrieve()
                .body(String.class);

        return extractContent(raw);
    }

    public String chatComplete(String baseUrl, String apiKey, String model, String prompt,
                               double temperature, int maxTokens) {
        log.info("[LLM] Calling chat completion API. baseUrl={}, model={}", baseUrl, model);

        RestClient restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));
        payload.put("temperature", temperature);
        payload.put("max_tokens", maxTokens);

        String raw = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(payload)
                .retrieve()
                .body(String.class);

        return extractContent(raw);
    }

    private String extractContent(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(raw);
            com.fasterxml.jackson.databind.JsonNode choices = root.path("choices");

            if (choices.isArray() && !choices.isEmpty()) {
                return choices.get(0).path("message").path("content").asText();
            }
            return "";
        } catch (Exception e) {
            log.warn("[LLM] Failed to extract content from response: {}", e.getMessage());
            return "";
        }
    }
}
