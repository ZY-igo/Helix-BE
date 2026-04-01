/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.executor;

import com.sipc115.helix.integration.workflow.node.agent.config.AiFlowStepConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 步骤执行器注册表类
 * <p>
 * 管理和注册所有的 AI 步骤执行器，根据步骤类型分发执行器。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class AiStepExecutorRegistry {
    private final Map<String, AiStepExecutor> executors = new HashMap<>();

    /**
     * 构造函数
     * <p>
     * 初始化并注册所有步骤执行器。
     */
    public AiStepExecutorRegistry() {
        // 注册执行器
        registerExecutors();
    }

    /**
     * 注册所有步骤执行器
     */
    private void registerExecutors() {
        // 注册生成步骤执行器
        executors.put(AiFlowStepConfig.StepType.GENERATE.name(), new GenerateStepExecutor());
        
        // 注册验证步骤执行器
        executors.put(AiFlowStepConfig.StepType.VALIDATE.name(), new ValidateStepExecutor());
        
        // 注册修复步骤执行器
        executors.put(AiFlowStepConfig.StepType.REPAIR.name(), new RepairStepExecutor());
        
        // 注册返回步骤执行器
        executors.put(AiFlowStepConfig.StepType.RETURN.name(), new ReturnStepExecutor());
        
        // 注册条件分支步骤执行器（需要自身引用）
        IfStepExecutor ifExecutor = new IfStepExecutor(this);
        executors.put(AiFlowStepConfig.StepType.IF.name(), ifExecutor);
        
        // 注册循环步骤执行器（需要自身引用）
        LoopWhileStepExecutor loopExecutor = new LoopWhileStepExecutor(this);
        executors.put(AiFlowStepConfig.StepType.LOOP_WHILE.name(), loopExecutor);
    }

    /**
     * 获取执行器
     * <p>
     * 根据步骤类型获取对应的执行器。
     * 
     * @param stepType 步骤类型
     * @return 对应的执行器
     * @throws IllegalArgumentException 如果步骤类型不支持
     */
    public AiStepExecutor getExecutor(String stepType) {
        AiStepExecutor executor = executors.get(stepType);
        if (executor == null) {
            throw new IllegalArgumentException("Unsupported step type: " + stepType);
        }
        return executor;
    }

    /**
     * 注册执行器
     * <p>
     * 注册自定义执行器。
     * 
     * @param stepType 步骤类型
     * @param executor 执行器
     */
    public void registerExecutor(String stepType, AiStepExecutor executor) {
        executors.put(stepType, executor);
    }
}
