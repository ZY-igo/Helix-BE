/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 循环步骤配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoopWhileStepConfig extends AiFlowStepConfig {

    /**
     * 循环继续条件
     */
    private String condition;

    /**
     * 最大循环轮次
     */
    private Integer maxRounds;

    /**
     * 循环体内的步骤列表
     */
    private List<AiFlowStepConfig> body;
}
