package com.sipc115.helix.integration.connect.lark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class FeishuApiHandler {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public FeishuApiHandler(ObjectMapper objectMapper, RestClient.Builder builder) {
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl("https://open.feishu.cn/open-apis").build();
    }

    public String sendText(String token, String chatId, String text) {
        String raw = restClient.post()
                .uri("/im/v1/messages?receive_id_type=chat_id")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(Map.of(
                        "receive_id", chatId,
                        "msg_type", "text",
                        "content", toJson(Map.of("text", text))
                ))
                .retrieve()
                .body(String.class);

        return extractMessageId(raw, "发送文本消息");
    }

    public String sendPost(String token, String chatId, String title, List<String> lines) {
        List<List<Map<String, Object>>> content = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                if (line != null && !line.isEmpty()) {
                    content.add(List.of(Map.of("tag", "text", "text", line)));
                }
            }
        }
        if (content.isEmpty()) {
            content.add(List.of(Map.of("tag", "text", "text", "No content.")));
        }

        Map<String, Object> postBody = Map.of(
                "zh_cn", Map.of(
                        "title", title != null ? title : "AI Daily Report",
                        "content", content
                )
        );

        String raw = restClient.post()
                .uri("/im/v1/messages?receive_id_type=chat_id")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(Map.of(
                        "receive_id", chatId,
                        "msg_type", "post",
                        "content", toJson(postBody)
                ))
                .retrieve()
                .body(String.class);

        return extractMessageId(raw, "发送富文本消息");
    }

    public String sendPostWithLink(String token, String chatId, String title, String text, String url, String linkText) {
        List<List<Map<String, Object>>> content = new ArrayList<>();
        if (text != null && !text.isEmpty()) {
            content.add(List.of(Map.of("tag", "text", "text", text)));
        }
        if (url != null && !url.isEmpty()) {
            content.add(List.of(Map.of(
                    "tag", "a",
                    "text", linkText != null ? linkText : "查看文档",
                    "href", url
            )));
        }
        if (content.isEmpty()) {
            content.add(List.of(Map.of("tag", "text", "text", "No content.")));
        }

        Map<String, Object> postBody = Map.of(
                "zh_cn", Map.of(
                        "title", title != null ? title : "AI Daily Report",
                        "content", content
                )
        );

        String raw = restClient.post()
                .uri("/im/v1/messages?receive_id_type=chat_id")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(Map.of(
                        "receive_id", chatId,
                        "msg_type", "post",
                        "content", toJson(postBody)
                ))
                .retrieve()
                .body(String.class);

        return extractMessageId(raw, "发送带链接富文本消息");
    }

    public String createDocument(String token, String title, String folderToken) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title != null ? title : "AI Daily Report");
        if (folderToken != null && !folderToken.isEmpty()) {
            payload.put("folder_token", folderToken);
        }

        String raw = restClient.post()
                .uri("/docx/v1/documents")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(payload)
                .retrieve()
                .body(String.class);

        JsonNode root = parseJson(raw);
        ensureSuccess(root, "创建云文档");

        String documentId = root.path("data").path("document").path("document_id").asText();
        if (documentId.isEmpty()) {
            documentId = root.path("data").path("document_id").asText();
        }
        if (documentId.isEmpty()) {
            throw new IllegalStateException("创建文档成功但 document_id 为空");
        }
        return documentId;
    }

    public void appendBlocks(String token, String documentId, List<Map<String, Object>> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return;
        }

        int batchSize = 20;
        for (int i = 0; i < blocks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, blocks.size());
            List<Map<String, Object>> batch = blocks.subList(i, end);

            String raw = restClient.post()
                    .uri("/docx/v1/documents/{documentId}/blocks/{blockId}/children", documentId, documentId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(Map.of("children", batch))
                    .retrieve()
                    .body(String.class);

            ensureSuccess(parseJson(raw), "追加文档内容");
        }
    }

    private String extractMessageId(String raw, String action) {
        JsonNode root = parseJson(raw);
        ensureSuccess(root, action);
        return root.path("data").path("message_id").asText();
    }

    private JsonNode parseJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException("解析响应失败: " + e.getMessage(), e);
        }
    }

    private void ensureSuccess(JsonNode root, String action) {
        int code = root.path("code").asInt(-1);
        if (code != 0) {
            throw new IllegalStateException(action + "失败: " + root.path("msg").asText());
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("序列化失败: " + e.getMessage(), e);
        }
    }
}
