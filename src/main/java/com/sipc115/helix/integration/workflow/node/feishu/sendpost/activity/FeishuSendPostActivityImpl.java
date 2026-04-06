/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpost.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.integration.connect.ConnectionClientRegistry;
import com.sipc115.helix.integration.connect.lark.FeishuApiHandler;
import com.sipc115.helix.integration.connect.lark.FeishuAuthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 飞书发送富文本消息 Activity 实现
 * <p>
 * 实现了 {@link FeishuSendPostActivity} 接口，
 * 负责在 Activity Worker 上执行飞书富文本消息发送 API 调用。
 *
 * <h3>与 NodeExecutor 的区别：</h3>
 * <ul>
 *   <li>NodeExecutor 运行在 Workflow 线程上，用于编排节点执行流程</li>
 *   <li>Activity 实现运行在 Activity Worker 上，执行真正的业务逻辑</li>
 *   <li>Activity 可以包含非确定性操作（HTTP 调用、文件 IO 等）</li>
 * </ul>
 *
 * <h3>Temporal 执行保证：</h3>
 * <ul>
 *   <li>Activity 执行失败时，Temporal 会根据配置的重试策略自动重试</li>
 *   <li>Activity 完成前，Workflow 状态不会固化为 History</li>
 *   <li>Activity 的执行结果是确定性的（相同输入 → 相同输出）</li>
 * </ul>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see FeishuSendPostActivity
 * @see FeishuSendPostNodeExecutor
 */
@Component
public class FeishuSendPostActivityImpl implements FeishuSendPostActivity {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendPostActivityImpl.class);

    /**
     * 连接客户端注册表
     */
    private static ConnectionClientRegistry connectionRegistry;

    @Autowired
    public void setConnectionRegistry(ConnectionClientRegistry connectionRegistry) {
        FeishuSendPostActivityImpl.connectionRegistry = connectionRegistry;
    }

    /**
     * 发送飞书富文本消息
     *
     * @param connectionId 飞书连接配置 ID
     * @param chatId 接收消息的会话 ID
     * @param title 消息标题
     * @param lines 内容行列表
     * @return 发送成功后的消息 ID
     */
    @Override
    public String sendPost(Long connectionId, String chatId, String title, List<String> lines) {
        log.info("FeishuSendPostActivity: 开始发送富文本消息, connectionId={}, chatId={}, title={}",
            connectionId, chatId, title);

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

            // 4. 发送富文本消息
            String messageId = handler.sendPost(token, chatId, title, lines);
            log.info("FeishuSendPostActivity: 富文本消息发送成功, connectionId={}, chatId={}, messageId={}",
                connectionId, chatId, messageId);

            return messageId;

        } catch (Exception e) {
            log.error("FeishuSendPostActivity: 富文本消息发送失败, connectionId={}, chatId={}, error={}",
                connectionId, chatId, e.getMessage(), e);
            throw new RuntimeException("飞书发送富文本消息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送飞书富文本消息（使用连接配置）
     */
    @Override
    public String sendPostWithConfig(Object connectionConfig, String chatId, String title, List<String> lines) {
        log.info("FeishuSendPostActivity: 开始发送富文本消息(带配置), chatId={}, title={}", chatId, title);

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

            // 4. 发送富文本消息
            String messageId = handler.sendPost(token, chatId, title, lines);
            log.info("FeishuSendPostActivity: 富文本消息发送成功, chatId={}, messageId={}", chatId, messageId);

            return messageId;

        } catch (Exception e) {
            log.error("FeishuSendPostActivity: 富文本消息发送失败, chatId={}, error={}", chatId, e.getMessage(), e);
            throw new RuntimeException("飞书发送富文本消息失败: " + e.getMessage(), e);
        }
    }
}