/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpost;

import com.sipc115.helix.common.constant.BranchKeyConstants;
import com.sipc115.helix.common.constant.WorkflowConstants;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.engine.ActivityInvocationSpec;
import com.sipc115.helix.integration.workflow.node.feishu.sendpost.activity.FeishuSendPostActivity;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 飞书发送富文本消息节点执行器
 * <p>
 * 负责向飞书群聊或个人会话发送富文本（Post）消息。
 *
 * <h3>DSL 配置示例：</h3>
 * <pre>
 * {
 *   "type": "FEISHU_SEND_POST",
 *   "config": {
 *     "connectionId": 123,
 *     "chatId": "oc_xxxx",
 *     "title": "通知",
 *     "lines": ["第一行内容", "第二行内容"]
 *   }
 * }
 * </pre>
 *
 * <h3>架构说明（使用 Temporal Activity）：</h3>
 * <p>
 * 此执行器将 API 调用委托给 Temporal Activity 执行，
 * 保证 Workflow 的确定性。
 *
 * <h3>幂等性保护：</h3>
 * <p>
 * 通过传递 executionId、nodeId、retryCount 给 Activity，
 * Activity 可以实现幂等性检查，避免 Temporal 重试导致重复发送消息。
 *
 * <h3>执行流程：</h3>
 * <pre>
 * Temporal Workflow Thread
 *   └─ FeishuSendPostNodeExecutor.execute()
 *       └─ bridge.activities().getActivity(FeishuSendPostActivity.class)
 *           └─ FeishuSendPostActivity.sendPost()
 *               └─ 幂等性检查
 *               └─ FeishuApiHandler.sendPost()    ← 真正的 HTTP 调用
 * </pre>
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowNodeExecutor
 * @see FeishuSendPostActivity
 */
@Component
public class FeishuSendPostNodeExecutor implements WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendPostNodeExecutor.class);

    @Override
    public boolean supports(String type) {
        return DslNodeType.FEISHU_SEND_POST.name().equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        Map<String, Object> config = node.getConfig();
        Long connectionId = getLongValue(config, "connectionId");
        if (connectionId == null) {
            throw new IllegalArgumentException("节点配置错误: connectionId 不能为空，节点ID: " + node.getId());
        }
        String chatId = getStringValue(config, "chatId");
        String title = getStringValue(config, "title");
        List<String> lines = getLinesValue(config, "lines");

        Map<String, Object> output = new HashMap<>();
        output.put("action", "sendPost");
        output.put("connectionId", connectionId);
        output.put("chatId", chatId);
        output.put("title", title);

        boolean success = false;
        try {
            ActivityInvocationSpec activitySpec = ActivityInvocationSpec.fromNodeConfig(config);
            FeishuSendPostActivity activity = bridge.activities().getActivity(FeishuSendPostActivity.class, activitySpec);

            Long executionId = context.getExecutionId();
            String nodeId = node.getId();

            String messageId;
            Object cachedConfig = config.get(WorkflowConstants.CONNECTION_CONFIG_KEY);
            if (cachedConfig != null) {
                messageId = activity.sendPostWithConfig(
                    executionId, 0, nodeId,
                    cachedConfig, chatId, title, lines);
            } else {
                messageId = activity.sendPost(
                    executionId, 0, nodeId,
                    connectionId, chatId, title, lines);
            }

            success = messageId != null && !messageId.isEmpty();
            output.put("success", success);
            output.put("messageId", messageId);
            output.put("timestamp", System.currentTimeMillis());

            log.info("发送富文本消息完成: connectionId={}, chatId={}, success={}", connectionId, chatId, success);
        } catch (Exception e) {
            output.put("success", false);
            output.put("error", e.getMessage());
            throw new RuntimeException("飞书发送富文本消息失败: " + e.getMessage(), e);
        }

        NodeExecutionResult result = NodeExecutionResult.completed();
        result.setBranchKey(success ? BranchKeyConstants.SUCCESS : BranchKeyConstants.FAILURE);
        result.setOutput(output);
        return result;
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

    @SuppressWarnings("unchecked")
    private List<String> getLinesValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value == null) return new ArrayList<>();
        if (value instanceof List) {
            return (List<String>) value;
        }
        return List.of(value.toString());
    }
}
