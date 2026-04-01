/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.agent.executor;

import com.sipc115.helix.context.node.agent.config.AiFlowStepConfig;
import com.sipc115.helix.context.node.agent.config.LoopWhileStepConfig;
import com.sipc115.helix.context.node.agent.runtime.AiTaskState;
import com.sipc115.helix.context.node.agent.runtime.AiTaskTrace.StepTrace;

import java.util.ArrayList;
import java.util.List;

/**
 * 循环步骤执行器类
 * <p>
 * 执行 AI 循环步骤，根据条件重复执行循环体。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public class LoopWhileStepExecutor implements AiStepExecutor {

    private final AiStepExecutorRegistry executorRegistry;

    public LoopWhileStepExecutor(AiStepExecutorRegistry executorRegistry) {
        this.executorRegistry = executorRegistry;
    }

    @Override
    public StepTrace execute(AiFlowStepConfig stepConfig, AiTaskState state) {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        Object outputSnapshot = null;
        
        try {
            LoopWhileStepConfig loopConfig = (LoopWhileStepConfig) stepConfig;
            
            // TODO: 实现循环步骤逻辑
            // 1. 解析条件表达式
            // 2. 执行循环体
            // 3. 检查循环条件
            // 4. 控制循环次数
            
            int currentRound = 0;
            List<StepTrace> loopSteps = new ArrayList<>();
            
            while (currentRound < loopConfig.getMaxRounds()) {
                boolean conditionResult = true; // 假设条件为真
                if (!conditionResult) {
                    break;
                }
                
                // 执行循环体
                List<AiFlowStepConfig> bodySteps = loopConfig.getBody();
                if (bodySteps != null) {
                    for (AiFlowStepConfig bodyStep : bodySteps) {
                        AiStepExecutor executor = executorRegistry.getExecutor(bodyStep.getType());
                        StepTrace bodyStepTrace = executor.execute(bodyStep, state);
                        loopSteps.add(bodyStepTrace);
                    }
                }
                
                currentRound++;
            }
            
            outputSnapshot = loopSteps;
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
        return AiFlowStepConfig.StepType.LOOP_WHILE.name();
    }
}
