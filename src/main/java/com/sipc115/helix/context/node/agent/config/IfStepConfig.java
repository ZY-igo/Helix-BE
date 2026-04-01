/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.agent.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 条件分支步骤配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IfStepConfig extends AiFlowStepConfig {

    /**
     * 条件表达式
     */
    private String condition;

    /**
     * then 分支的步骤列表
     */
    private List<AiFlowStepConfig> thenSteps;

    /**
     * else 分支的步骤列表（可选）
     */
    private List<AiFlowStepConfig> elseSteps;
}
