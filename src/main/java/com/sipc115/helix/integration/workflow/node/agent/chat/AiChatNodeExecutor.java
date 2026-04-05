package com.sipc115.helix.integration.workflow.node.agent.chat;

import com.sipc115.helix.common.constant.NodeRoleConstants;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.ExecutionStatus;
import com.sipc115.helix.domain.workflow.NodeExecutionTraceEntity;
import com.sipc115.helix.integration.connect.ConnectionClientRegistry;
import com.sipc115.helix.integration.connect.llm.LlmAuthClient;
import com.sipc115.helix.integration.expression.ExpressionEngine;
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

@Component
public class AiChatNodeExecutor implements WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiChatNodeExecutor.class);

    private static WorkflowTraceService traceService;
    private static ConnectionClientRegistry connectionRegistry;
    private static ExpressionEngine expressionEngine;

    @Autowired
    public void setTraceService(WorkflowTraceService traceService) {
        AiChatNodeExecutor.traceService = traceService;
    }

    @Autowired
    public void setConnectionRegistry(ConnectionClientRegistry connectionRegistry) {
        AiChatNodeExecutor.connectionRegistry = connectionRegistry;
    }

    @Autowired
    public void setExpressionEngine(ExpressionEngine expressionEngine) {
        AiChatNodeExecutor.expressionEngine = expressionEngine;
    }

    @Override
    public boolean supports(String type) {
        return DslNodeType.AI_TASK.name().equals(type);
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
                        NodeRoleConstants.NORMAL,
                        context.getExecutionOrder(),
                        context.getVariables()
                );
                context.setCurrentNodeTraceId(trace.getId());
            } catch (Exception e) {
                log.warn("启动节点追踪失败: {}", e.getMessage());
            }
        }

        try {
            Map<String, Object> config = node.getConfig();

            Long connectionId = getLongValue(config.get("connectionId"));
            if (connectionId == null) {
                throw new IllegalArgumentException("AI 聊天节点必须指定 connectionId: " + node.getId());
            }

            String systemPrompt = getStringValue(config.get("systemPrompt"), "You are a helpful AI assistant.");
            String userPromptRaw = getStringValue(config.get("userPrompt"), "");
            if (userPromptRaw.isEmpty()) {
                throw new IllegalArgumentException("AI 聊天节点的 userPrompt 不能为空: " + node.getId());
            }

            Double temperature = getDoubleValue(config.get("temperature"), 0.7);
            Integer maxTokens = getIntValue(config.get("maxTokens"), 4096);
            String thinking = getStringValue(config.get("thinking"), "disabled");
            String outputVar = getStringValue(config.get("outputVar"), "aiResponse");

            String userPrompt = evaluateExpression(userPromptRaw, context);

            log.info("执行 AI 聊天节点: {}, connectionId: {}", node.getId(), connectionId);

            LlmAuthClient llmClient = connectionRegistry.getOrCreateClientByConnection(connectionId);
            String response = llmClient.chat(systemPrompt, userPrompt, temperature, maxTokens, thinking);

            Map<String, Object> output = new HashMap<>();
            output.put("action", "aiChat");
            output.put("connectionId", connectionId);
            output.put("model", llmClient.getModel());
            output.put(outputVar, response);
            output.put("status", "success");
            output.put("timestamp", System.currentTimeMillis());

            if (traceService != null && trace != null) {
                try {
                    traceService.markNodeSuccess(trace.getId(), output);
                } catch (Exception e) {
                    log.warn("标记节点成功失败: {}", e.getMessage());
                }
            }

            NodeExecutionResult result = new NodeExecutionResult();
            result.setStatus(ExecutionStatus.COMPLETED);
            result.setOutput(output);
            return result;

        } catch (Exception e) {
            log.error("AI 聊天节点执行失败: {}", node.getId(), e);

            if (traceService != null && trace != null) {
                try {
                    traceService.markNodeFailed(trace.getId(), e.getMessage(), e.toString());
                } catch (Exception ex) {
                    log.warn("标记节点失败失败: {}", ex.getMessage());
                }
            }

            NodeExecutionResult result = new NodeExecutionResult();
            result.setStatus(ExecutionStatus.FAILED);
            result.setOutput(Map.of("error", e.getMessage()));
            return result;
        }
    }

    private String evaluateExpression(String expression, ExecutionContext context) {
        if (expression == null || expression.isEmpty()) {
            return "";
        }

        if (expressionEngine != null && expression.contains("${")) {
            Object cached = context.getCachedExpression(expression);
            if (cached != null) {
                return cached.toString();
            }

            try {
                Object result = expressionEngine.execute(expression, context.getVariables());
                String evaluated = result != null ? result.toString() : "";
                context.cacheExpressionResult(expression, evaluated);
                return evaluated;
            } catch (Exception e) {
                log.warn("表达式求值失败，使用原始值: {}", e.getMessage());
                return expression;
            }
        }

        return expression;
    }

    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String getStringValue(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    private Double getDoubleValue(Object value, Double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private Integer getIntValue(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
}