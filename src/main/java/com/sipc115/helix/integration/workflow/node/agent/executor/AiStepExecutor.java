/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.executor;

import com.sipc115.helix.integration.workflow.node.agent.config.AiFlowStepConfig;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskState;
import com.sipc115.helix.integration.workflow.node.agent.runtime.AiTaskTrace.StepTrace;

/**
 * AI 步骤执行器接口
 * <p>
 * 所有 AI 步骤执行器的统一接口，定义了执行步骤的方法。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
public interface AiStepExecutor {

    /**
     * 执行步骤
     * <p>
     * 执行指定的 AI 流程步骤，并返回执行轨迹。
     * 
     * @param stepConfig 步骤配置
     * @param state 任务状态
     * @return 步骤执行轨迹
     */
    StepTrace execute(AiFlowStepConfig stepConfig, AiTaskState state);

    /**
     * 获取支持的步骤类型
     * <p>
     * 返回此执行器支持的步骤类型，如 "GENERATE"、"VALIDATE" 等。
     * 
     * @return 支持的步骤类型
     */
    String getSupportedType();
}
