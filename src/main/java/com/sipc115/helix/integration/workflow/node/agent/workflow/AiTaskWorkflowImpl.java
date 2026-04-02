/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.workflow;

import com.sipc115.helix.integration.workflow.node.agent.activity.AiStepActivity;
import com.sipc115.helix.integration.workflow.node.agent.activity.AiStepActivityFactory;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.step.task.AiTaskConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * AI 任务 Workflow 实现
 * <p>
 * 协调执行 AI 子流程的各个步骤
 *
 * @author Helix Team
 * @since 2.0.0
 */
public class AiTaskWorkflowImpl implements AiTaskWorkflow {

    private static final Logger log = LoggerFactory.getLogger(AiTaskWorkflowImpl.class);

    private final AiStepActivityFactory activityFactory = Workflow.newActivityStub(AiStepActivityFactory.class);

    @Override
    public AiTaskState execute(AiTaskConfig aiTaskConfig, AiTaskState initialState) {
        log.info("Starting AI task workflow with {} steps", aiTaskConfig.getFlow().size());

        // 执行流程步骤
        List<AiFlowStepConfig> steps = aiTaskConfig.getFlow();
        for (AiFlowStepConfig step : steps) {
            log.info("Executing step: {} ({})", step.getId(), step.getType());

            // 获取对应的 Activity
            AiStepActivity activity = activityFactory.getActivity(step.getType());

            // 执行 Activity
            StepTrace trace = activity.execute(step, initialState);

            // 检查执行结果
            if ("FAILED".equals(trace.getStatus())) {
                log.error("Step failed: {} ({}) - {}", step.getId(), step.getType(), trace.getErrorMessage());
                break;
            }

            log.info("Step completed: {} ({})", step.getId(), step.getType());
        }

        log.info("AI task workflow completed");
        return initialState;
    }
}
