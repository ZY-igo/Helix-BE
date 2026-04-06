package com.sipc115.helix.integration.connect.lark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.domain.integration.IntegrationConnection;
import com.sipc115.helix.integration.connect.ConnectionClient;
import com.sipc115.helix.repository.jpa.IntegrationConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component("feishuConnectionClient")
@RequiredArgsConstructor
public class FeishuConnectionClient implements ConnectionClient<FeishuAuthClient> {

    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;
    private final IntegrationConnectionRepository connectionRepo;

    @Override
    public String getConnectionType() {
        return "FEISHU";
    }

    @Override
    public void test(Object config) throws Exception {
        Map<String, Object> configMap = toConfigMap(config);
        String appId = getString(configMap, "appId");
        String appSecret = getString(configMap, "appSecret");

        FeishuAuthClient client = new FeishuAuthClient(appId, appSecret, objectMapper, restClientBuilder);
        client.getToken();
    }

    @Override
    public FeishuAuthClient createClient(Object config) {
        Map<String, Object> configMap = toConfigMap(config);
        String appId = getString(configMap, "appId");
        String appSecret = getString(configMap, "appSecret");

        return new FeishuAuthClient(appId, appSecret, objectMapper, restClientBuilder);
    }

    public FeishuAuthClient getClientById(Long connectionId) throws Exception {
        IntegrationConnection conn = connectionRepo.findById(connectionId)
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

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
