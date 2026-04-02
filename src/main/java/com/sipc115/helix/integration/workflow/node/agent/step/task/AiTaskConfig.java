/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.task;

import com.sipc115.helix.integration.workflow.node.agent.step.LlmConfig;
import com.sipc115.helix.integration.workflow.node.agent.step.RuntimePolicyConfig;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * AI 任务配置
 * <p>
 * 定义 AI 任务的完整配置，包括输入参数、中间变量、流程步骤、运行时策略和 LLM 配置。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class AiTaskConfig {

    /**
     * 输入参数映射
     * <p>
     * Key: 变量名，Value: Aviator 表达式
     * <p>
     * 示例：
     * <pre>
     * {
     *   "query": "${workflow.input.query}",
     *   "userId": "${workflow.input.userId}"
     * }
     * </pre>
     */
    private Map<String, String> inputs;

    /**
     * 中间变量声明
     * <p>
     * Key: 变量名，Value: 初始值
     * <p>
     * 示例：
     * <pre>
     * {
     *   "maxAttempts": 3,
     *   "timeout": 60000
     * }
     * </pre>
     */
    private Map<String, Object> vars;

    /**
     * 微流程步骤列表
     * <p>
     * 包含一系列 AI 流程步骤，按照执行顺序排列
     */
    private List<AiFlowStepConfig> flow;

    /**
     * 运行时策略
     * <p>
     * 控制 AI 任务的运行时行为，如最大轮次、超时时间等
     */
    private RuntimePolicyConfig runtimePolicy;
    
    /**
     * LLM 配置
     * <p>
     * 配置 AI 模型的参数，支持不同的 AI 提供商
     */
    private LlmConfig llmConfig;
}
