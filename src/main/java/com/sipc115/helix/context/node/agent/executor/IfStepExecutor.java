/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.agent.executor;

import com.sipc115.helix.context.node.agent.config.AiFlowStepConfig;
import com.sipc115.helix.context.node.agent.config.IfStepConfig;
import com.sipc115.helix.context.node.agent.runtime.AiTaskState;
import com.sipc115.helix.context.node.agent.runtime.AiTaskTrace.StepTrace;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件分支步骤执行器类
 * <p>
 * 执行 AI 条件分支步骤，根据条件执行不同的子流程。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class IfStepExecutor implements AiStepExecutor {

    private final AiStepExecutorRegistry executorRegistry;

    public IfStepExecutor(AiStepExecutorRegistry executorRegistry) {
        this.executorRegistry = executorRegistry;
    }

    @Override
    public StepTrace execute(AiFlowStepConfig stepConfig, AiTaskState state) {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        Object outputSnapshot = null;
        
        try {
            IfStepConfig ifConfig = (IfStepConfig) stepConfig;
            
            // TODO: 实现条件分支步骤逻辑
            // 1. 解析条件表达式
            // 2. 执行条件判断
            // 3. 执行相应的子流程
            
            boolean conditionResult = true; // 假设条件为真
            List<StepTrace> childSteps = new ArrayList<>();
            
            if (conditionResult) {
                // 执行 then 分支
                List<AiFlowStepConfig> thenSteps = ifConfig.getThenSteps();
                if (thenSteps != null) {
                    for (AiFlowStepConfig thenStep : thenSteps) {
                        AiStepExecutor executor = executorRegistry.getExecutor(thenStep.getType());
                        StepTrace childStepTrace = executor.execute(thenStep, state);
                        childSteps.add(childStepTrace);
                    }
                }
            } else {
                // 执行 else 分支
                List<AiFlowStepConfig> elseSteps = ifConfig.getElseSteps();
                if (elseSteps != null) {
                    for (AiFlowStepConfig elseStep : elseSteps) {
                        AiStepExecutor executor = executorRegistry.getExecutor(elseStep.getType());
                        StepTrace childStepTrace = executor.execute(elseStep, state);
                        childSteps.add(childStepTrace);
                    }
                }
            }
            
            outputSnapshot = childSteps;
        } catch (Exception e) {
            status = "FAILED";
            // TODO: 处理异常
        }
        
        long durationMs = System.currentTimeMillis() - startTime;
        return new StepTrace(
                stepConfig.getId(),
                stepConfig.getType(),
                status,
                null, // 输入快照
                outputSnapshot,
                durationMs
        );
    }

    @Override
    public String getSupportedType() {
        return AiFlowStepConfig.StepType.IF.name();
    }
}
