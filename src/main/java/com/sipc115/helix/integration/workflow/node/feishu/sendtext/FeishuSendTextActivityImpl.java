/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendtext;

import com.sipc115.helix.integration.connect.ConnectionClientRegistry;
import com.sipc115.helix.integration.connect.lark.FeishuApiHandler;
import com.sipc115.helix.integration.connect.lark.FeishuAuthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 飞书发送文本消息 Activity 实现
 */
@Component
public class FeishuSendTextActivityImpl implements FeishuSendTextActivity {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendTextActivityImpl.class);

    @Autowired
    private ConnectionClientRegistry connectionRegistry;

    @Override
    @SuppressWarnings("unchecked")
    public boolean sendText(Long connectionId, String chatId, String text) {
        try {
            log.info("Sending Feishu text message. chatId={}, connectionId={}", chatId, connectionId);

            FeishuAuthClient authClient = connectionRegistry.getOrCreateClientByConnection(connectionId);
            String token = authClient.getToken();

            FeishuApiHandler handler = new FeishuApiHandler(
                    new com.fasterxml.jackson.databind.ObjectMapper(),
                    org.springframework.web.client.RestClient.builder()
            );

            String messageId = handler.sendText(token, chatId, text);
            log.info("Feishu message sent successfully. messageId={}", messageId);

            return messageId != null && !messageId.isEmpty();
        } catch (Exception e) {
            log.error("Failed to send Feishu text message. chatId={}, connectionId={}", chatId, connectionId, e);
            return false;
        }
    }
}