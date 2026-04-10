/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpostwithlink;

import com.sipc115.helix.common.constant.BranchKeyConstants;
import com.sipc115.helix.common.constant.WorkflowConstants;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.node.feishu.sendpostwithlink.activity.FeishuSendPostWithLinkActivity;
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
 * 飞书发送带链接富文本消息节点执行器
 * <p>
 * 负责向飞书群聊或个人会话发送带链接的富文本消息。
 *
 * <h3>DSL 配置示例：</h3>
 * <pre>
 * {
 *   "type": "FEISHU_SEND_POST_WITH_LINK",
 *   "config": {
 *     "connectionId": 123,
 *     "chatId": "oc_xxxx",
 *     "title": "请查收",
 *     "text": "点击下方链接查看详情",
 *     "url": "https://example.com/page",
 *     "linkText": "查看详情"
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
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowNodeExecutor
 * @see FeishuSendPostWithLinkActivity
 */
@Component
public class FeishuSendPostWithLinkNodeExecutor implements WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendPostWithLinkNodeExecutor.class);

    @Override
    public boolean supports(String type) {
        return DslNodeType.FEISHU_SEND_POST_WITH_LINK.name().equals(type);
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
        String text = getStringValue(config, "text");
        String url = getStringValue(config, "url");
        String linkText = getStringValue(config, "linkText");

        Map<String, Object> output = new HashMap<>();
        output.put("action", "sendPostWithLink");
        output.put("connectionId", connectionId);
        output.put("chatId", chatId);
        output.put("title", title);

        boolean success = false;
        try {
            FeishuSendPostWithLinkActivity activity = bridge.activities().getActivity(FeishuSendPostWithLinkActivity.class);

            Long executionId = context.getExecutionId();
            String nodeId = node.getId();

            Object cachedConfig = config.get(WorkflowConstants.CONNECTION_CONFIG_KEY);
            if (cachedConfig != null) {
                activity.sendPostWithLinkWithConfig(
                    executionId, 0, nodeId,
                    cachedConfig, chatId, title, text, url, linkText);
            } else {
                activity.sendPostWithLink(
                    executionId, 0, nodeId,
                    connectionId, chatId, title, text, url, linkText);
            }

            success = true;
            output.put("success", success);
            output.put("timestamp", System.currentTimeMillis());

            log.info("发送带链接富文本消息完成: connectionId={}, chatId={}, success={}", connectionId, chatId, success);
        } catch (Exception e) {
            output.put("success", false);
            output.put("error", e.getMessage());
            throw new RuntimeException("飞书发送带链接富文本消息失败: " + e.getMessage(), e);
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