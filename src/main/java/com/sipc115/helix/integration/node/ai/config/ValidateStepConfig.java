/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.node.ai.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 验证步骤配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ValidateStepConfig extends AiFlowStepConfig {

    /**
     * 待验证的内容（Aviator 表达式）
     */
    private String input;

    /**
     * 验证器列表
     */
    private List<ValidatorConfig> validators;

    /**
     * 输出变量名
     */
    private String outputVar;
}
