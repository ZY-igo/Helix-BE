/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.workflow;

import com.sipc115.helix.integration.workflow.node.agent.step.task.AiTaskConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * AI 任务 Workflow 接口
 * <p>
 * 协调执行 AI 子流程的各个步骤
 *
 * @author Helix Team
 * @since 2.0.0
 */
@WorkflowInterface
public interface AiTaskWorkflow {

    /**
     * 执行 AI 任务
     *
     * @param aiTaskConfig AI 任务配置
     * @param initialState 初始状态
     * @return 最终状态
     */
    @WorkflowMethod
    AiTaskState execute(AiTaskConfig aiTaskConfig, AiTaskState initialState);
}
