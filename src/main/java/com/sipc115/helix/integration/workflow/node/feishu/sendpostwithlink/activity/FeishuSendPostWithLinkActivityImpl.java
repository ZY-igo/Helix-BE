/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpostwithlink.activity;

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

import java.util.HashMap;
import java.util.Map;

/**
 * 飞书发送带链接富文本消息 Activity 实现
 * <p>
 * 实现了 {@link FeishuSendPostWithLinkActivity} 接口，
 * 负责在 Activity Worker 上执行飞书带链接富文本消息发送 API 调用。
 *
 * <h3>幂等性保护机制：</h3>
 * <p>
 * 为了防止 Temporal 重试导致重复发送消息，此 Activity 实现了幂等性保护：
 * 执行前检查该节点是否已成功，如果已成功则直接返回，不重复调用 API。
 *
 * @author Helix Team
 * @since 2.0.0
 * @see FeishuSendPostWithLinkActivity
 */
@Component
public class FeishuSendPostWithLinkActivityImpl implements FeishuSendPostWithLinkActivity {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendPostWithLinkActivityImpl.class);

    private static ConnectionClientRegistry connectionRegistry;
    private static WorkflowTraceService traceService;

    @Autowired
    public void setConnectionRegistry(ConnectionClientRegistry connectionRegistry) {
        FeishuSendPostWithLinkActivityImpl.connectionRegistry = connectionRegistry;
    }

    @Autowired
    public void setTraceService(WorkflowTraceService traceService) {
        FeishuSendPostWithLinkActivityImpl.traceService = traceService;
    }

    @Override
    public void sendPostWithLink(Long executionId, Integer retryCount, String nodeId,
                                 Long connectionId, String chatId, String title, String text, String url, String linkText) {
        log.info("FeishuSendPostWithLinkActivity: 开始发送带链接富文本消息, executionId={}, nodeId={}, retryCount={}, chatId={}",
            executionId, nodeId, retryCount, chatId);

        String attemptId = String.format("%d_%s_%d", executionId, nodeId, retryCount);
        if (checkAlreadySucceeded(attemptId)) {
            log.info("FeishuSendPostWithLinkActivity: 检测到重复执行，跳过发送, attemptId={}", attemptId);
            return;
        }

        Long traceId = startNodeTracking(executionId, nodeId);

        try {
            FeishuAuthClient authClient = connectionRegistry.getOrCreateClientByConnection(connectionId);
            String token = authClient.getToken();
            log.debug("获取飞书访问令牌成功, connectionId={}", connectionId);

            FeishuApiHandler handler = new FeishuApiHandler(
                new ObjectMapper(),
                RestClient.builder()
            );

            handler.sendPostWithLink(token, chatId, title, text, url, linkText);
            log.info("FeishuSendPostWithLinkActivity: 带链接富文本消息发送成功, attemptId={}", attemptId);

            markNodeSuccess(traceId, new HashMap<>());

        } catch (Exception e) {
            log.error("FeishuSendPostWithLinkActivity: 带链接富文本消息发送失败, attemptId={}, error={}",
                attemptId, e.getMessage(), e);
            markNodeFailed(traceId, e.getMessage());
            throw new RuntimeException("飞书发送带链接富文本消息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendPostWithLinkWithConfig(Long executionId, Integer retryCount, String nodeId,
                                          Object connectionConfig, String chatId, String title, String text, String url, String linkText) {
        log.info("FeishuSendPostWithLinkActivity: 开始发送带链接富文本消息(带配置), executionId={}, nodeId={}, retryCount={}, chatId={}",
            executionId, nodeId, retryCount, chatId);

        String attemptId = String.format("%d_%s_%d", executionId, nodeId, retryCount);
        if (checkAlreadySucceeded(attemptId)) {
            log.info("FeishuSendPostWithLinkActivity: 检测到重复执行，跳过发送, attemptId={}", attemptId);
            return;
        }

        Long traceId = startNodeTracking(executionId, nodeId);

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

            handler.sendPostWithLink(token, chatId, title, text, url, linkText);
            log.info("FeishuSendPostWithLinkActivity: 带链接富文本消息发送成功, attemptId={}", attemptId);

            markNodeSuccess(traceId, new HashMap<>());

        } catch (Exception e) {
            log.error("FeishuSendPostWithLinkActivity: 带链接富文本消息发送失败, attemptId={}, error={}",
                attemptId, e.getMessage(), e);
            markNodeFailed(traceId, e.getMessage());
            throw new RuntimeException("飞书发送带链接富文本消息失败: " + e.getMessage(), e);
        }
    }

    private boolean checkAlreadySucceeded(String attemptId) {
        if (traceService == null) {
            log.debug("WorkflowTraceService 未注入，跳过幂等性检查");
            return false;
        }

        try {
            String[] parts = attemptId.split("_");
            if (parts.length < 3) {
                log.warn("无效的 attemptId 格式: {}", attemptId);
                return false;
            }

            long executionId = Long.parseLong(parts[0]);
            String nodeId = parts[1];

            var traces = traceService.getNodeTracesByNodeId(executionId, nodeId);

            for (var trace : traces) {
                if ("SUCCESS".equals(trace.getStatus())) {
                    log.info("检测到节点已成功执行过, executionId={}, nodeId={}, attemptId={}",
                        executionId, nodeId, attemptId);
                    return true;
                }
            }

            log.debug("节点未执行过或未成功: attemptId={}", attemptId);
            return false;

        } catch (Exception e) {
            log.warn("检查节点成功状态异常，跳过幂等检查: attemptId={}, error={}",
                attemptId, e.getMessage());
            return false;
        }
    }

    private Long startNodeTracking(Long executionId, String nodeId) {
        if (traceService == null) {
            return null;
        }
        try {
            var trace = traceService.startNodeExecution(
                executionId, nodeId, "FeishuSendPostWithLink",
                "NORMAL", 0, null, 0
            );
            return trace != null ? trace.getId() : null;
        } catch (Exception e) {
            log.warn("启动节点追踪失败: {}", e.getMessage());
            return null;
        }
    }

    private void markNodeSuccess(Long traceId, Map<String, Object> output) {
        if (traceService == null || traceId == null) {
            return;
        }
        try {
            traceService.markNodeSuccess(traceId, output);
        } catch (Exception e) {
            log.warn("标记节点成功失败: {}", e.getMessage());
        }
    }

    private void markNodeFailed(Long traceId, String errorMessage) {
        if (traceService == null || traceId == null) {
            return;
        }
        try {
            traceService.markNodeFailed(traceId, errorMessage, null);
        } catch (Exception e) {
            log.warn("标记节点失败失败: {}", e.getMessage());
        }
    }
}