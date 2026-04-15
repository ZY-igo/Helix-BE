/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.chat;

import com.sipc115.helix.common.constant.WorkflowConstants;
import com.sipc115.helix.domain.workflow.CompiledNode;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.domain.workflow.ExecutionStatus;
import com.sipc115.helix.integration.expression.ExpressionEngine;
import com.sipc115.helix.integration.workflow.engine.ActivityInvocationSpec;
import com.sipc115.helix.integration.workflow.node.agent.chat.activity.AiChatActivity;
import com.sipc115.helix.integration.workflow.node.agent.chat.activity.AiChatActivityResult;
import com.sipc115.helix.integration.workflow.runtime.ExecutionContext;
import com.sipc115.helix.integration.workflow.runtime.NodeExecutionResult;
import com.sipc115.helix.integration.workflow.runtime.WorkflowNodeExecutor;
import com.sipc115.helix.integration.workflow.runtime.WorkflowRuntimeBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 对话节点的运行期执行器。
 * <p>
 * 它位于工作流运行时和具体 AI Activity 之间，负责把编译后的节点配置转换为一次真正的节点执行。
 * 和编译器相比，执行器关心的是“当前上下文下怎么运行”，而不是“配置是否合法”。
 * </p>
 * <p>
 * 主要职责包括：
 * </p>
 * <ul>
 *     <li>从编译后的节点配置中读取模型参数、提示词和连接信息。</li>
 *     <li>对提示词中的表达式做运行时求值。</li>
 *     <li>通过 {@link WorkflowRuntimeBridge} 获取 Activity 代理并发起调用。</li>
 *     <li>把 Activity 返回值转换为统一的 {@link NodeExecutionResult}。</li>
 * </ul>
 */
@Component
public class AiChatNodeExecutor implements WorkflowNodeExecutor {

    /**
     * 当前类日志对象。
     */
    private static final Logger log = LoggerFactory.getLogger(AiChatNodeExecutor.class);

    /**
     * 表达式引擎。
     * <p>
     * 采用静态持有方式，便于在执行节点时对 systemPrompt / userPrompt 中的动态表达式做求值。
     * </p>
     */
    private static ExpressionEngine expressionEngine;

    /**
     * 注入表达式引擎。
     */
    @Autowired
    public void setExpressionEngine(ExpressionEngine expressionEngine) {
        AiChatNodeExecutor.expressionEngine = expressionEngine;
    }

    /**
     * 判断当前执行器是否支持某个节点类型。
     */
    @Override
    public boolean supports(String type) {
        return DslNodeType.AI_TASK.name().equals(type);
    }

