/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Return;

import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Map;

/**
 * 返回步骤配置
 * <p>
 * 用于配置 AI 返回步骤，定义任务的返回结果。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReturnStepConfig extends AiFlowStepConfig {

    /**
     * 返回结果
     * <p>
     * 任务的返回结果，键值对形式
     * <p>
     * 示例：
     * <pre>
     * {
     *   "answer": "${generatedAnswer}",
     *   "score": "${score}"
     * }
     * </pre>
     */
    private Map<String, Object> result;
}
