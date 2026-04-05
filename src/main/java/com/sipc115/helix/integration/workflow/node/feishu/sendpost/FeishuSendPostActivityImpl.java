/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpost;

import com.sipc115.helix.integration.connect.ConnectionClientRegistry;
import com.sipc115.helix.integration.connect.lark.FeishuApiHandler;
import com.sipc115.helix.integration.connect.lark.FeishuAuthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FeishuSendPostActivityImpl implements FeishuSendPostActivity {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendPostActivityImpl.class);

    @Autowired
    private ConnectionClientRegistry connectionRegistry;

    @Override
    @SuppressWarnings("unchecked")
    public boolean sendPost(Long connectionId, String chatId, String title, List<String> lines) {
        try {
            log.info("Sending Feishu post message. chatId={}, title={}", chatId, title);

            FeishuAuthClient authClient = connectionRegistry.getOrCreateClientByConnection(connectionId);
            String token = authClient.getToken();

            FeishuApiHandler handler = new FeishuApiHandler(
                    new com.fasterxml.jackson.databind.ObjectMapper(),
                    org.springframework.web.client.RestClient.builder()
            );

            String messageId = handler.sendPost(token, chatId, title, lines);

            return messageId != null && !messageId.isEmpty();
        } catch (Exception e) {
            log.error("Failed to send Feishu post message", e);
            return false;
        }
    }
}