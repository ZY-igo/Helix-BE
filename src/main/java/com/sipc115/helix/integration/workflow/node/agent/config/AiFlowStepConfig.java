/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.config;

import lombok.Data;

/**
 * AI 流程步骤配置基类
 * <p>
 * 所有 AI 流程步骤配置的基类，定义了步骤的基本属性。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public abstract class AiFlowStepConfig {
    
    /**
     * 步骤类型枚举
     */
    public enum StepType {
        GENERATE,    // 生成步骤
        VALIDATE,    // 验证步骤
        REPAIR,      // 修复步骤
        IF,          // 条件步骤
        LOOP_WHILE,  // 循环步骤
        RETURN       // 返回步骤
    }

    /**
     * 步骤 ID
     * <p>
     * 步骤的唯一标识符，用于追踪和调试
     */
    private String id;

    /**
     * 步骤类型
     * <p>
     * 步骤的类型，如 GENERATE、VALIDATE、REPAIR 等
     */
    private String type;
}
