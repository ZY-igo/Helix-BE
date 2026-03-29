package com.sipc115.helix.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.service.BotCommandService;
import com.sipc115.helix.config.FeishuClient;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feishu")
public class FeishuEventController {

    private static final Logger log = LoggerFactory.getLogger(FeishuEventController.class);

    private final ObjectMapper objectMapper;
    private final BotCommandService commandService;
    private final FeishuClient feishuClient;

    public FeishuEventController(ObjectMapper objectMapper, BotCommandService commandService, FeishuClient feishuClient) {
        this.objectMapper = objectMapper;
        this.commandService = commandService;
        this.feishuClient = feishuClient;
    }

    @PostMapping("/events")
    public Map<String, Object> receiveEvent(@RequestBody JsonNode payload) {
        // Feishu event entrypoint: verify URL, parse command message, execute and reply.
        String type = payload.path("type").asText();
        if ("url_verification".equals(type)) {
            log.info("[FeishuEvent] URL verification challenge received.");
            return Map.of("challenge", payload.path("challenge").asText());
        }
        if (!"event_callback".equals(type)) {
            return Map.of("ok", true);
        }

        JsonNode event = payload.path("event");
        if (!"im.message.receive_v1".equals(event.path("type").asText())
                && !"message".equals(event.path("message_type").asText())) {
            return Map.of("ok", true);
        }

        String chatId = event.path("message").path("chat_id").asText();
        String contentJson = event.path("message").path("content").asText();
        String text = extractTextContent(contentJson);
        log.info("[FeishuEvent] Message received. chatId={}, text={}", chatId, text);

        if (text.contains("/")) {
            String commandText = text.substring(text.indexOf('/')).trim();
            log.info("[FeishuEvent] Execute command: {}", commandText);
            String result = commandService.execute(commandText);
            log.info("[FeishuEvent] Command result: {}", result);
            feishuClient.sendTextToChat(chatId, result);
        }
        return Map.of("ok", true);
    }

    private String extractTextContent(String contentJson) {
        try {
            JsonNode content = objectMapper.readTree(contentJson);
            return content.path("text").asText("");
        } catch (JsonProcessingException ignored) {
            return "";
        }
    }
}
