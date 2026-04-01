/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.config;

import lombok.Data;

/**
 * AI 流程步骤配置基类
 */
@Data
public abstract class AiFlowStepConfig {

    /**
     * 步骤类型：GENERATE, VALIDATE, REPAIR, IF, LOOP_WHILE, RETURN
     */
    private String type;

    /**
     * 步骤唯一 ID
     */
    private String id;

    /**
     * 显示名称（可选）
     */
    private String label;

    /**
     * 是否禁用（可选，默认 false）
     */
    private Boolean disabled = false;

    /**
     * 步骤类型枚举
     */
    public enum StepType {
        GENERATE, VALIDATE, REPAIR, IF, LOOP_WHILE, RETURN
    }
}
