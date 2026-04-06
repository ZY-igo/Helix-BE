/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.publishclouddoc.activity;

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
 * 飞书发布云文档 Activity 实现
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuPublishCloudDocActivityImpl implements FeishuPublishCloudDocActivity {

    private static final Logger log = LoggerFactory.getLogger(FeishuPublishCloudDocActivityImpl.class);

    private static ConnectionClientRegistry connectionRegistry;

    @Autowired
    public void setConnectionRegistry(ConnectionClientRegistry connectionRegistry) {
        FeishuPublishCloudDocActivityImpl.connectionRegistry = connectionRegistry;
    }

    @Override
    public String publishCloudDoc(Long connectionId, String title, String content) {
        log.info("FeishuPublishCloudDocActivity: 开始发布云文档, connectionId={}, title={}",
            connectionId, title);

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

            // 4. 创建云文档
            String documentId = handler.createDocument(token, title, null);
            log.info("FeishuPublishCloudDocActivity: 云文档创建成功, connectionId={}, documentId={}",
                connectionId, documentId);

            // 5. 返回文档 URL
            return "https://feishu.cn/docx/" + documentId;

        } catch (Exception e) {
            log.error("FeishuPublishCloudDocActivity: 云文档发布失败, connectionId={}, error={}",
                connectionId, e.getMessage(), e);
            throw new RuntimeException("飞书发布云文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String publishCloudDocWithConfig(Object connectionConfig, String title, String content) {
        log.info("FeishuPublishCloudDocActivity: 开始发布云文档(带配置), title={}", title);

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

            // 4. 创建云文档
            String documentId = handler.createDocument(token, title, null);
            log.info("FeishuPublishCloudDocActivity: 云文档创建成功, documentId={}", documentId);

            // 5. 返回文档 URL
            return "https://feishu.cn/docx/" + documentId;

        } catch (Exception e) {
            log.error("FeishuPublishCloudDocActivity: 云文档发布失败, error={}", e.getMessage(), e);
            throw new RuntimeException("飞书发布云文档失败: " + e.getMessage(), e);
        }
    }
}