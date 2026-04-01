/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.feishu;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 飞书通知节点执行器
 */
@Component
public class FeishuNotificationNodeExecutor implements WorkflowNodeExecutor {

    @Override
    public boolean supports(String type) {
        return DslNodeType.FEISHU_NOTIFICATION.name().equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        Map<String, Object> config = node.getConfig();
        String action = (String) config.getOrDefault("action", "sendText");
        String chatId = getStringValue(config, "chatId");

        boolean success = false;
        String result = null;

        switch (action) {
            case "sendText":
                String text = getStringValue(config, "text");
                success = bridge.sendFeishuText(chatId, text);
                break;

            case "sendPost":
                String title = getStringValue(config, "title");
                List<String> lines = getStringListValue(config, "lines");
                success = bridge.sendFeishuPost(chatId, title, lines);
                break;

            case "sendPostWithLink":
                String postTitle = getStringValue(config, "title");
                String postText = getStringValue(config, "text");
                String url = getStringValue(config, "url");
                String linkText = getStringValue(config, "linkText");
                success = bridge.sendFeishuPostWithLink(chatId, postTitle, postText, url, linkText);
                break;

            case "publishCloudDoc":
                String docTitle = getStringValue(config, "title");
                String content = getStringValue(config, "content");
                result = bridge.publishFeishuCloudDoc(docTitle, content);
                success = result != null;
                break;

            default:
                throw new IllegalArgumentException("Unknown action type: " + action);
        }

        NodeExecutionResult executionResult = NodeExecutionResult.completed();
        executionResult.setBranchKey(success ? "success" : "failure");

        if (result != null) {
            executionResult.getOutput().put("result", result);
        }

        return executionResult;
    }

    /**
     * 安全地获取字符串值
     */
    private String getStringValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * 安全地获取字符串列表值
     */
    @SuppressWarnings("unchecked")
    private List<String> getStringListValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return null;
    }
}
