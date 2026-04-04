package com.sipc115.helix.integration.workflow.node.feishu.sendtext;

import com.sipc115.helix.integration.lark.FeishuClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FeishuSendTextActivityImpl implements FeishuSendTextActivity {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendTextActivityImpl.class);

    @Autowired
    private FeishuClient feishuClient;

    @Override
    public boolean sendText(String chatId, String text) {
        try {
            log.info("Sending Feishu text message. chatId={}", chatId);

            String messageId;
            if (chatId == null || chatId.isEmpty()) {
                messageId = feishuClient.sendText(text);
            } else {
                messageId = feishuClient.sendTextToChat(chatId, text);
            }

            return messageId != null && !messageId.isEmpty();
        } catch (Exception e) {
            log.error("Failed to send Feishu text message", e);
            return false;
        }
    }
}
