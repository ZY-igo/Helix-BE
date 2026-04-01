/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.ai.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 修复步骤配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RepairStepConfig extends AiFlowStepConfig {

    /**
     * 待修复的内容
     */
    private String input;

    /**
     * 反馈信息
     */
    private String feedback;

    /**
     * 修复提示词模板
     */
    private String promptTemplate;

    /**
     * 模型名称（可选）
     */
    private String model;

    /**
     * 温度参数（可选）
     */
    private Double temperature;

    /**
     * 输出变量名
     */
    private String outputVar;
}
