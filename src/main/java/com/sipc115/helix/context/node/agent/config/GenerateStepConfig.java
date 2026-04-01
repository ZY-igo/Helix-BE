/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.agent.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生成步骤配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GenerateStepConfig extends AiFlowStepConfig {

    /**
     * 提示词模板
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
     * 最大 Token 数（可选）
     */
    private Integer maxTokens;

    /**
     * 输出格式类型（可选）：null 或 "json"
     */
    private String responseFormatType;

    /**
     * Schema 引用（可选）
     */
    private String schemaRef;

    /**
     * 输出变量名
     */
    private String outputVar;
}
