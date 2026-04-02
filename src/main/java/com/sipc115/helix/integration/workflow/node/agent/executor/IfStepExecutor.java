/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.executor;

import com.googlecode.aviator.AviatorEvaluator;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.step.If.IfStepConfig;
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
 * 条件分支步骤执行器
 */
@Component
public class IfStepExecutor implements AiStepExecutor {

    private static final Logger log = LoggerFactory.getLogger(IfStepExecutor.class);

    private final AiStepExecutorRegistry executorRegistry;

    /**
     * 构造函数
     *
     * @param executorRegistry 执行器注册表
     */
    @Autowired
    public IfStepExecutor(AiStepExecutorRegistry executorRegistry) {
        this.executorRegistry = executorRegistry;
    }

    @Override
    public StepTrace execute(AiFlowStepConfig stepConfig, AiTaskState state) {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        Object outputSnapshot = null;
        String errorMessage = null;

        try {
            IfStepConfig ifConfig = (IfStepConfig) stepConfig;

            // ⭐ 1. 解析条件表达式
            boolean conditionResult = evaluateCondition(ifConfig.getCondition(), state);
            List<StepTrace> childSteps = new ArrayList<>();

            log.info("IF step condition evaluated to: {}", conditionResult);

            // ⭐ 2. 根据条件执行相应分支
            if (conditionResult) {
                // 执行 then 分支
                List<AiFlowStepConfig> thenSteps = ifConfig.getThenSteps();
                if (thenSteps != null && !thenSteps.isEmpty()) {
                    log.info("Executing THEN branch with {} steps", thenSteps.size());
                    for (AiFlowStepConfig thenStep : thenSteps) {
                        AiStepExecutor executor = executorRegistry.getExecutor(thenStep.getType());
                        StepTrace childStepTrace = executor.execute(thenStep, state);
                        childSteps.add(childStepTrace);

                        if ("FAILED".equals(childStepTrace.getStatus())) {
                            log.warn("Step failed in THEN branch: {}", thenStep.getId());
                        }
                    }
                }
            } else {
                // 执行 else 分支
                List<AiFlowStepConfig> elseSteps = ifConfig.getElseSteps();
                if (elseSteps != null && !elseSteps.isEmpty()) {
                    log.info("Executing ELSE branch with {} steps", elseSteps.size());
                    for (AiFlowStepConfig elseStep : elseSteps) {
                        AiStepExecutor executor = executorRegistry.getExecutor(elseStep.getType());
                        StepTrace childStepTrace = executor.execute(elseStep, state);
                        childSteps.add(childStepTrace);

                        if ("FAILED".equals(childStepTrace.getStatus())) {
                            log.warn("Step failed in ELSE branch: {}", elseStep.getId());
                        }
                    }
                }
            }

            outputSnapshot = childSteps;

        } catch (AiTaskExecutionException e) {
            log.error("AI task execution failed at IF step: {}", stepConfig.getId(), e);
            status = "FAILED";
            errorMessage = e.getOriginalMessage();

        } catch (Exception e) {
            log.error("Unexpected error in IF step: {}", stepConfig.getId(), e);
            status = "FAILED";
            errorMessage = "系统异常：" + e.getMessage();
        }

        long durationMs = System.currentTimeMillis() - startTime;

        // ⭐ 创建 StepTrace 并设置错误信息
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
     * ⭐ 使用 Aviator 引擎评估条件表达式
     */
    private boolean evaluateCondition(String condition, AiTaskState state) {
        if (condition == null || condition.trim().isEmpty()) {
            log.warn("Empty condition, defaulting to false");
            return false;
        }

        try {
            log.debug("Evaluating condition: {}", condition);

            // ⭐ 准备表达式环境
            Map<String, Object> env = prepareExpressionEnv(state);

            // ⭐ 使用 Aviator 表达式引擎执行
            Object result = AviatorEvaluator.execute(condition, env);

            if (result instanceof Boolean) {
                boolean boolResult = (Boolean) result;
                log.debug("Condition result: {}", boolResult);
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
            log.error("Failed to evaluate condition '{}'", condition, e);

            // ⭐ 抛出正确的异常（5 个参数）
            throw AiTaskExecutionException.validationFailed(
                "IF-" + System.currentTimeMillis(),
                "条件表达式解析失败：" + e.getMessage()
            );
        }
    }

    /**
     * ⭐ 准备表达式执行环境
     */
    private Map<String, Object> prepareExpressionEnv(AiTaskState state) {
        Map<String, Object> env = new java.util.HashMap<>();

        // ⭐ 添加所有变量到表达式环境
        if (state.getVars() != null) {
            env.putAll(state.getVars());
        }

        // ⭐ 添加输入到表达式环境
        if (state.getInput() != null) {
            env.putAll(state.getInput());
        }

        // ⭐ 添加元数据到表达式环境（可选）
        if (state.getMeta() != null) {
            env.putAll(state.getMeta());
        }

        log.debug("Prepared expression environment with {} variables", env.size());
        return env;
    }

    @Override
    public String getSupportedType() {
        return AiFlowStepConfig.StepType.IF.name();
    }
}
