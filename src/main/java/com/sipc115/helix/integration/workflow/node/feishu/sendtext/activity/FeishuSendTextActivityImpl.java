/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendtext.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import com.sipc115.helix.integration.connect.ConnectionClientRegistry;
import com.sipc115.helix.integration.connect.lark.FeishuApiHandler;
import com.sipc115.helix.integration.connect.lark.FeishuAuthClient;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 飞书发送文本消息 Activity 实现
 * <p>
 * 实现了 {@link FeishuSendTextActivity} 接口，
 * 负责在 Activity Worker 上执行飞书 API 调用。
 *
 * <h3>幂等性保护机制：</h3>
 * <p>
 * 为了防止 Temporal 重试导致重复发送消息，此 Activity 实现了幂等性保护：
 *
 * <h4>核心原理：</h4>
 * <ul>
 *   <li>每次 Activity 调用都携带 executionId + nodeId + retryCount 作为幂等键</li>
 *   <li>执行前查询 WorkflowTraceService，确认该 attempt 是否已成功</li>
 *   <li>如果已成功，直接返回缓存的 messageId，不重复调用 API</li>
 *   <li>如果未成功，执行 API 发送并记录结果</li>
 * </ul>
 *
 * <h4>为什么需要幂等性：</h4>
 * <pre>
 * Temporal Activity 重试场景：
 * 1. Activity 执行成功，但结果返回超时
 * 2. Temporal 认为 Activity 失败，重新调度执行
 * 3. 如果没有幂等保护，飞书会收到两条消息 ❌
 *
 * 有幂等保护：
 * 1. Activity 执行成功，结果返回超时
 * 2. Temporal 重试，Activity 被调用
 * 3. Activity 检查发现该 attempt 已成功
 * 4. 直接返回缓存的 messageId，不会重复发送 ✅
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see FeishuSendTextActivity
 */
