package com.sipc115.helix.integration.workflow.node.feishu.sendpostwithlink;

import com.sipc115.helix.integration.lark.FeishuClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FeishuSendPostWithLinkActivityImpl implements FeishuSendPostWithLinkActivity {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendPostWithLinkActivityImpl.class);

    @Autowired
    private FeishuClient feishuClient;

    @Override
    public boolean sendPostWithLink(String chatId, String title, String text, String url, String linkText) {
        try {
            log.info("Sending Feishu post with link. chatId={}, url={}", chatId, url);

            String messageId;
            if (chatId == null || chatId.isEmpty()) {
                messageId = feishuClient.sendPostWithLink(title, text, url, linkText);
            } else {
                messageId = feishuClient.sendPostWithLinkToChat(chatId, title, text, url, linkText);
            }

            return messageId != null && !messageId.isEmpty();
        } catch (Exception e) {
            log.error("Failed to send Feishu post with link", e);
            return false;
        }
    }
}
