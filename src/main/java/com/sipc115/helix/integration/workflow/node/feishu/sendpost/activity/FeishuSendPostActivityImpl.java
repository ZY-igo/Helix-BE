/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpost.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.integration.connect.ConnectionClientRegistry;
import com.sipc115.helix.integration.connect.lark.FeishuApiHandler;
import com.sipc115.helix.integration.connect.lark.FeishuAuthClient;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
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
 * <h3>幂等性保护机制：</h3>
 * <p>
 * 为了防止 Temporal 重试导致重复发送消息，此 Activity 实现了幂等性保护：
 *
 * <h4>核心原理：</h4>
 * <ul>
 *   <li>每次 Activity 调用都携带 executionId + nodeId + retryCount 作为幂等键</li>
 *   <li>执行前查询 WorkflowTraceService，确认该节点是否已成功</li>
 *   <li>如果已成功，直接返回缓存的 messageId，不重复调用 API</li>
 *   <li>如果未成功，执行 API 发送并记录结果</li>
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

    private static ConnectionClientRegistry connectionRegistry;
    private static WorkflowTraceService traceService;

    @Autowired
    public void setConnectionRegistry(ConnectionClientRegistry connectionRegistry) {
        FeishuSendPostActivityImpl.connectionRegistry = connectionRegistry;
    }

    @Autowired
    public void setTraceService(WorkflowTraceService traceService) {
        FeishuSendPostActivityImpl.traceService = traceService;
    }

    @Override
    public String sendPost(Long executionId, Integer retryCount, String nodeId,
                          Long connectionId, String chatId, String title, List<String> lines) {
        log.info("FeishuSendPostActivity: 开始发送富文本消息, executionId={}, nodeId={}, retryCount={}, chatId={}",
            executionId, nodeId, retryCount, chatId);

        String attemptId = String.format("%d_%s_%d", executionId, nodeId, retryCount);
        String cachedMessageId = checkAlreadySucceeded(attemptId);
        if (cachedMessageId != null) {
            log.info("FeishuSendPostActivity: 检测到重复执行，返回缓存结果, attemptId={}, messageId={}",
                attemptId, cachedMessageId);
            return cachedMessageId;
        }

        try {
            FeishuAuthClient authClient = connectionRegistry.getOrCreateClientByConnection(connectionId);
            String token = authClient.getToken();
            log.debug("获取飞书访问令牌成功, connectionId={}", connectionId);

            FeishuApiHandler handler = new FeishuApiHandler(
                new ObjectMapper(),
                RestClient.builder()
            );

            String messageId = handler.sendPost(token, chatId, title, lines);
            log.info("FeishuSendPostActivity: 富文本消息发送成功, attemptId={}, messageId={}",
                attemptId, messageId);

            return messageId;

        } catch (Exception e) {
            log.error("FeishuSendPostActivity: 富文本消息发送失败, attemptId={}, error={}",
                attemptId, e.getMessage(), e);
            throw new RuntimeException("飞书发送富文本消息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String sendPostWithConfig(Long executionId, Integer retryCount, String nodeId,
                                   Object connectionConfig, String chatId, String title, List<String> lines) {
        log.info("FeishuSendPostActivity: 开始发送富文本消息(带配置), executionId={}, nodeId={}, retryCount={}, chatId={}",
            executionId, nodeId, retryCount, chatId);

        String attemptId = String.format("%d_%s_%d", executionId, nodeId, retryCount);
        String cachedMessageId = checkAlreadySucceeded(attemptId);
        if (cachedMessageId != null) {
            log.info("FeishuSendPostActivity: 检测到重复执行，返回缓存结果, attemptId={}, messageId={}",
                attemptId, cachedMessageId);
            return cachedMessageId;
        }

        try {
            FeishuAuthClient authClient = connectionRegistry.getOrCreateClient(
                null,
                "FEISHU",
                connectionConfig
            );

            String token = authClient.getToken();
            log.debug("获取飞书访问令牌成功");

            FeishuApiHandler handler = new FeishuApiHandler(
                new ObjectMapper(),
                RestClient.builder()
            );

            String messageId = handler.sendPost(token, chatId, title, lines);
            log.info("FeishuSendPostActivity: 富文本消息发送成功, attemptId={}, messageId={}", attemptId, messageId);

            return messageId;

        } catch (Exception e) {
            log.error("FeishuSendPostActivity: 富文本消息发送失败, attemptId={}, error={}", attemptId, e.getMessage(), e);
            throw new RuntimeException("飞书发送富文本消息失败: " + e.getMessage(), e);
        }
    }

    private String checkAlreadySucceeded(String attemptId) {
        if (traceService == null) {
            log.debug("WorkflowTraceService 未注入，跳过幂等性检查");
            return null;
        }

        try {
            String[] parts = attemptId.split("_");
            if (parts.length < 3) {
                log.warn("无效的 attemptId 格式: {}", attemptId);
                return null;
            }

            long executionId = Long.parseLong(parts[0]);
            String nodeId = parts[1];

            var traces = traceService.getNodeTracesByNodeId(executionId, nodeId);

            for (var trace : traces) {
                if ("SUCCESS".equals(trace.getStatus()) && trace.getOutput() != null) {
                    Object messageId = trace.getOutput().get("messageId");
                    if (messageId != null) {
                        log.info("检测到节点已成功执行过, executionId={}, nodeId={}, cachedMessageId={}",
                            executionId, nodeId, messageId);
                        return messageId.toString();
                    }
                }
            }

            log.debug("节点未执行过或未成功: attemptId={}", attemptId);
            return null;

        } catch (Exception e) {
            log.warn("检查节点成功状态异常，跳过幂等检查: attemptId={}, error={}",
                attemptId, e.getMessage());
            return null;
        }
    }
}