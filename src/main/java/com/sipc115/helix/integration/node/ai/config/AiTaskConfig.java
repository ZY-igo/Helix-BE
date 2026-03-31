/*-*- coding: UTF-8 -*-*/
package com.sipc115.helix.integration.node.ai.config;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * AI 任务配置
 */
@Data
public class AiTaskConfig {

    /**
     * 输入参数映射
     * Key: 变量名，Value: Aviator 表达式
     */
    private Map<String, String> inputs;

    /**
     * 中间变量声明
     * Key: 变量名，Value: 初始值
     */
    private Map<String, Object> vars;

    /**
     * 微流程步骤列表
     */
    private List<AiFlowStepConfig> flow;

    /**
     * 运行时策略
     */
    private RuntimePolicyConfig runtimePolicy;
}
