package com.sipc115.helix.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.config.BotProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class FeishuClient {

    private static final Logger log = LoggerFactory.getLogger(FeishuClient.class);
    private static final String DEFAULT_OPEN_API_BASE = "https://open.feishu.cn/open-apis";
    private static final String DEFAULT_DOCX_URL_PREFIX = "https://feishu.cn/docx/";
    private static final int DOC_BLOCK_MAX_CHARS = 900;
    private static final int DOC_APPEND_BATCH_SIZE = 20;
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");

    private final BotProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    private volatile String cachedTenantAccessToken;
    private volatile long tenantTokenExpireAtEpochSecond;

    public FeishuClient(BotProperties properties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(DEFAULT_OPEN_API_BASE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String sendText(String text) {
        return sendTextToChat(properties.getFeishu().getChatId(), text);
    }

    public String sendPost(String title, List<String> lines) {
        return sendPostToChat(properties.getFeishu().getChatId(), title, lines);
    }

    public String sendPostWithLink(String title, String text, String url, String linkText) {
        return sendPostWithLinkToChat(properties.getFeishu().getChatId(), title, text, url, linkText);
    }

    public String sendTextToChat(String chatId, String text) {
        if (!StringUtils.hasText(chatId)) {
            throw new IllegalStateException("app.feishu.chat-id is empty.");
        }
        String tenantToken = getTenantAccessToken();
        Map<String, Object> payload = new HashMap<>();
        payload.put("receive_id", chatId);
        payload.put("msg_type", "text");
        payload.put("content", toJsonString(Map.of("text", text)));

        String raw = restClient.post()
                .uri("/im/v1/messages?receive_id_type=chat_id")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantToken)
                .body(payload)
                .retrieve()
                .body(String.class);
        JsonNode root = parseResponse(raw);
        ensureSuccess(root, "send text message");
        String messageId = root.path("data").path("message_id").asText();
        log.info("[Feishu] text message sent. chatId={}, messageId={}", chatId, messageId);
        return messageId;
    }

    public String sendPostToChat(String chatId, String title, List<String> lines) {
        if (!StringUtils.hasText(chatId)) {
            throw new IllegalStateException("app.feishu.chat-id is empty.");
        }
        String tenantToken = getTenantAccessToken();
        List<List<Map<String, Object>>> content = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                content.add(List.of(Map.of("tag", "text", "text", line)));
            }
        }
        if (content.isEmpty()) {
            content.add(List.of(Map.of("tag", "text", "text", "No content.")));
        }
        Map<String, Object> postBody = Map.of(
                "zh_cn", Map.of(
                        "title", StringUtils.hasText(title) ? title : "AI Daily Report",
                        "content", content
                )
        );
        Map<String, Object> payload = new HashMap<>();
        payload.put("receive_id", chatId);
        payload.put("msg_type", "post");
        payload.put("content", toJsonString(postBody));

        String raw = restClient.post()
                .uri("/im/v1/messages?receive_id_type=chat_id")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantToken)
                .body(payload)
                .retrieve()
                .body(String.class);
        JsonNode root = parseResponse(raw);
        ensureSuccess(root, "send post message");
        String messageId = root.path("data").path("message_id").asText();
        log.info("[Feishu] post message sent. chatId={}, messageId={}", chatId, messageId);
        return messageId;
    }

    public String sendPostWithLinkToChat(String chatId, String title, String text, String url, String linkText) {
        if (!StringUtils.hasText(chatId)) {
            throw new IllegalStateException("app.feishu.chat-id is empty.");
        }
        String tenantToken = getTenantAccessToken();
        List<List<Map<String, Object>>> content = new ArrayList<>();
        if (StringUtils.hasText(text)) {
            content.add(List.of(Map.of("tag", "text", "text", text)));
        }
        if (StringUtils.hasText(url)) {
            content.add(List.of(Map.of("tag", "a", "text",
                    StringUtils.hasText(linkText) ? linkText : "View document", "href", url)));
        }
        if (content.isEmpty()) {
            content.add(List.of(Map.of("tag", "text", "text", "No content.")));
        }
        Map<String, Object> postBody = Map.of(
                "zh_cn", Map.of(
                        "title", StringUtils.hasText(title) ? title : "AI Daily Report",
                        "content", content
                )
        );
        Map<String, Object> payload = new HashMap<>();
        payload.put("receive_id", chatId);
        payload.put("msg_type", "post");
        payload.put("content", toJsonString(postBody));

        String raw = restClient.post()
                .uri("/im/v1/messages?receive_id_type=chat_id")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantToken)
                .body(payload)
                .retrieve()
                .body(String.class);
        JsonNode root = parseResponse(raw);
        ensureSuccess(root, "send post link message");
        String messageId = root.path("data").path("message_id").asText();
        log.info("[Feishu] post link message sent. chatId={}, messageId={}", chatId, messageId);
        return messageId;
    }

    public String publishCloudDocIfEnabled(String title, String content) {
        if (!properties.getFeishu().isEnableCloudDoc()) {
            return null;
        }
        String documentId = createDocument(title);
        List<Map<String, Object>> blocks = buildRichDocBlocks(title, content);
        appendBlocksToDocument(documentId, blocks);
        String docUrl = buildDocUrl(documentId);
        log.info("[Feishu] cloud doc published. documentId={}, docUrl={}", documentId, docUrl);
        return docUrl;
    }

    private String createDocument(String title) {
        String tenantToken = getTenantAccessToken();
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", StringUtils.hasText(title) ? title : "AI Daily Report");
        if (StringUtils.hasText(properties.getFeishu().getCloudDocFolderToken())) {
            payload.put("folder_token", properties.getFeishu().getCloudDocFolderToken());
        }

        String raw = restClient.post()
                .uri("/docx/v1/documents")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantToken)
                .body(payload)
                .retrieve()
                .body(String.class);
        JsonNode root = parseResponse(raw);
        ensureSuccess(root, "create document");

        String documentId = root.path("data").path("document").path("document_id").asText();
        if (!StringUtils.hasText(documentId)) {
            documentId = root.path("data").path("document_id").asText();
        }
        if (!StringUtils.hasText(documentId)) {
            throw new IllegalStateException("Feishu create document success but document_id is empty.");
        }
        return documentId;
    }

    private void appendBlocksToDocument(String documentId, List<Map<String, Object>> blocks) {
        String tenantToken = getTenantAccessToken();
        if (blocks == null || blocks.isEmpty()) {
            blocks = List.of(buildTextBlock(List.of(buildTextRun("No details available.", false, null, false))));
        }

        for (int i = 0; i < blocks.size(); i += DOC_APPEND_BATCH_SIZE) {
            int end = Math.min(i + DOC_APPEND_BATCH_SIZE, blocks.size());
            List<Map<String, Object>> batch = blocks.subList(i, end);
            Map<String, Object> payload = Map.of("children", batch);

            String raw = restClient.post()
                    .uri("/docx/v1/documents/{document_id}/blocks/{block_id}/children", documentId, documentId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantToken)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            JsonNode root = parseResponse(raw);
            ensureSuccess(root, "append document content");
        }
    }

    private List<Map<String, Object>> buildRichDocBlocks(String title, String content) {
        String normalized = normalizeDocInput(title, content);
        List<Map<String, Object>> blocks = new ArrayList<>();
        for (String rawLine : normalized.split("\n")) {
            String line = rawLine == null ? "" : rawLine.strip();
            if (!StringUtils.hasText(line)) {
                continue;
            }
            if (line.startsWith("# ")) {
                appendChunkedStyledLine(blocks, line.substring(2).trim(), true, false);
                continue;
            }
            if (line.startsWith("## ")) {
                appendChunkedStyledLine(blocks, "[ " + line.substring(3).trim() + " ]", true, false);
                continue;
            }
            if (line.startsWith("- ")) {
                appendListLine(blocks, line.substring(2).trim());
                continue;
            }
            appendKeyValueOrPlainLine(blocks, line);
        }
        return blocks;
    }

    private String normalizeDocInput(String title, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(StringUtils.hasText(title) ? title : "AI Daily Report").append("\n\n");
        if (!StringUtils.hasText(content)) {
            sb.append("No details available.");
            return sb.toString();
        }
        sb.append(content.replace("\r\n", "\n"));
        return sb.toString();
    }

    private void appendListLine(List<Map<String, Object>> blocks, String line) {
        Matcher matcher = URL_PATTERN.matcher(line);
        if (matcher.find()) {
            String url = matcher.group(1);
            String left = line.substring(0, matcher.start()).trim();
            List<Map<String, Object>> elements = new ArrayList<>();
            elements.add(buildTextRun("- " + left + " ", false, null, false));
            elements.add(buildTextRun(url, false, url, false));
            blocks.add(buildTextBlock(elements));
            return;
        }
        appendChunkedStyledLine(blocks, "- " + line, false, false);
    }

    private void appendKeyValueOrPlainLine(List<Map<String, Object>> blocks, String line) {
        int idx = line.indexOf('\uFF1A');
        if (idx < 0) {
            idx = line.indexOf(':');
        }
        if (idx > 0) {
            String key = line.substring(0, idx + 1).trim();
            String value = line.substring(idx + 1).trim();
            Matcher matcher = URL_PATTERN.matcher(value);
            if (matcher.find()) {
                String url = matcher.group(1);
                List<Map<String, Object>> elements = new ArrayList<>();
                elements.add(buildTextRun(key + " ", true, null, false));
                elements.add(buildTextRun(url, false, url, false));
                blocks.add(buildTextBlock(elements));
                return;
            }

            List<String> chunks = splitToChunks(value);
            if (chunks.isEmpty()) {
                blocks.add(buildTextBlock(List.of(buildTextRun(key, true, null, false))));
                return;
            }
            for (int i = 0; i < chunks.size(); i++) {
                List<Map<String, Object>> elements = new ArrayList<>();
                if (i == 0) {
                    elements.add(buildTextRun(key + " ", true, null, false));
                }
                elements.add(buildTextRun(chunks.get(i), false, null, false));
                blocks.add(buildTextBlock(elements));
            }
            return;
        }
        appendChunkedStyledLine(blocks, line, false, false);
    }

    private void appendChunkedStyledLine(List<Map<String, Object>> blocks, String text, boolean bold, boolean italic) {
        List<String> chunks = splitToChunks(text);
        if (chunks.isEmpty()) {
            return;
        }
        for (String chunk : chunks) {
            blocks.add(buildTextBlock(List.of(buildTextRun(chunk, bold, null, italic))));
        }
    }

    private List<String> splitToChunks(String text) {
        List<String> chunks = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return chunks;
        }
        int from = 0;
        while (from < text.length()) {
            int to = Math.min(from + DOC_BLOCK_MAX_CHARS, text.length());
            chunks.add(text.substring(from, to));
            from = to;
        }
        return chunks;
    }

    private Map<String, Object> buildTextBlock(List<Map<String, Object>> elements) {
        return Map.of(
                "block_type", 2,
                "text", Map.of("elements", elements)
        );
    }

    private Map<String, Object> buildTextRun(String content, boolean bold, String linkUrl, boolean italic) {
        Map<String, Object> textRun = new HashMap<>();
        textRun.put("content", content);

        Map<String, Object> style = new HashMap<>();
        if (bold) {
            style.put("bold", true);
        }
        if (italic) {
            style.put("italic", true);
        }
        if (StringUtils.hasText(linkUrl)) {
            style.put("link", Map.of("url", URLEncoder.encode(linkUrl, StandardCharsets.UTF_8)));
        }
        if (!style.isEmpty()) {
            textRun.put("text_element_style", style);
        }
        return Map.of("text_run", textRun);
    }

    private String buildDocUrl(String documentId) {
        String prefix = properties.getFeishu().getDocxUrlPrefix();
        if (!StringUtils.hasText(prefix)) {
            return DEFAULT_DOCX_URL_PREFIX + documentId;
        }
        String normalized = prefix.endsWith("/") ? prefix : prefix + "/";
        return normalized + documentId;
    }

    private String getTenantAccessToken() {
        long now = Instant.now().getEpochSecond();
        String cached = cachedTenantAccessToken;
        if (StringUtils.hasText(cached) && now < tenantTokenExpireAtEpochSecond) {
            return cached;
        }
        synchronized (this) {
            now = Instant.now().getEpochSecond();
            cached = cachedTenantAccessToken;
            if (StringUtils.hasText(cached) && now < tenantTokenExpireAtEpochSecond) {
                return cached;
            }
            Map<String, Object> payload = Map.of(
                    "app_id", properties.getFeishu().getAppId(),
                    "app_secret", properties.getFeishu().getAppSecret()
            );
            String raw = restClient.post()
                    .uri("/auth/v3/tenant_access_token/internal")
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            JsonNode root = parseResponse(raw);
            ensureSuccess(root, "fetch tenant access token");
            String token = root.path("tenant_access_token").asText();
            int expire = root.path("expire").asInt(7200);
            if (!StringUtils.hasText(token)) {
                throw new IllegalStateException("Feishu tenant_access_token is empty.");
            }
            cachedTenantAccessToken = token;
            tenantTokenExpireAtEpochSecond = Instant.now().getEpochSecond() + Math.max(expire - 60, 300);
            return token;
        }
    }

    private JsonNode parseResponse(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Feishu response: " + e.getMessage(), e);
        }
    }

    private void ensureSuccess(JsonNode root, String action) {
        int code = root.path("code").asInt(-1);
        if (code != 0) {
            String msg = root.path("msg").asText("unknown error");
            throw new IllegalStateException("Feishu " + action + " failed. code=" + code + ", msg=" + msg);
        }
    }

    private String toJsonString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize message content: " + e.getMessage(), e);
        }
    }
}
