/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Loop;

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

import java.util.List;
import java.util.Map;

/**
 * 循环步骤 Activity
 * <p>
 * 执行循环步骤，根据条件重复执行循环体
 *
 * @author Helix Team
 * @since 2.0.0
 */
@ActivityInterface
@Component
public class LoopWhileStepActivity implements AiStepActivity {

    private static final Logger log = LoggerFactory.getLogger(LoopWhileStepActivity.class);

    private final AiStepActivityFactory activityFactory;

    public LoopWhileStepActivity(AiStepActivityFactory activityFactory) {
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
            LoopWhileStepConfig loopConfig = (LoopWhileStepConfig) stepConfig;
            int currentRound = 0;
            int maxRounds = loopConfig.getMaxRounds();

            // 执行循环
            while (evaluateCondition(loopConfig.getCondition(), state) && currentRound < maxRounds) {
                currentRound++;
                log.info("Loop iteration {}/{}", currentRound, maxRounds);

                // 执行循环体
                List<AiFlowStepConfig> bodySteps = loopConfig.getBody();
                for (AiFlowStepConfig bodyStep : bodySteps) {
                    AiStepActivity activity = activityFactory.getActivity(bodyStep.getType());
                    StepTrace childTrace = activity.execute(bodyStep, state);

                    // 如果子步骤失败，停止循环
                    if ("FAILED".equals(childTrace.getStatus())) {
                        status = "FAILED";
                        errorMessage = childTrace.getErrorMessage();
                        break;
                    }
                }

                // 如果有错误，停止循环
                if ("FAILED".equals(status)) {
                    break;
                }
            }

            log.info("Loop completed after {} iterations", currentRound);

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
