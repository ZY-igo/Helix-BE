/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.executor;

import com.sipc115.helix.integration.workflow.node.agent.config.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.config.AiTaskConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.RoundTrace;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 任务节点执行器类
 * <p>
 * 执行完整的 AI 任务流程，整合所有步骤执行器。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class AiTaskNodeExecutor {
    private final AiStepExecutorRegistry executorRegistry;

    /**
     * 构造函数
     * <p>
     * 初始化执行器注册表。
     */
    public AiTaskNodeExecutor() {
        this.executorRegistry = new AiStepExecutorRegistry();
    }

    /**
     * 执行 AI 任务
     * <p>
     * 执行完整的 AI 任务流程，返回执行轨迹。
     * 
     * @param config AI 任务配置
     * @param input 输入参数
     * @return 执行轨迹
     */
    public AiTaskTrace execute(AiTaskConfig config, Map<String, Object> input) {
        long startTime = System.currentTimeMillis();
        String finalStatus = "SUCCESS";
        String stopReason = "任务完成";
        List<RoundTrace> rounds = new ArrayList<>();
        
        try {
            // 初始化任务状态
            AiTaskState state = initializeState(config, input);
            
            // 执行流程步骤
            List<StepTrace> steps = executeSteps(config.getFlow(), state);
            
            // 添加轮次轨迹
            rounds.add(new RoundTrace(1, steps));
        } catch (Exception e) {
            finalStatus = "FAILED";
            stopReason = "执行异常: " + e.getMessage();
            // TODO: 处理异常
        }
        
        // 创建并返回执行轨迹
        AiTaskTrace trace = new AiTaskTrace();
        trace.setRounds(rounds);
        trace.setFinalStatus(finalStatus);
        trace.setStopReason(stopReason);
        return trace;
    }

    /**
     * 初始化任务状态
     * <p>
     * 根据配置初始化任务状态。
     * 
     * @param config AI 任务配置
     * @param input 输入参数
     * @return 初始化后的任务状态
     */
    private AiTaskState initializeState(AiTaskConfig config, Map<String, Object> input) {
        // 解析输入
        Map<String, Object> resolvedInput = resolveInputs(config.getInputs(), input);
        
        // 初始化变量
        Map<String, Object> vars = new HashMap<>(config.getVars());
        
        // 初始化元数据
        Map<String, Object> meta = new HashMap<>();
        meta.put("currentRound", 1);
        meta.put("modelCalls", 0);
        
        // 由于 AiTaskState 使用了 Builder 模式，直接使用它
        return new AiTaskState.Builder()
                .input(resolvedInput)
                .vars(vars)
                .meta(meta)
                .build();
    }

    /**
     * 解析输入
     * <p>
     * 解析输入表达式，替换变量。
     * 
     * @param inputConfigs 输入配置
     * @param workflowInput 工作流输入
     * @return 解析后的输入
     */
    private Map<String, Object> resolveInputs(Map<String, String> inputConfigs, Map<String, Object> workflowInput) {
        Map<String, Object> resolvedInput = new HashMap<>();
        
        // TODO: 实现输入解析逻辑
        // 1. 解析表达式
        // 2. 替换变量
        // 3. 返回解析后的输入
        
        return resolvedInput;
    }

    /**
     * 执行流程步骤
     * <p>
     * 执行流程中的所有步骤。
     * 
     * @param flow 流程步骤配置
     * @param state 任务状态
     * @return 步骤执行轨迹
     */
    private List<StepTrace> executeSteps(List<AiFlowStepConfig> flow, AiTaskState state) {
        List<StepTrace> steps = new ArrayList<>();
        
        for (AiFlowStepConfig stepConfig : flow) {
            AiStepExecutor executor = executorRegistry.getExecutor(stepConfig.getType());
            StepTrace stepTrace = executor.execute(stepConfig, state);
            steps.add(stepTrace);
            
            // 检查是否为返回步骤
            if (stepConfig.getType().equals(AiFlowStepConfig.StepType.RETURN.name())) {
                break;
            }
        }
        
        return steps;
    }
}
