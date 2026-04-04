/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpostwithlink;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 飞书发送带链接富文本消息节点执行器
 *
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class FeishuSendPostWithLinkNodeExecutor implements WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendPostWithLinkNodeExecutor.class);
    private static WorkflowTraceService traceService;

    @Autowired
    public void setTraceService(WorkflowTraceService traceService) {
        FeishuSendPostWithLinkNodeExecutor.traceService = traceService;
    }

    @Override
    public boolean supports(String type) {
        return DslNodeType.FEISHU_SEND_POST_WITH_LINK.name().equals(type);
    }

    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        NodeExecutionTraceEntity trace = startTrace(node, context);

        Map<String, Object> config = node.getConfig();
        String chatId = getStringValue(config, "chatId");
        String title = getStringValue(config, "title");
        String text = getStringValue(config, "text");
        String url = getStringValue(config, "url");
        String linkText = getStringValue(config, "linkText");

        Map<String, Object> output = new HashMap<>();
        output.put("action", "sendPostWithLink");
        output.put("chatId", chatId);
        output.put("title", title);
        output.put("text", text);
        output.put("url", url);
        output.put("linkText", linkText);

        boolean success = false;
        try {
            ActivityInvocationSpec spec = ActivityInvocationSpec.fromNodeConfig(config);
            ActivityFactory factory = bridge.activities();
            FeishuSendPostWithLinkActivity activity = factory.getActivity(FeishuSendPostWithLinkActivity.class, spec);
            success = activity.sendPostWithLink(chatId, title, text, url, linkText);

            output.put("success", success);
            output.put("timestamp", System.currentTimeMillis());

            markNodeSuccess(trace, output);
            log.info("发送带链接富文本消息完成: chatId={}, url={}, success={}", chatId, url, success);
        } catch (Exception e) {
            output.put("success", false);
            output.put("error", e.getMessage());
            markNodeFailed(trace, e.getMessage());
            throw new RuntimeException("飞书发送带链接富文本消息失败: " + e.getMessage(), e);
        }

        NodeExecutionResult result = NodeExecutionResult.completed();
        result.setBranchKey(success ? "success" : "failure");
        result.setOutput(output);
        return result;
    }

    private NodeExecutionTraceEntity startTrace(CompiledNode node, ExecutionContext context) {
        if (traceService == null || context.getExecutionId() == null) {
            return null;
        }
        try {
            NodeExecutionTraceEntity trace = traceService.startNodeExecution(
                context.getExecutionId(),
                node.getId(),
                node.getType().name(),
                "NORMAL",
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

    private void markNodeSuccess(NodeExecutionTraceEntity trace, Map<String, Object> output) {
        if (traceService != null && trace != null) {
            try {
                traceService.markNodeSuccess(trace.getId(), output);
            } catch (Exception e) {
                log.warn("标记节点成功失败: {}", e.getMessage());
            }
        }
    }

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
}
