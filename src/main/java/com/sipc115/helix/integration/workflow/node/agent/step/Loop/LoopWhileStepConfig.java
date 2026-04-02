/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Loop;

import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 循环步骤配置
 * <p>
 * 用于配置 AI 循环步骤，定义根据条件重复执行一系列步骤。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoopWhileStepConfig extends AiFlowStepConfig {

    /**
     * 循环条件表达式
     * <p>
     * 用于判断是否继续循环的表达式，结果为布尔值
     * <p>
     * 示例：
     * <pre>
     * "attempts < maxAttempts"
     * "!validationResult.passed"
     * </pre>
     */
    private String condition;

    /**
     * 循环体步骤列表
     * <p>
     * 每次循环执行的步骤列表
     */
    private List<AiFlowStepConfig> body;

    /**
     * 最大循环次数
     * <p>
     * 防止无限循环的保护机制
     * <p>
     * 默认值：3
     */
    private Integer maxRounds;
}
