/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.feishu.sendpost;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipc115.helix.common.constant.NodeRoleConstants;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.integration.connect.ConnectionClientRegistry;
import com.sipc115.helix.integration.connect.lark.FeishuApiHandler;
import com.sipc115.helix.integration.connect.lark.FeishuAuthClient;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import com.sipc115.helix.integration.workflow.trace.WorkflowTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FeishuSendPostNodeExecutor implements WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(FeishuSendPostNodeExecutor.class);
    private static WorkflowTraceService traceService;
    private static ConnectionClientRegistry connectionRegistry;

    @Autowired
    public void setTraceService(WorkflowTraceService traceService) {
        FeishuSendPostNodeExecutor.traceService = traceService;
    }

    @Autowired
    public void setConnectionRegistry(ConnectionClientRegistry connectionRegistry) {
        FeishuSendPostNodeExecutor.connectionRegistry = connectionRegistry;
    }

    @Override
    public boolean supports(String type) {
        return DslNodeType.FEISHU_SEND_POST.name().equals(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        NodeExecutionTraceEntity trace = startTrace(node, context);

        Map<String, Object> config = node.getConfig();
        Long connectionId = getLongValue(config, "connectionId");
        if (connectionId == null) {
            throw new IllegalArgumentException("节点配置错误: connectionId 不能为空，节点ID: " + node.getId());
        }
        String chatId = getStringValue(config, "chatId");
        String title = getStringValue(config, "title");
        List<String> lines = (List<String>) config.get("lines");

        Map<String, Object> output = new HashMap<>();
        output.put("action", "sendPost");
        output.put("connectionId", connectionId);
        output.put("chatId", chatId);
        output.put("title", title);

        boolean success = false;
        try {
            FeishuAuthClient authClient;
            Object cachedConfig = config.get("_connectionConfig");
            if (cachedConfig != null) {
                authClient = connectionRegistry.getOrCreateClient(connectionId, "FEISHU", cachedConfig);
            } else {
                authClient = connectionRegistry.getOrCreateClientByConnection(connectionId);
            }
            String token = authClient.getToken();

            FeishuApiHandler handler = new FeishuApiHandler(new ObjectMapper(), RestClient.builder());
            String messageId = handler.sendPost(token, chatId, title, lines);

            success = messageId != null && !messageId.isEmpty();
            output.put("success", success);
            output.put("messageId", messageId);
            output.put("timestamp", System.currentTimeMillis());

            markNodeSuccess(trace, output);
            log.info("发送富文本消息完成: connectionId={}, chatId={}, title={}, success={}", connectionId, chatId, title, success);
        } catch (Exception e) {
            output.put("success", false);
            output.put("error", e.getMessage());
            markNodeFailed(trace, e.getMessage());
            throw new RuntimeException("飞书发送富文本消息失败: " + e.getMessage(), e);
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

    private Long getLongValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
}