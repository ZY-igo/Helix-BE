/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.If;

import com.googlecode.aviator.AviatorEvaluator;
import com.sipc115.helix.integration.workflow.node.agent.activity.AiStepActivity;
import com.sipc115.helix.integration.workflow.node.agent.activity.AiStepActivityFactory;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 条件步骤 Activity
 * <p>
 * 执行条件分支步骤，根据条件执行不同的分支
 *
 * @author Helix Team
 * @since 2.0.0
 */
@ActivityInterface
@Component
public class IfStepActivity implements AiStepActivity {

    private static final Logger log = LoggerFactory.getLogger(IfStepActivity.class);

    private final AiStepActivityFactory activityFactory;

    public IfStepActivity(AiStepActivityFactory activityFactory) {
        this.activityFactory = activityFactory;
    }

    @Override
    @ActivityMethod
    public StepTrace execute(AiFlowStepConfig stepConfig, AiTaskState state) {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        Object outputSnapshot = null;
        String errorMessage = null;

        try {
            IfStepConfig ifConfig = (IfStepConfig) stepConfig;

            // 1. 评估条件
            boolean conditionResult = evaluateCondition(ifConfig.getCondition(), state);
            List<StepTrace> childSteps = new ArrayList<>();

            log.info("IF step condition evaluated to: {}", conditionResult);

            // 2. 根据条件执行相应分支
            if (conditionResult) {
                // 执行 then 分支
                List<AiFlowStepConfig> thenSteps = ifConfig.getThenSteps();
                for (AiFlowStepConfig thenStep : thenSteps) {
                    AiStepActivity activity = activityFactory.getActivity(thenStep.getType());
                    StepTrace childTrace = activity.execute(thenStep, state);
                    childSteps.add(childTrace);

                    // 如果子步骤失败，停止执行
                    if ("FAILED".equals(childTrace.getStatus())) {
                        status = "FAILED";
                        errorMessage = childTrace.getErrorMessage();
                        break;
                    }
                }
            } else {
                // 执行 else 分支
                List<AiFlowStepConfig> elseSteps = ifConfig.getElseSteps();
                for (AiFlowStepConfig elseStep : elseSteps) {
                    AiStepActivity activity = activityFactory.getActivity(elseStep.getType());
                    StepTrace childTrace = activity.execute(elseStep, state);
                    childSteps.add(childTrace);

                    // 如果子步骤失败，停止执行
                    if ("FAILED".equals(childTrace.getStatus())) {
                        status = "FAILED";
                        errorMessage = childTrace.getErrorMessage();
                        break;
                    }
                }
            }

        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
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
        trace.setErrorMessage(errorMessage);
        return trace;
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String condition, AiTaskState state) {
        try {
            Map<String, Object> env = prepareExpressionEnv(state);
            Object result = AviatorEvaluator.execute(condition, env);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to evaluate condition: {}", condition, e);
            return false;
        }
    }

    /**
     * 准备表达式环境
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
}
