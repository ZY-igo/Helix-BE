/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Map;

/**
 * 返回步骤配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReturnStepConfig extends AiFlowStepConfig {

    /**
     * 返回结果映射
     * Key: 字段名，Value: Aviator 表达式或实际值
     */
    private Map<String, Object> result;
}
