/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendtext;

import com.sipc115.helix.common.constant.BranchKeyConstants;
import com.sipc115.helix.common.constant.WorkflowConstants;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.engine.ActivityInvocationSpec;
import com.sipc115.helix.integration.workflow.node.feishu.sendtext.activity.FeishuSendTextActivity;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <h3>幂等性保护：</h3>
 * <p>
 * 通过传递 executionId、nodeId、retryCount 给 Activity，
 * Activity 可以实现幂等性检查，避免 Temporal 重试导致重复发送消息。
 *
 * <h3>执行流程：</h3>
 * <pre>
 * Temporal Workflow Thread
 *   └─ FeishuSendTextNodeExecutor.execute()
 *       └─ bridge.activities().getActivity(FeishuSendTextActivity.class)
 *           └─ FeishuSendTextActivity.sendText()
 *               └─ 幂等性检查 (通过 executionId + nodeId + retryCount)
 *               └─ FeishuApiHandler.sendText()  ← 在 Activity Worker 上执行
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

    @Override
    public boolean supports(String type) {
        return DslNodeType.FEISHU_SEND_TEXT.name().equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        // 注意: Workflow 线程中不能执行阻塞式 DB 操作(如节点追踪)
        // 节点追踪已移至 Activity 层或通过 MQ 异步处理

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
            ActivityInvocationSpec activitySpec = ActivityInvocationSpec.fromNodeConfig(config);
            FeishuSendTextActivity activity = bridge.activities().getActivity(FeishuSendTextActivity.class, activitySpec);

            // 获取幂等性参数
            Long executionId = context.getExecutionId();
            String nodeId = node.getId();

            String messageId;
            Object cachedConfig = config.get(WorkflowConstants.CONNECTION_CONFIG_KEY);
            if (cachedConfig != null) {
                // 使用连接配置调用（避免重复查询）
                // 传递幂等性参数给 Activity
                messageId = activity.sendTextWithConfig(
                    executionId, 0, nodeId,
                    cachedConfig, chatId, text
                );
            } else {
                // 使用 connectionId 调用
                // 传递幂等性参数给 Activity
                messageId = activity.sendText(
                    executionId, 0, nodeId,
                    connectionId, chatId, text
                );
            }

            success = messageId != null && !messageId.isEmpty();
            output.put("success", success);
            output.put("messageId", messageId);
            output.put("timestamp", System.currentTimeMillis());

            log.info("发送文本消息完成: connectionId={}, chatId={}, success={}, messageId={}",
                connectionId, chatId, success, messageId);
        } catch (Exception e) {
            output.put("success", false);
            output.put("error", e.getMessage());
            throw new RuntimeException("飞书发送文本消息失败: " + e.getMessage(), e);
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
}