    /**
     * 执行一个 AI 对话节点。
     * <p>
     * 运行流程如下：
     * </p>
     * <ul>
     *     <li>读取编译后的节点配置，并提取连接、提示词和生成参数。</li>
     *     <li>对支持表达式的字段做运行时求值。</li>
     *     <li>根据节点配置生成 Activity 调用规格，并获取对应 Activity 代理。</li>
     *     <li>调用 {@link AiChatActivity} 执行 AI 节点。</li>
     *     <li>把 Activity 结果整理为统一的节点输出结构。</li>
     * </ul>
     *
     * @param node 编译后的节点
     * @param context 当前工作流执行上下文
     * @param bridge 工作流运行时桥接对象，用于获取 activity 等运行资源
     * @return 节点执行结果
     */
    @Override
    public NodeExecutionResult execute(CompiledNode node, ExecutionContext context, WorkflowRuntimeBridge bridge) {
        try {
            // 复制一份配置，避免在当前执行中意外修改编译产物。
            Map<String, Object> config = new LinkedHashMap<>(node.getConfig());

            Long connectionId = getLongValue(config.get("connectionId"));
            if (connectionId == null) {
                throw new IllegalArgumentException("AI_TASK node requires connectionId: " + node.getId());
            }

            // systemPrompt 和 userPrompt 都支持表达式求值，以便引用当前工作流变量。
            String systemPrompt = evaluateExpression(getStringValue(config.get("systemPrompt"), "You are a capable workflow agent."), context);
            String userPromptRaw = getStringValue(config.get("userPrompt"), "");
            if (userPromptRaw.isEmpty()) {
                throw new IllegalArgumentException("AI_TASK node userPrompt cannot be empty: " + node.getId());
            }
            String userPrompt = evaluateExpression(userPromptRaw, context);

            // 读取生成参数和输出变量配置。
            Double temperature = getDoubleValue(config.get("temperature"), 0.7);
            Integer maxTokens = getIntValue(config.get("maxTokens"), 4096);
            String thinking = getStringValue(config.get("thinking"), "disabled");
            String outputVar = getStringValue(config.get("outputVar"), "aiResponse");

            // 结合节点配置生成 Activity 调用规格，交给运行时桥接层构造代理。
            ActivityInvocationSpec activitySpec = ActivityInvocationSpec.fromNodeConfig(config);
            AiChatActivity activity = bridge.activities().getActivity(AiChatActivity.class, activitySpec);

            // 把当前执行上下文和标准化配置整体交给 Activity，由 Activity 决定 chat 或 agent 的具体执行。
            AiChatActivityResult activityResult = activity.chat(
                    context.getExecutionId(),
                    0,
                    node.getId(),
                    connectionId,
                    config.get(WorkflowConstants.CONNECTION_CONFIG_KEY),
                    new LinkedHashMap<>(context.getVariables()),
                    config,
                    systemPrompt,
                    userPrompt,
                    temperature,
                    maxTokens,
                    thinking
            );

            // 整理统一的节点输出，既保留 AI 原始响应，也保留 agent 模式下的扩展信息。
            Map<String, Object> output = new HashMap<>();
            output.put("action", "agent".equals(activityResult.getMode()) ? "aiAgent" : "aiChat");
            output.put("connectionId", connectionId);
            output.put("model", getStringValue(config.get("model"), "unknown"));
            output.put("mode", activityResult.getMode());
            output.put("response", activityResult.getResponse());
            output.put(outputVar, activityResult.getResponse());
            output.put("iterations", activityResult.getIterations());
            output.put("toolCalls", activityResult.getToolCalls());
            output.put("memory", activityResult.getMemory());
            output.put("finalOutput", activityResult.getFinalOutput());
            output.put("status", "success");
            output.put("timestamp", System.currentTimeMillis());
            if (activityResult.getBranchKey() != null) {
                output.put("branchKey", activityResult.getBranchKey());
            }
            if (activityResult.getNextNodeId() != null) {
                output.put("nextNodeId", activityResult.getNextNodeId());
            }

            // 将 Activity 返回值映射为工作流引擎可识别的节点执行结果。
            NodeExecutionResult result = new NodeExecutionResult();
            result.setStatus(ExecutionStatus.COMPLETED);
            result.setOutput(output);
            result.setBranchKey(activityResult.getBranchKey());
            result.setNextNodeId(activityResult.getNextNodeId());
            return result;

        } catch (Exception e) {
            log.error("AI_TASK node execution failed: {}", node.getId(), e);

            NodeExecutionResult result = new NodeExecutionResult();
            result.setStatus(ExecutionStatus.FAILED);
            result.setOutput(Map.of("error", e.getMessage()));
            return result;
        }
    }

    /**
     * 对支持表达式的字符串做运行时求值。
     * <p>
     * 仅当字符串中包含 `${` 且表达式引擎可用时才尝试执行表达式。
     * 为降低重复求值开销，会优先使用 {@link ExecutionContext} 中的缓存结果。
     * </p>
     * <p>
     * 如果表达式执行失败，不中断节点执行，而是退回原始文本。
     * </p>
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
                log.warn("Expression evaluation failed, use raw text. error={}", e.getMessage());
            }
        }

        return expression;
    }

    /**
     * 安全读取 Long 值。
     */
    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    /**
     * 安全读取字符串；空值时返回默认值。
     */
    private String getStringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : value.toString();
    }

    /**
     * 安全读取 Double 值。
     */
    private Double getDoubleValue(Object value, Double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    /**
     * 安全读取 Integer 值。
     */
    private Integer getIntValue(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
