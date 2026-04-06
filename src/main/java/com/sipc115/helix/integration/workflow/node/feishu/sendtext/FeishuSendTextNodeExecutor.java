/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendtext;

import com.sipc115.helix.common.constant.NodeRoleConstants;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.integration.workflow.node.feishu.sendtext.activity.FeishuSendTextActivity;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 飞书发送文本消息节点执行器
 * <p>
 * 负责向飞书群聊或个人会话发送文本消息。
 *
 * <h3>DSL 配置示例：</h3>
 * <pre>
 * {
 *   "type": "FEISHU_SEND_TEXT",
 *   "config": {
 *     "connectionId": 123,
 *     "chatId": "oc_xxxx",
 *     "text": "Hello, ${user.name}!"
 *   }
 * }
 * </pre>
 *
 * <h3>架构说明（使用 Temporal Activity）：</h3>
 * <p>
 * 此执行器将 API 调用委托给 Temporal Activity 执行，
 * 保证 Workflow 的确定性。
 *
 * <h3>执行流程：</h3>
 * <pre>
 * Temporal Workflow Thread
 *   └─ FeishuSendTextNodeExecutor.execute()
 *       └─ bridge.activities().getActivity(FeishuSendTextActivity.class)
 *           └─ FeishuSendTextActivity.sendText()  ← 在 Activity Worker 上执行
 *               └─ FeishuApiHandler.sendText()   ← 真正的 HTTP 调用
 * </pre>
 *
 * <h3>与旧架构对比：</h3>
 * <pre>
 * 旧架构（直接调用 - 违反确定性原则）：
 *   Executor → FeishuAuthClient → FeishuApiHandler  ❌ HTTP 调用在 Workflow 线程
 *
 * 新架构（Activity 委托 - 正确做法）：
 *   Executor → FeishuSendTextActivity → FeishuApiHandler  ✅ HTTP 调用在 Activity Worker
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowNodeExecutor
 * @see FeishuSendTextActivity
 */
@Component
public class FeishuSendTextNodeExecutor implements WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendTextNodeExecutor.class);
    private static WorkflowTraceService traceService;

    @Autowired
    public void setTraceService(WorkflowTraceService traceService) {
        FeishuSendTextNodeExecutor.traceService = traceService;
    }

    @Override
    public boolean supports(String type) {
        return DslNodeType.FEISHU_SEND_TEXT.name().equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        NodeExecutionTraceEntity trace = startTrace(node, context);

        Map<String, Object> config = node.getConfig();
        Long connectionId = getLongValue(config, "connectionId");
        if (connectionId == null) {
            String errorMsg = String.format(
                "节点配置错误: connectionId 不能为空\n" +
                "节点ID: %s\n" +
                "当前配置: %s\n" +
                "请在 DSL 中配置 connectionId 字段，指向有效的飞书集成连接",
                node.getId(),
                config.keySet()
            );
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        String chatId = getStringValue(config, "chatId");
        String text = getStringValue(config, "text");

        Map<String, Object> output = new HashMap<>();
        output.put("action", "sendText");
        output.put("connectionId", connectionId);
        output.put("chatId", chatId);
        output.put("text", text);

        boolean success = false;
        try {
            // 通过 Bridge 获取 Temporal Activity 存根
            // Activity 调用会在 Activity Worker 上执行，而不是 Workflow 线程
            FeishuSendTextActivity activity = bridge.activities().getActivity(FeishuSendTextActivity.class);

            String messageId;
            Object cachedConfig = config.get("_connectionConfig");
            if (cachedConfig != null) {
                // 使用连接配置调用（避免重复查询）
                messageId = activity.sendTextWithConfig(cachedConfig, chatId, text);
            } else {
                // 使用 connectionId 调用
                messageId = activity.sendText(connectionId, chatId, text);
            }

            success = messageId != null && !messageId.isEmpty();
            output.put("success", success);
            output.put("messageId", messageId);
            output.put("timestamp", System.currentTimeMillis());

            markNodeSuccess(trace, output);
            log.info("发送文本消息完成: connectionId={}, chatId={}, success={}", connectionId, chatId, success);
        } catch (Exception e) {
            output.put("success", false);
            output.put("error", e.getMessage());
            markNodeFailed(trace, e.getMessage());
            throw new RuntimeException("飞书发送文本消息失败: " + e.getMessage(), e);
        }

        NodeExecutionResult result = NodeExecutionResult.completed();
        result.setBranchKey(success ? "success" : "failure");
        result.setOutput(output);
        return result;
    }

    /**
     * 启动节点追踪
     */
    private NodeExecutionTraceEntity startTrace(CompiledNode node, ExecutionContext context) {
        if (traceService == null || context.getExecutionId() == null) {
            return null;
        }
        try {
            NodeExecutionTraceEntity trace = traceService.startNodeExecution(
                context.getExecutionId(),
                node.getId(),
                node.getType().name(),
                NodeRoleConstants.NORMAL,
                context.getExecutionOrder(),
                context.getVariables()
            );
            context.setCurrentNodeTraceId(trace.getId());
            return trace;
        } catch (Exception e) {
            log.warn("启动节点追踪失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 标记节点执行成功
     */
    private void markNodeSuccess(NodeExecutionTraceEntity trace, Map<String, Object> output) {
        if (traceService != null && trace != null) {
            try {
                traceService.markNodeSuccess(trace.getId(), output);
            } catch (Exception e) {
                log.warn("标记节点成功失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 标记节点执行失败
     */
    private void markNodeFailed(NodeExecutionTraceEntity trace, String errorMessage) {
        if (traceService != null && trace != null) {
            try {
                traceService.markNodeFailed(trace.getId(), errorMessage, null);
            } catch (Exception e) {
                log.warn("标记节点失败失败: {}", e.getMessage());
            }
        }
    }

    private String getStringValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLongValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
}