/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.connect.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.integration.IntegrationConnection;
import com.sipc115.helix.integration.connect.ConnectionClient;
import com.sipc115.helix.repository.jpa.IntegrationConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("llmConnectionClient")
@RequiredArgsConstructor
public class LlmConnectionClient implements ConnectionClient<LlmAuthClient> {

    private final IntegrationConnectionRepository connectionRepository;
    private final LlmApiHandler apiHandler;

    @Override
    public String getConnectionType() {
        return "LLM";
    }

    @Override
    public void test(Object config) throws Exception {
        LlmAuthClient client = createClient(config);
        client.chat("You are a helpful assistant.", "Hi", 0.7, 100, "disabled");
    }

    @Override
    public LlmAuthClient createClient(Object config) throws Exception {
        Map<String, Object> configMap = toConfigMap(config);
        String apiKey = getString(configMap, "apiKey", null);
        String model = getString(configMap, "model", null);
        String baseUrl = getString(configMap, "baseUrl", null);

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API Key 不能为空");
        }
        if (model == null || model.isEmpty()) {
            throw new IllegalStateException("Model 不能为空");
        }
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalStateException("Base URL 不能为空");
        }

        return new LlmAuthClient(apiKey, baseUrl, model, apiHandler);
    }

    public LlmAuthClient getClientById(Long connectionId) throws Exception {
        IntegrationConnection conn = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

        return createClient(conn.getConfig());
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
}