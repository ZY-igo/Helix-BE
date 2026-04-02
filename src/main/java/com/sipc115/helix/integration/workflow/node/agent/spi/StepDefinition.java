/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.workflow.node.agent.spi;

import com.sipc115.helix.integration.workflow.node.agent.step.task.AiFlowStepConfig;
import java.util.Map;

/**
 * AI 步骤定义接口
 * <p>
 * 每个步骤类型都应该有自己的 Definition，负责：
 * 1. 声明支持的步骤类型
 * 2. 解析 DSL 配置为强类型对象
 * 3. 验证配置的合法性
 *
 * @param <T> 步骤配置类型
 * @author Helix Team
 * @since 2.0.0
 */
public interface StepDefinition<T extends AiFlowStepConfig> {
    
    /**
     * 获取支持的步骤类型
     *
     * @return 步骤类型字符串
     */
    String supportedType();
    
    /**
     * 解析 DSL 配置
     *
     * @param stepConfig DSL 中的步骤配置
     * @return 强类型的配置对象
     */
    T parseConfig(Map<String, Object> stepConfig);
    
    /**
     * 验证配置合法性
     *
     * @param stepConfig DSL 中的步骤配置
     * @param stepIndex 步骤索引
     * @param nodeId 节点 ID
     */
    default void validate(Map<String, Object> stepConfig, int stepIndex, String nodeId) {
        // 可选的验证逻辑
    }
}
