package com.sipc115.helix.integration.connect.lark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Slf4j
public class FeishuAuthClient {

    private final String appId;
    private final String appSecret;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Getter
    private volatile String cachedToken;
    private volatile long tokenExpireAt;

    public FeishuAuthClient(String appId, String appSecret, ObjectMapper objectMapper, RestClient.Builder builder) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl("https://open.feishu.cn/open-apis").build();
    }

    public String getToken() {
        long now = Instant.now().getEpochSecond();
        if (cachedToken != null && now < tokenExpireAt) {
            return cachedToken;
        }

        synchronized (this) {
            now = Instant.now().getEpochSecond();
            if (cachedToken != null && now < tokenExpireAt) {
                return cachedToken;
            }

            String raw = restClient.post()
                    .uri("/auth/v3/tenant_access_token/internal")
                    .body(Map.of("app_id", appId, "app_secret", appSecret))
                    .retrieve()
                    .body(String.class);

            try {
                JsonNode root = objectMapper.readTree(raw);
                if (root.path("code").asInt() != 0) {
                    throw new IllegalStateException("Token 获取失败: " + root.path("msg").asText());
                }

                cachedToken = root.path("tenant_access_token").asText();
                int expire = root.path("expire").asInt(7200);
                tokenExpireAt = Instant.now().getEpochSecond() + expire - 60;

                log.debug("Feishu token refreshed for appId={}, expire in {}s", appId, expire);
                return cachedToken;
            } catch (Exception e) {
                throw new RuntimeException("解析 Token 响应失败", e);
            }
        }
    }
}
