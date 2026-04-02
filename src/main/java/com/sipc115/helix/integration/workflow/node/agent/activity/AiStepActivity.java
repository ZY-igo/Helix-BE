/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.activity;

import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;

/**
 * AI 步骤 Activity 接口
 * <p>
 * 所有 AI 步骤 Activity 的基础接口
 *
 * @author Helix Team
 * @since 2.0.0
 */
public interface AiStepActivity {

    /**
     * 执行步骤
     *
     * @param stepConfig 步骤配置
     * @param state 任务状态
     * @return 步骤执行轨迹
     */
    StepTrace execute(AiFlowStepConfig stepConfig, AiTaskState state);
}
