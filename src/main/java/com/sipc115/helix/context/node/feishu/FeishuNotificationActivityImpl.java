/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.feishu;

import com.sipc115.helix.integration.lark.FeishuClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 飞书通知活动实现类
 */
@Component
public class FeishuNotificationActivityImpl implements FeishuNotificationActivity {

    private static final Logger logger = LoggerFactory.getLogger(FeishuNotificationActivityImpl.class);

    @Autowired
    private FeishuClient feishuClient;

    @Override
    public boolean sendText(String chatId, String text) {
        try {
            logger.info("Sending Feishu text message. chatId={}", chatId);

            String messageId;
            if (chatId == null || chatId.isEmpty()) {
                messageId = feishuClient.sendText(text);
            } else {
                messageId = feishuClient.sendTextToChat(chatId, text);
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to send Feishu text message", e);
            return false;
        }
    }

    @Override
    public boolean sendPost(String chatId, String title, List<String> lines) {
        try {
            logger.info("Sending Feishu post message. chatId={}, title={}", chatId, title);

            String messageId;
            if (chatId == null || chatId.isEmpty()) {
                messageId = feishuClient.sendPost(title, lines);
            } else {
                messageId = feishuClient.sendPostToChat(chatId, title, lines);
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to send Feishu post message", e);
            return false;
        }
    }

    @Override
    public boolean sendPostWithLink(String chatId, String title, String text, String url, String linkText) {
        try {
            logger.info("Sending Feishu post with link. chatId={}, url={}", chatId, url);

            String messageId;
            if (chatId == null || chatId.isEmpty()) {
                messageId = feishuClient.sendPostWithLink(title, text, url, linkText);
            } else {
                messageId = feishuClient.sendPostWithLinkToChat(chatId, title, text, url, linkText);
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to send Feishu post with link", e);
            return false;
        }
    }

    @Override
    public String publishCloudDoc(String title, String content) {
        try {
            logger.info("Publishing Feishu cloud doc. title={}", title);

            String docUrl = feishuClient.publishCloudDocIfEnabled(title, content);

            if (docUrl != null) {
                logger.info("Feishu cloud doc published. docUrl={}", docUrl);
            } else {
                logger.warn("Feishu cloud doc is disabled, skipped publishing");
            }

            return docUrl;
        } catch (Exception e) {
            logger.error("Failed to publish Feishu cloud doc", e);
            return null;
        }
    }
}
