package com.sipc115.helix.integration.workflow.node.agent;

import com.sipc115.helix.integration.llm.AiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AiChatActivityImpl implements AiChatActivity {

    private static final Logger log = LoggerFactory.getLogger(AiChatActivityImpl.class);

    @Autowired
    private AiClient aiClient;

    @Override
    public Object chat(String prompt, String model) {
        log.info("AI chat activity called with model: {}", model);

        String systemPrompt = "You are a helpful AI assistant.";
        String response = aiClient.chat("ai-task", systemPrompt, prompt);

        return Map.of(
                "response", response,
                "model", model,
                "status", "success"
        );
    }
}
