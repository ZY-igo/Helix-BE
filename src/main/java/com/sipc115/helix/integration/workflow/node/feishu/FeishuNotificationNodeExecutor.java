/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu;

import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.integration.workflow.engine.ActivityFactory;
import com.sipc115.helix.integration.workflow.engine.ActivityInvocationSpec;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FeishuNotificationNodeExecutor implements WorkflowNodeExecutor {

    private static WorkflowTraceService traceService;

    @Autowired
    public void setTraceService(WorkflowTraceService traceService) {
        FeishuNotificationNodeExecutor.traceService = traceService;
    }

    @Override
    public boolean supports(String type) {
        return DslNodeType.FEISHU_NOTIFICATION.name().equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        NodeExecutionTraceEntity trace = null;
        if (traceService != null && context.getExecutionId() != null) {
            try {
                trace = traceService.startNodeExecution(
                    context.getExecutionId(),
                    node.getId(),
                    node.getType().name(),
                    "NORMAL",
                    context.getExecutionOrder(),
                    context.getVariables()
                );
                context.setCurrentNodeTraceId(trace.getId());
            } catch (Exception e) {
            }
        }

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
            if (traceService != null && trace != null) {
                try {
                    traceService.markNodeFailed(trace.getId(), e.getMessage(), getStackTrace(e));
                } catch (Exception ex) {
                }
            }
            throw new RuntimeException("Feishu notification failed: " + e.getMessage(), e);
        }

        NodeExecutionResult executionResult = NodeExecutionResult.completed();
        executionResult.setBranchKey(success ? "success" : "failure");

        if (result != null) {
            executionResult.getOutput().put("result", result);
        }

        if (traceService != null && trace != null) {
            try {
                traceService.markNodeSuccess(trace.getId(), executionResult.getOutput());
            } catch (Exception e) {
            }
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

    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
