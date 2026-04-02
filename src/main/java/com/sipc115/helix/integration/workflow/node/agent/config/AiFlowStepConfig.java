/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.config;

import lombok.Data;

/**
 * AI 流程步骤配置基类
 * <p>
 * 所有 AI 流程步骤的基础配置类，定义了步骤的基本属性。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Data
public class AiFlowStepConfig {

    /**
     * 步骤唯一标识
     */
    private String id;

    /**
     * 步骤类型
     */
    private String type;
}
