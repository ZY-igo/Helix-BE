/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.executor;

import com.googlecode.aviator.AviatorEvaluator;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.step.Loop.LoopWhileStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskExecutionException;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 循环步骤执行器
 */
@Component
public class LoopWhileStepExecutor implements AiStepExecutor {

    private static final Logger log = LoggerFactory.getLogger(LoopWhileStepExecutor.class);

    private final AiStepExecutorRegistry executorRegistry;

    /**
     * 构造函数
     *
     * @param executorRegistry 执行器注册表
     */
    @Autowired
    public LoopWhileStepExecutor(AiStepExecutorRegistry executorRegistry) {
        this.executorRegistry = executorRegistry;
    }

    @Override
    public StepTrace execute(AiFlowStepConfig stepConfig, AiTaskState state) {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        Object outputSnapshot = null;
        String errorMessage = null;

        try {
            LoopWhileStepConfig loopConfig = (LoopWhileStepConfig) stepConfig;

            int currentRound = 0;
            List<StepTrace> loopSteps = new ArrayList<>();
            int maxRounds = loopConfig.getMaxRounds() != null ? loopConfig.getMaxRounds() : 3;

            log.info("Starting loop with max {} rounds", maxRounds);

            while (currentRound < maxRounds) {
                // 解析并计算条件表达式
                boolean conditionResult = evaluateCondition(loopConfig.getCondition(), state);
                log.debug("Loop round {}, condition result: {}", currentRound, conditionResult);

                if (!conditionResult) {
                    log.info("Loop condition false, breaking after {} rounds", currentRound);
                    break;
                }

                // 执行循环体
                List<AiFlowStepConfig> bodySteps = loopConfig.getBody();
                if (bodySteps != null) {
                    log.debug("Executing loop body with {} steps", bodySteps.size());
                    for (AiFlowStepConfig bodyStep : bodySteps) {
                        AiStepExecutor executor = executorRegistry.getExecutor(bodyStep.getType());
                        StepTrace bodyStepTrace = executor.execute(bodyStep, state);
                        loopSteps.add(bodyStepTrace);

                        if ("FAILED".equals(bodyStepTrace.getStatus())) {
                            log.warn("Step failed in loop body: {}", bodyStep.getId());
                        }
                    }
                }

                currentRound++;
            }

            log.info("Loop completed after {} rounds", currentRound);
            outputSnapshot = loopSteps;

        } catch (AiTaskExecutionException e) {
            log.error("AI task execution failed at loop step: {}", stepConfig.getId(), e);
            status = "FAILED";
            errorMessage = e.getOriginalMessage();

        } catch (Exception e) {
            log.error("Unexpected error in loop step: {}", stepConfig.getId(), e);
            status = "FAILED";
            errorMessage = "系统异常：" + e.getMessage();
        }

        long durationMs = System.currentTimeMillis() - startTime;

        StepTrace trace = new StepTrace(
            stepConfig.getId(),
            stepConfig.getType(),
            status,
            null,
            outputSnapshot,
            durationMs
        );

        if (errorMessage != null) {
            trace.setErrorMessage(errorMessage);
        }

        return trace;
    }

    /**
     * 使用 Aviator 引擎评估条件表达式
     */
    private boolean evaluateCondition(String condition, AiTaskState state) {
        if (condition == null || condition.trim().isEmpty()) {
            log.warn("Empty condition, defaulting to false");
            return false;
        }

        try {
            log.debug("Evaluating loop condition: {}", condition);

            // 准备表达式环境
            Map<String, Object> env = prepareExpressionEnv(state);

            // 使用 Aviator 表达式引擎执行
            Object result = AviatorEvaluator.execute(condition, env);

            if (result instanceof Boolean) {
                boolean boolResult = (Boolean) result;
                log.debug("Loop condition result: {}", boolResult);
                return boolResult;
            } else if (result instanceof Number) {
                boolean truthy = ((Number) result).doubleValue() != 0;
                log.debug("Numeric condition converted to: {}", truthy);
                return truthy;
            } else {
                boolean truthy = result != null;
                log.debug("Other type converted to: {}", truthy);
                return truthy;
            }

        } catch (Exception e) {
            log.error("Failed to evaluate loop condition '{}'", condition, e);

            throw AiTaskExecutionException.validationFailed(
                "LOOP-" + System.currentTimeMillis(),
                "循环条件表达式解析失败：" + e.getMessage()
            );
        }
    }

    /**
     * 准备表达式执行环境
     */
    private Map<String, Object> prepareExpressionEnv(AiTaskState state) {
        Map<String, Object> env = new java.util.HashMap<>();

        // 添加所有变量到表达式环境
        if (state.getVars() != null) {
            env.putAll(state.getVars());
        }

        // 添加输入到表达式环境
        if (state.getInput() != null) {
            env.putAll(state.getInput());
        }

        // 添加元数据到表达式环境（可选）
        if (state.getMeta() != null) {
            env.putAll(state.getMeta());
        }

        log.debug("Prepared expression environment with {} variables", env.size());
        return env;
    }

    @Override
    public String getSupportedType() {
        return AiFlowStepConfig.StepType.LOOP_WHILE.name();
    }
}
