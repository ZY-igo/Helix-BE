/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.If;

import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 条件步骤配置
 * <p>
 * 用于配置 AI 条件步骤，定义根据条件执行不同的分支。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IfStepConfig extends AiFlowStepConfig {

    /**
     * 条件表达式
     * <p>
     * 用于判断执行哪个分支的表达式，结果为布尔值
     * <p>
     * 示例：
     * <pre>
     * "validationResult.passed"
     * "score > 0.8"
     * </pre>
     */
    private String condition;

    /**
     * 条件为真时执行的步骤列表
     * <p>
     * 当条件表达式结果为 true 时执行的步骤
     */
    private List<AiFlowStepConfig> thenSteps;

    /**
     * 条件为假时执行的步骤列表
     * <p>
     * 当条件表达式结果为 false 时执行的步骤
     */
    private List<AiFlowStepConfig> elseSteps;
}
