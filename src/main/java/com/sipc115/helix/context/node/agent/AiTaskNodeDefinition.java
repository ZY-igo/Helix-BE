/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.context.node.agent;

import com.sipc115.helix.context.node.agent.config.AiTaskConfig;
import com.sipc115.helix.domain.workflow.DslNodeType;
import com.sipc115.helix.integration.workflow.compiler.NodeDefinition;
import org.springframework.stereotype.Component;

/**
 * AI 任务节点定义
 * <p>
 * 实现了 NodeDefinition 接口，为 AI 任务节点提供类型定义和配置类信息。
 * 支持 SPI 架构的自动注册机制。
 * 
 * @author Helix Team
 * @since 2.0.0
 */
@Component
public class AiTaskNodeDefinition implements NodeDefinition<AiTaskConfig> {
    
    /**
     * 节点类型
     * 
     * @return AI_TASK 类型
     */
    @Override
    public DslNodeType type() {
        return DslNodeType.AI_TASK;
    }

    /**
     * 配置类
     * 
     * @return AiTaskConfig 类
     */
    @Override
    public Class<AiTaskConfig> configClass() {
        return AiTaskConfig.class;
    }
}