@Component
public class FeishuSendTextActivityImpl implements FeishuSendTextActivity {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendTextActivityImpl.class);

    private static ConnectionClientRegistry connectionRegistry;
    private static WorkflowTraceService traceService;

    @Autowired
    public void setConnectionRegistry(ConnectionClientRegistry connectionRegistry) {
        FeishuSendTextActivityImpl.connectionRegistry = connectionRegistry;
    }

    @Autowired
    public void setTraceService(WorkflowTraceService traceService) {
        FeishuSendTextActivityImpl.traceService = traceService;
    }

    /**
     * 发送飞书文本消息
     * <p>
     * 实现了幂等性保护，防止重复发送。
     *
     * <h3>执行流程：</h3>
     * <pre>
     * 1. 生成 attemptId = {executionId}_{nodeId}_{retryCount}
     * 2. 检查该 attempt 是否已成功
     *    ├── 已成功 → 返回缓存的 messageId
     *    └── 未成功 → 继续执行
     * 3. 获取飞书认证
     * 4. 调用飞书 API 发送消息
     * 5. 返回 messageId
     * </pre>
     *
     * @param executionId 工作流执行 ID
     * @param retryCount 重试次数
     * @param nodeId 节点 ID
     * @param connectionId 飞书连接配置 ID
     * @param chatId 接收消息的会话 ID
     * @param text 消息内容
     * @return 发送成功后的消息 ID
     */
    @Override
    public String sendText(Long executionId, Integer retryCount, String nodeId,
                          Long connectionId, String chatId, String text) {
        log.info("FeishuSendTextActivity: 开始发送文本消息, executionId={}, nodeId={}, retryCount={}, chatId={}",
            executionId, nodeId, retryCount, chatId);

        // 1. 幂等性检查
        String attemptId = String.format("%d_%s_%d", executionId, nodeId, retryCount);
        String cachedMessageId = checkAlreadySucceeded(attemptId);
        if (cachedMessageId != null) {
            log.info("FeishuSendTextActivity: 检测到重复执行，返回缓存结果, attemptId={}, messageId={}",
                attemptId, cachedMessageId);
            return cachedMessageId;
        }

        // 2. 记录开始节点追踪
        Long traceId = startNodeTracking(executionId, nodeId);

        try {
            // 3. 获取飞书认证客户端
            FeishuAuthClient authClient = connectionRegistry.getOrCreateClientByConnection(connectionId);

            // 4. 获取访问令牌
            String token = authClient.getToken();
            log.debug("获取飞书访问令牌成功, connectionId={}", connectionId);

            // 5. 创建飞书 API 处理器
            FeishuApiHandler handler = new FeishuApiHandler(
                new ObjectMapper(),
                RestClient.builder()
            );

            // 6. 发送消息
            String messageId = handler.sendText(token, chatId, text);
            log.info("FeishuSendTextActivity: 消息发送成功, attemptId={}, messageId={}",
                attemptId, messageId);

            // 7. 记录成功状态
            Map<String, Object> successOutput = new HashMap<>();
            successOutput.put("messageId", messageId);
            markNodeSuccess(traceId, successOutput);

            return messageId;

        } catch (Exception e) {
            log.error("FeishuSendTextActivity: 消息发送失败, attemptId={}, error={}",
                attemptId, e.getMessage(), e);
            markNodeFailed(traceId, e.getMessage());
            throw new RuntimeException("飞书发送文本消息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送飞书文本消息（使用连接配置）
     */
    @Override
    public String sendTextWithConfig(Long executionId, Integer retryCount, String nodeId,
                                   Object connectionConfig, String chatId, String text) {
        log.info("FeishuSendTextActivity: 开始发送文本消息(带配置), executionId={}, nodeId={}, retryCount={}, chatId={}",
            executionId, nodeId, retryCount, chatId);

        // 1. 幂等性检查
        String attemptId = String.format("%d_%s_%d", executionId, nodeId, retryCount);
        String cachedMessageId = checkAlreadySucceeded(attemptId);
        if (cachedMessageId != null) {
            log.info("FeishuSendTextActivity: 检测到重复执行，返回缓存结果, attemptId={}, messageId={}",
                attemptId, cachedMessageId);
            return cachedMessageId;
        }

        // 2. 记录开始节点追踪
        Long traceId = startNodeTracking(executionId, nodeId);

        try {
            // 3. 获取飞书认证客户端（使用连接配置）
            FeishuAuthClient authClient = connectionRegistry.getOrCreateClient(
                null,
                "FEISHU",
                connectionConfig
            );

            // 4. 获取访问令牌
            String token = authClient.getToken();
            log.debug("获取飞书访问令牌成功");

            // 5. 创建飞书 API 处理器
            FeishuApiHandler handler = new FeishuApiHandler(
                new ObjectMapper(),
                RestClient.builder()
            );

            // 6. 发送消息
            String messageId = handler.sendText(token, chatId, text);
            log.info("FeishuSendTextActivity: 消息发送成功, attemptId={}, messageId={}",
                attemptId, messageId);

            // 7. 记录成功状态
            Map<String, Object> successOutput = new HashMap<>();
            successOutput.put("messageId", messageId);
            markNodeSuccess(traceId, successOutput);

            return messageId;

        } catch (Exception e) {
            log.error("FeishuSendTextActivity: 消息发送失败, attemptId={}, error={}",
                attemptId, e.getMessage(), e);
            markNodeFailed(traceId, e.getMessage());
            throw new RuntimeException("飞书发送文本消息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查节点是否已成功执行过
     * <p>
     * 通过 executionId + nodeId 查询 WorkflowTraceService，
     * 判断该节点是否已经在之前的 attempt 中成功执行过。
     *
     * <h3>重要设计决策：</h3>
     * <ul>
     *   <li>检查基于 executionId + nodeId，而非完整的 attemptId</li>
     *   <li>这是因为 Temporal 重试时 retryCount 会变化（0→1→2...）</li>
     *   <li>如果第一次执行成功了，但结果返回超时，Temporal 会重试</li>
     *   <li>重试时 retryCount=1，但节点实际上已经成功过了</li>
     *   <li>此时应该复用第一次成功的 messageId，而不是重新发送</li>
     * </ul>
     *
     * <h3>场景示例：</h3>
     * <pre>
     * 1. 首次执行: attemptId = 123_nodeA_0 → 成功，messageId = "msg_abc"
     * 2. 结果返回超时，Temporal 重试
     * 3. 重试执行: attemptId = 123_nodeA_1
     * 4. 检查发现 123_nodeA_0 已成功 → 返回 "msg_abc" ✅
     * </pre>
     *
     * <h3>边界情况：</h3>
     * <ul>
     *   <li>如果 traceService 为 null，跳过检查，继续执行</li>
     *   <li>如果查询异常，跳过检查，继续执行（避免阻塞业务）</li>
     * </ul>
     *
     * @param attemptId 幂等键（格式: executionId_nodeId_retryCount）
     * @return 如果已成功返回 messageId，否则返回 null
     */
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

    private Long startNodeTracking(Long executionId, String nodeId) {
        if (traceService == null) {
            return null;
        }
        try {
            var trace = traceService.startNodeExecution(
                executionId, nodeId, "FeishuSendText",
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