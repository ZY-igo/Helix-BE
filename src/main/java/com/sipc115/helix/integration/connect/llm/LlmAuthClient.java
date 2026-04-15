/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.connect.llm;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class LlmAuthClient {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final LlmApiHandler apiHandler;

    public LlmAuthClient(String apiKey, String baseUrl, String model, LlmApiHandler apiHandler) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.apiHandler = apiHandler;
    }

    public String getToken() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API Key 不能为空");
        }
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public String chat(String systemPrompt, String userPrompt, double temperature, int maxTokens, String thinking) {
        return apiHandler.chat(baseUrl, apiKey, model, systemPrompt, userPrompt, temperature, maxTokens, thinking);
    }

    public String chatJson(List<Map<String, Object>> messages, double temperature, int maxTokens, String thinking) {
        return apiHandler.chatJson(baseUrl, apiKey, model, messages, temperature, maxTokens, thinking);
    }

    public String chatComplete(String prompt, double temperature, int maxTokens) {
        return apiHandler.chatComplete(baseUrl, apiKey, model, prompt, temperature, maxTokens);
    }
}
