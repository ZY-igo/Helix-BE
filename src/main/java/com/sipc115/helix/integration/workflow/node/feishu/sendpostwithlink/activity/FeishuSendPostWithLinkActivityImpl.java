/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpostwithlink.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.integration.connect.ConnectionClientRegistry;
import com.sipc115.helix.integration.connect.lark.FeishuApiHandler;
import com.sipc115.helix.integration.connect.lark.FeishuAuthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 飞书发送带链接富文本消息 Activity 实现
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuSendPostWithLinkActivityImpl implements FeishuSendPostWithLinkActivity {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendPostWithLinkActivityImpl.class);

    private static ConnectionClientRegistry connectionRegistry;

    @Autowired
    public void setConnectionRegistry(ConnectionClientRegistry connectionRegistry) {
        FeishuSendPostWithLinkActivityImpl.connectionRegistry = connectionRegistry;
    }

    @Override
    public void sendPostWithLink(Long connectionId, String chatId, String title, String text, String url, String linkText) {
        log.info("FeishuSendPostWithLinkActivity: 开始发送带链接富文本消息, connectionId={}, chatId={}",
            connectionId, chatId);

        try {
            // 1. 获取飞书认证客户端
            FeishuAuthClient authClient = connectionRegistry.getOrCreateClientByConnection(connectionId);

            // 2. 获取访问令牌
            String token = authClient.getToken();
            log.debug("获取飞书访问令牌成功, connectionId={}", connectionId);

            // 3. 创建飞书 API 处理器
            FeishuApiHandler handler = new FeishuApiHandler(
                new ObjectMapper(),
                RestClient.builder()
            );

            // 4. 发送带链接富文本消息
            handler.sendPostWithLink(token, chatId, title, text, url, linkText);
            log.info("FeishuSendPostWithLinkActivity: 带链接富文本消息发送成功, connectionId={}, chatId={}",
                connectionId, chatId);

        } catch (Exception e) {
            log.error("FeishuSendPostWithLinkActivity: 带链接富文本消息发送失败, connectionId={}, chatId={}, error={}",
                connectionId, chatId, e.getMessage(), e);
            throw new RuntimeException("飞书发送带链接富文本消息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendPostWithLinkWithConfig(Object connectionConfig, String chatId, String title, String text, String url, String linkText) {
        log.info("FeishuSendPostWithLinkActivity: 开始发送带链接富文本消息(带配置), chatId={}", chatId);

        try {
            // 1. 获取飞书认证客户端（使用连接配置）
            FeishuAuthClient authClient = connectionRegistry.getOrCreateClient(
                null,
                "FEISHU",
                connectionConfig
            );

            // 2. 获取访问令牌
            String token = authClient.getToken();
            log.debug("获取飞书访问令牌成功");

            // 3. 创建飞书 API 处理器
            FeishuApiHandler handler = new FeishuApiHandler(
                new ObjectMapper(),
                RestClient.builder()
            );

            // 4. 发送带链接富文本消息
            handler.sendPostWithLink(token, chatId, title, text, url, linkText);
            log.info("FeishuSendPostWithLinkActivity: 带链接富文本消息发送成功, chatId={}", chatId);

        } catch (Exception e) {
            log.error("FeishuSendPostWithLinkActivity: 带链接富文本消息发送失败, chatId={}, error={}",
                chatId, e.getMessage(), e);
            throw new RuntimeException("飞书发送带链接富文本消息失败: " + e.getMessage(), e);
        }
    }
}