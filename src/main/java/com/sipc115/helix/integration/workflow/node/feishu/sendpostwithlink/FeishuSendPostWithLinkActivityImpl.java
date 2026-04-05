package com.sipc115.helix.integration.workflow.node.feishu.sendpostwithlink;

import com.sipc115.helix.integration.connect.ConnectionClientRegistry;
import com.sipc115.helix.integration.connect.lark.FeishuApiHandler;
import com.sipc115.helix.integration.connect.lark.FeishuAuthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FeishuSendPostWithLinkActivityImpl implements FeishuSendPostWithLinkActivity {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendPostWithLinkActivityImpl.class);

    @Autowired
    private ConnectionClientRegistry connectionRegistry;

    @Override
    @SuppressWarnings("unchecked")
    public boolean sendPostWithLink(Long connectionId, String chatId, String title, String text, String url, String linkText) {
        try {
            log.info("Sending Feishu post with link. chatId={}, url={}", chatId, url);

            FeishuAuthClient authClient = (FeishuAuthClient) connectionRegistry.getOrCreateClient(connectionId, "FEISHU", null);
            String token = authClient.getToken();

            FeishuApiHandler handler = new FeishuApiHandler(
                    new com.fasterxml.jackson.databind.ObjectMapper(),
                    org.springframework.web.client.RestClient.builder()
            );

            String messageId = handler.sendPostWithLink(token, chatId, title, text, url, linkText);

            return messageId != null && !messageId.isEmpty();
        } catch (Exception e) {
            log.error("Failed to send Feishu post with link", e);
            return false;
        }
    }
}