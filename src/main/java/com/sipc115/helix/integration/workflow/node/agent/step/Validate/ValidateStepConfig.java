/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.step.Validate;

import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 验证步骤配置
 * <p>
 * 用于配置 AI 验证步骤，定义如何验证内容是否符合要求。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ValidateStepConfig extends AiFlowStepConfig {

    /**
     * 输入变量名
     * <p>
     * 要验证的内容所在的变量名
     */
    private String input;

    /**
     * 验证器列表
     * <p>
     * 用于验证内容的验证器列表，每个验证器可以使用不同的验证方式
     */
    private List<ValidatorConfig> validators;

    /**
     * 输出变量名
     * <p>
     * 验证结果存储的变量名，可在后续步骤中使用
     */
    private String outputVar;
}
