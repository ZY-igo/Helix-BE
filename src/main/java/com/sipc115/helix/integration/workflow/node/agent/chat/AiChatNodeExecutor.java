/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.chat;

import com.sipc115.helix.common.constant.BranchKeyConstants;
import com.sipc115.helix.common.constant.WorkflowConstants;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.ExecutionStatus;
import com.sipc115.helix.integration.expression.ExpressionEngine;
import com.sipc115.helix.integration.workflow.engine.ActivityInvocationSpec;
import com.sipc115.helix.integration.workflow.node.agent.chat.activity.AiChatActivity;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 聊天节点执行器
 * <p>
 * 负责执行 AI 对话任务节点，支持调用 LLM API 进行对话。
 *
 * <h3>DSL 配置示例：</h3>
 * <pre>
 * {
 *   "type": "AI_TASK",
 *   "config": {
 *     "connectionId": 1,
 *     "systemPrompt": "你是一个专业的分析师",
 *     "userPrompt": "请分析这份报告：${reportNode.content}",
 *     "temperature": 0.7,
 *     "maxTokens": 4096,
 *     "outputVar": "analysisResult"
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
 * Activity 可以实现幂等性检查，避免 Temporal 重试导致重复调用 LLM API。
 * 由于 LLM API 通常费用较高，幂等性保护尤为重要。
 *
 * <h3>执行流程：</h3>
 * <pre>
 * Temporal Workflow Thread
 *   └─ AiChatNodeExecutor.execute()
 *       └─ bridge.activities().getActivity(AiChatActivity.class)
 *           └─ AiChatActivity.chat()
 *               └─ 幂等性检查
 *               └─ LlmAuthClient.chat()   ← 真正的 HTTP 调用
 * </pre>
 *
 * <h3>与旧架构对比：</h3>
 * <pre>
 * 旧架构（直接调用 - 违反确定性原则）：
 *   Executor → LlmAuthClient → LLM API  ❌ HTTP 调用在 Workflow 线程
 *
 * 新架构（Activity 委托 - 正确做法）：
 *   Executor → AiChatActivity → LlmAuthClient  ✅ HTTP 调用在 Activity Worker
 * </pre>
 *
 * <h3>表达式缓存机制：</h3>
 * <p>
 * 为了避免重复求值，相同的表达式只会求值一次，结果会被缓存。
 * 例如多个节点都引用 ${A.result}，只会求值一次。
 *
 * @author Helix Team
 * @since 2.0.0
 * @see WorkflowNodeExecutor
 * @see AiChatActivity
 * @see ExpressionEngine
 */
@Component
public class AiChatNodeExecutor implements WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiChatNodeExecutor.class);

    private static ExpressionEngine expressionEngine;

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

            ActivityInvocationSpec activitySpec = ActivityInvocationSpec.fromNodeConfig(config);
            AiChatActivity activity = bridge.activities().getActivity(AiChatActivity.class, activitySpec);

            Long executionId = context.getExecutionId();
            String nodeId = node.getId();

            String response;
            Object cachedConfig = config.get(WorkflowConstants.CONNECTION_CONFIG_KEY);
            if (cachedConfig != null) {
                response = activity.chatWithConfig(
                    executionId, 0, nodeId,
                    cachedConfig, systemPrompt, userPrompt, temperature, maxTokens, thinking);
            } else {
                response = activity.chat(
                    executionId, 0, nodeId,
                    connectionId, systemPrompt, userPrompt, temperature, maxTokens, thinking);
            }

            String model = getStringValue(config.get("model"), "unknown");

            Map<String, Object> output = new HashMap<>();
            output.put("action", "aiChat");
            output.put("connectionId", connectionId);
            output.put("model", model);
            output.put("response", response);
            output.put(outputVar, response);
            output.put("status", "success");
            output.put("timestamp", System.currentTimeMillis());

            NodeExecutionResult result = new NodeExecutionResult();
            result.setStatus(ExecutionStatus.COMPLETED);
            result.setOutput(output);
            return result;

        } catch (Exception e) {
            log.error("AI 聊天节点执行失败: {}", node.getId(), e);

            NodeExecutionResult result = new NodeExecutionResult();
            result.setStatus(ExecutionStatus.FAILED);
            result.setOutput(Map.of("error", e.getMessage()));
            return result;
        }
    }

    /**
     * 表达式求值
     * <p>
     * 对包含 ${} 表达式的字符串进行求值。
     * 支持从 context.variables 中读取变量值。
     *
     * <h3>表达式缓存机制：</h3>
     * <pre>
     * 1. 先检查缓存是否有结果
     * 2. 如果没有，调用 ExpressionEngine 求值
     * 3. 将结果缓存到 context.expressionCache
     * 4. 下次相同表达式直接返回缓存结果
     * </pre>
     *
     * <h3>示例：</h3>
     * <pre>
     * 输入: "Hello, ${user.name}!"
     * 变量: {user: {name: "Alice"}}
     * 输出: "Hello, Alice!"
     * </pre>
     *
     * @param expression 包含表达式的字符串
     * @param context 执行上下文
     * @return 求值后的字符串
     */
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

    /**
     * 安全获取 Long 类型值
     */
    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    /**
     * 安全获取 String 类型值
     */
    private String getStringValue(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    /**
     * 安全获取 Double 类型值
     */
    private Double getDoubleValue(Object value, Double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    /**
     * 安全获取 Integer 类型值
     */
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
