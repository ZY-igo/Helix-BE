package com.sipc115.helix.integration.workflow.node.feishu;

import com.sipc115.helix.integration.lark.FeishuClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FeishuSendPostActivityImpl implements FeishuSendPostActivity {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendPostActivityImpl.class);

    @Autowired
    private FeishuClient feishuClient;

    @Override
    public boolean sendPost(String chatId, String title, List<String> lines) {
        try {
            log.info("Sending Feishu post message. chatId={}, title={}", chatId, title);

            String messageId;
            if (chatId == null || chatId.isEmpty()) {
                messageId = feishuClient.sendPost(title, lines);
            } else {
                messageId = feishuClient.sendPostToChat(chatId, title, lines);
            }

            return messageId != null && !messageId.isEmpty();
        } catch (Exception e) {
            log.error("Failed to send Feishu post message", e);
            return false;
        }
    }
}
