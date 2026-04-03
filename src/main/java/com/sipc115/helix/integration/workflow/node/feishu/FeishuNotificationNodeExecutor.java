/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.engine.ActivityFactory;
import com.sipc115.helix.integration.workflow.engine.ActivityInvocationSpec;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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

        ActivityInvocationSpec spec = ActivityInvocationSpec.fromNodeConfig(config);
        ActivityFactory factory = bridge.activities();

        boolean success = false;
        String result = null;

        try {
            switch (action) {
                case "sendText":
                    String text = getStringValue(config, "text");
                    FeishuSendTextActivity sendTextActivity = factory.getActivity(FeishuSendTextActivity.class, spec);
                    success = sendTextActivity.sendText(chatId, text);
                    break;

                case "sendPost":
                    String title = getStringValue(config, "title");
                    List<String> lines = getStringListValue(config, "lines");
                    FeishuSendPostActivity sendPostActivity = factory.getActivity(FeishuSendPostActivity.class, spec);
                    success = sendPostActivity.sendPost(chatId, title, lines);
                    break;

                case "sendPostWithLink":
                    String postTitle = getStringValue(config, "title");
                    String postText = getStringValue(config, "text");
                    String url = getStringValue(config, "url");
                    String linkText = getStringValue(config, "linkText");
                    FeishuSendPostWithLinkActivity sendPostWithLinkActivity = factory.getActivity(FeishuSendPostWithLinkActivity.class, spec);
                    success = sendPostWithLinkActivity.sendPostWithLink(chatId, postTitle, postText, url, linkText);
                    break;

                case "publishCloudDoc":
                    String docTitle = getStringValue(config, "title");
                    String content = getStringValue(config, "content");
                    FeishuPublishCloudDocActivity publishCloudDocActivity = factory.getActivity(FeishuPublishCloudDocActivity.class, spec);
                    result = publishCloudDocActivity.publishCloudDoc(docTitle, content);
                    success = result != null;
                    break;

                default:
                    throw new IllegalArgumentException("Unknown action type: " + action);
            }
        } catch (Exception e) {
            throw new RuntimeException("Feishu notification failed: " + e.getMessage(), e);
        }

        NodeExecutionResult executionResult = NodeExecutionResult.completed();
        executionResult.setBranchKey(success ? "success" : "failure");

        if (result != null) {
            executionResult.getOutput().put("result", result);
        }

        return executionResult;
    }

    private String getStringValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value != null ? value.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringListValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value instanceof List ? (List<String>) value : null;
    }
}
